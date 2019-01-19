/*
 * Copyright 2018 Acosix GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.acosix.alfresco.utility.repo.content.transformer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.transform.AbstractContentTransformer2;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.HtmlUtils;

/**
 * @author Axel Faust
 */
public class RFC822ToBodyHTMLTransformer extends AbstractContentTransformer2
{

    @FunctionalInterface
    private static interface BodyRenderer
    {

        void renderEmailBody(StringBuilder builder) throws IOException, MessagingException;
    }

    private final static Logger LOGGER = LoggerFactory.getLogger(RFC822ToBodyHTMLTransformer.class);

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isTransformableMimetype(final String sourceMimetype, final String targetMimetype, final TransformationOptions options)
    {
        final boolean result = MimetypeMap.MIMETYPE_RFC822.equals(sourceMimetype) && MimetypeMap.MIMETYPE_HTML.equals(targetMimetype);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void transformInternal(final ContentReader reader, final ContentWriter writer, final TransformationOptions options)
    {
        try (InputStream is = reader.getContentInputStream();)
        {
            final MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()), is);

            // TODO Render HTML markup for email header + merge in with potentially existing default content (i.e. offset div)
            final Object content = mimeMessage.getContent();
            if (content instanceof Multipart)
            {
                this.processMultipart(mimeMessage.getSubject(), (Multipart) content, writer);
            }
            else if (mimeMessage.getContentType().equalsIgnoreCase(MimetypeMap.MIMETYPE_HTML))
            {
                writer.putContent(mimeMessage.getContent().toString());
            }
            else if (mimeMessage.getContentType().startsWith("text/"))
            {
                this.renderEmailHtml(mimeMessage.getSubject(), writer, sb -> this.renderTextPart(sb, mimeMessage));
            }
            else
            {
                this.createEmptyHtmlRepresentation(mimeMessage.getSubject(), writer);
            }
        }
        catch (final IOException | MessagingException ex)
        {
            LOGGER.debug("Error transforming RFC 822 to HTML", ex);
            throw new ContentIOException("Error transforming RFC822 to HTML");

        }
    }

    protected void processMultipart(final String subject, final Multipart multipart, final ContentWriter writer)
            throws IOException, MessagingException
    {
        final List<Part> textualParts = new ArrayList<>();
        final List<Part> htmlParts = new ArrayList<>();
        final List<Part> plainTextParts = new ArrayList<>();

        this.collectRelevantParts(multipart, textualParts, htmlParts, plainTextParts);

        writer.setMimetype(MimetypeMap.MIMETYPE_HTML);
        if (!htmlParts.isEmpty())
        {
            LOGGER.debug("Considering first HTML part of mail as primary content");
            // TODO merge multiple HTML parts together
            final Part firstPart = htmlParts.get(0);
            writer.putContent(firstPart.getContent().toString());
        }
        else if (!plainTextParts.isEmpty() || !textualParts.isEmpty())
        {
            final List<Part> partsToProcess = plainTextParts.isEmpty() ? textualParts : plainTextParts;
            LOGGER.debug("Creating pseudo-HTML representation of mail from {} non-HTML text parts (only plain-text: {})",
                    partsToProcess.size(), partsToProcess == plainTextParts);

            this.renderEmailHtml(subject, writer, sb -> {
                for (final Part currentPart : partsToProcess)
                {
                    this.renderTextPart(sb, currentPart);
                }
            });
        }
        else
        {
            this.createEmptyHtmlRepresentation(subject, writer);
        }
    }

    protected void renderEmailHtml(final String subject, final ContentWriter writer, final BodyRenderer bodyRenderer)
            throws IOException, MessagingException
    {
        final StringBuilder builder = new StringBuilder(4096);
        builder.append("<html><head><title>");
        builder.append(subject);
        builder.append("</title></head><body>");

        bodyRenderer.renderEmailBody(builder);

        builder.append("</body></html>");

        writer.putContent(builder.toString());
    }

    protected void createEmptyHtmlRepresentation(final String subject, final ContentWriter writer) throws IOException, MessagingException
    {
        LOGGER.debug("Creating empty pseudo-HTML representation of mail");
        this.renderEmailHtml(subject, writer, sb -> {
            // NO-OP
        });
    }

    protected void renderTextPart(final StringBuilder builder, final Part currentPart) throws IOException, MessagingException
    {
        builder.append("<div>");
        final Object content = currentPart.getContent();
        final String textContent = content.toString();
        // escape so text is treated literally without any chance of being treated as HTML
        final String escapedContent = HtmlUtils.htmlEscape(textContent);
        final String effectiveContent = escapedContent.replaceAll("\\n", "<br />");
        builder.append(effectiveContent);
        builder.append("</div>");
    }

    protected void collectRelevantParts(final Multipart multipart, final List<Part> textualParts, final List<Part> htmlParts,
            final List<Part> plainTextParts) throws MessagingException, IOException
    {
        final List<Multipart> remainingMultiparts = new LinkedList<>();
        remainingMultiparts.add(multipart);

        while (!remainingMultiparts.isEmpty())
        {
            final Multipart currentMultipart = remainingMultiparts.remove(0);
            for (int partIdx = 0; partIdx < currentMultipart.getCount(); partIdx++)
            {
                final Part bodyPart = currentMultipart.getBodyPart(partIdx);
                final Object bodyContent = bodyPart.getContent();
                if (bodyContent instanceof Multipart)
                {
                    remainingMultiparts.add((Multipart) bodyContent);
                }
                else if (bodyPart.getContentType().equalsIgnoreCase(MimetypeMap.MIMETYPE_HTML))
                {
                    textualParts.add(bodyPart);
                    htmlParts.add(bodyPart);
                }
                else if (bodyPart.getContentType().equalsIgnoreCase(MimetypeMap.MIMETYPE_TEXT_PLAIN))
                {
                    textualParts.add(bodyPart);
                    plainTextParts.add(bodyPart);
                }
                else if (bodyPart.getContentType().startsWith("text/"))
                {
                    textualParts.add(bodyPart);
                }
            }
        }
    }
}
