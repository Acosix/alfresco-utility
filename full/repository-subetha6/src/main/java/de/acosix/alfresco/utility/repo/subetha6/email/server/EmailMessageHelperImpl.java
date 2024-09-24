/*
 * Copyright 2016 - 2024 Acosix GmbH
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
package de.acosix.alfresco.utility.repo.subetha6.email.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.function.Consumer;

import org.alfresco.error.AlfrescoRuntimeException;

import de.acosix.alfresco.utility.repo.email.EmailMessage;
import de.acosix.alfresco.utility.repo.email.server.EmailMessageHelper;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;

/**
 *
 * @author Axel Faust
 */
public class EmailMessageHelperImpl implements EmailMessageHelper
{

    /**
     * @author Axel Faust
     */
    private static class EmailMessagePartImpl implements EmailMessagePart
    {

        private final Part part;

        /**
         * Constructs a new instance of this class.
         *
         * @param part
         *     the message part
         */
        private EmailMessagePartImpl(final Part part)
        {
            this.part = part;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getFileName(final boolean decoded)
        {
            try
            {
                String fileName = this.part.getFileName();
                if (fileName != null && decoded)
                {
                    fileName = MimeUtility.decodeText(fileName);
                }
                return fileName;
            }
            catch (final UnsupportedEncodingException e)
            {
                throw new AlfrescoRuntimeException("Failed to decode the message part file name", e);
            }
            catch (final MessagingException e)
            {
                return null;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getContentType()
        {
            try
            {
                return this.part.getContentType();
            }
            catch (final MessagingException e)
            {
                return null;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDisposition()
        {
            try
            {
                return this.part.getDisposition();
            }
            catch (final MessagingException e)
            {
                return null;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isAttachmentDisposition()
        {
            final String disposition = this.getDisposition();
            return Part.ATTACHMENT.equalsIgnoreCase(disposition);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isMultipart()
        {
            try
            {
                return this.part.isMimeType("multipart/*");
            }
            catch (final MessagingException e)
            {
                return false;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void processBodyParts(final Consumer<EmailMessagePart> bodyPartConsumer)
        {
            if (this.isMultipart())
            {
                try
                {
                    final Multipart mp = (Multipart) this.part.getContent();
                    final int count = mp.getCount();
                    for (int i = 0; i < count; i++)
                    {
                        bodyPartConsumer.accept(new EmailMessagePartImpl(mp.getBodyPart(i)));
                    }
                }
                catch (final IOException | MessagingException e)
                {
                    throw new AlfrescoRuntimeException("Failed to handle multipart body parts", e);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public InputStream getInputStream()
        {
            try
            {
                return this.part.getInputStream();
            }
            catch (final IOException | MessagingException e)
            {
                throw new AlfrescoRuntimeException("Failed to get message part input stream", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeTo(final EmailMessage message, final OutputStream os) throws IOException
    {
        try
        {
            message.getWrappedMessage(MimeMessage.class).writeTo(os);
        }
        catch (final MessagingException mEx)
        {
            throw new IOException(mEx);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EmailMessagePart getMessagePart(final EmailMessage message)
    {
        return new EmailMessagePartImpl(message.getWrappedMessage(MimeMessage.class));
    }
}
