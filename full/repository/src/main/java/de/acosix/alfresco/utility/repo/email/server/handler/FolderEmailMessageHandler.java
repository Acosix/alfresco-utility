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
package de.acosix.alfresco.utility.repo.email.server.handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.model.ImapModel;
import org.alfresco.repo.action.executer.ContentMetadataExtracter;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.email.EmailMessage;
import org.alfresco.service.cmr.email.EmailMessageException;
import org.alfresco.service.cmr.email.EmailMessagePart;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.surf.util.I18NUtil;

import de.acosix.alfresco.utility.repo.email.server.ImprovedEmailMessage;
import de.acosix.alfresco.utility.repo.model.EmailModel;

/**
 * @author Axel Faust
 */
public class FolderEmailMessageHandler extends AbstractEmailMessageHandler
{

    // copied from Alfresco class of same name
    protected static final String MSG_RECEIVED_BY_SMTP = "email.server.msg.received_by_smtp";

    protected static final String MSG_DEFAULT_SUBJECT = "email.server.msg.default_subject";

    protected static final String ERR_MAIL_READ_ERROR = "email.server.err.mail_read_error";

    private static final Logger LOGGER = LoggerFactory.getLogger(FolderEmailMessageHandler.class);

    private static final Pattern ENCODING_EXTRACTOR = Pattern.compile("charset\\s*=[\\s\"]*([^\";\\s]*)");

    private static final Set<QName> KNOWN_EMAIL_PROPERTIES = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList(ContentModel.PROP_SENTDATE, ContentModel.PROP_ORIGINATOR,
                    ContentModel.PROP_ADDRESSEE, ContentModel.PROP_ADDRESSEES, ContentModel.PROP_SUBJECT)));

    protected boolean overwriteDuplicates = false;

    protected boolean extractAttachments = false;

    protected boolean extractAttachmentsAsDirectChildren = false;

    protected boolean copyEmailMetadataToAttachments = false;

    /**
     * @param overwriteDuplicates
     *     the overwriteDuplicates to set
     */
    public void setOverwriteDuplicates(final boolean overwriteDuplicates)
    {
        this.overwriteDuplicates = overwriteDuplicates;
    }

    /**
     * @param extractAttachments
     *     the extractAttachments to set
     */
    public void setExtractAttachments(final boolean extractAttachments)
    {
        this.extractAttachments = extractAttachments;
    }

    /**
     * @param extractAttachmentsAsDirectChildren
     *     the extractAttachmentsAsDirectChildren to set
     */
    public void setExtractAttachmentsAsDirectChildren(final boolean extractAttachmentsAsDirectChildren)
    {
        this.extractAttachmentsAsDirectChildren = extractAttachmentsAsDirectChildren;
    }

    /**
     * @param copyEmailMetadataToAttachments
     *     the copyEmailMetadataToAttachments to set
     */
    public void setCopyEmailMetadataToAttachments(final boolean copyEmailMetadataToAttachments)
    {
        this.copyEmailMetadataToAttachments = copyEmailMetadataToAttachments;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void processMessage(final NodeRef nodeRef, final EmailMessage message)
    {
        LOGGER.debug("Message from {} to {} is being processed by FolderMailMessageHandler", message.getFrom(), message.getTo());
        try
        {
            final QName nodeTypeQName = this.nodeService.getType(nodeRef);

            if (this.dictionaryService.isSubClass(nodeTypeQName, ContentModel.TYPE_FOLDER))
            {
                // Add the content into the system
                this.addToFolder(nodeRef, message);
            }
            else
            {
                LOGGER.debug("Handler called on unsupported target node type {} of {}", nodeTypeQName, nodeRef);
                throw new AlfrescoRuntimeException("\n" + "Message handler " + this.getClass().getName() + " cannot handle type "
                        + nodeTypeQName + ".\n" + "Check the message handler mappings.");
            }
        }
        catch (final IOException ex)
        {
            LOGGER.error("IO exception during processing of email", ex);
            throw new EmailMessageException(ERR_MAIL_READ_ERROR, ex.getMessage());
        }
    }

    /**
     * Add content to Alfresco repository
     *
     * @param folderNode
     *     Addressed node
     * @param message
     *     Mail message
     * @throws IOException
     *     Exception can be thrown while saving a content into Alfresco repository.
     */
    protected void addToFolder(final NodeRef folderNode, final EmailMessage message) throws IOException
    {
        final Map<QName, Serializable> folderProperties = this.nodeService.getProperties(folderNode);
        final Boolean folderExtractAttachments = DefaultTypeConverter.INSTANCE.convert(Boolean.class,
                folderProperties.get(EmailModel.PROP_EXTRACT_ATTACHMENTS));
        final Boolean folderExtractAttachmentsAsDirectChildren = DefaultTypeConverter.INSTANCE.convert(Boolean.class,
                folderProperties.get(EmailModel.PROP_EXTRACT_ATTACHMENTS_AS_DIRECT_CHILDREN));
        final Boolean folderOverwriteDuplicates = DefaultTypeConverter.INSTANCE.convert(Boolean.class,
                folderProperties.get(EmailModel.PROP_OVERWRITE_DUPLICATES));

        String messageSubject = message.getSubject();
        if (messageSubject == null || messageSubject.length() == 0)
        {
            final Date now = new Date();
            final SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy-hh-mm-ss", Locale.ENGLISH);
            df.setTimeZone(TimeZone.getDefault());
            messageSubject = I18NUtil.getMessage(MSG_DEFAULT_SUBJECT, df.format(now));
        }

        String messageFrom = message.getFrom();
        if (messageFrom == null)
        {
            messageFrom = "";
        }

        final Map<QName, Serializable> properties = new HashMap<>();
        properties.put(ContentModel.PROP_TITLE, messageSubject);
        properties.put(ContentModel.PROP_DESCRIPTION, I18NUtil.getMessage(MSG_RECEIVED_BY_SMTP, messageFrom));

        final boolean effectiveOverwriteDuplicates = folderOverwriteDuplicates != null ? Boolean.TRUE.equals(folderOverwriteDuplicates)
                : this.overwriteDuplicates;
        final NodeRef contentNode = this.getOrCreateContentNode(folderNode, messageSubject, ContentModel.ASSOC_CONTAINS,
                effectiveOverwriteDuplicates, properties);
        this.writeMailContent(contentNode, message);

        final boolean effectiveExtractAttachments = folderExtractAttachments != null ? Boolean.TRUE.equals(folderExtractAttachments)
                : this.extractAttachments;

        final Action extracterAction = this.actionService.createAction(ContentMetadataExtracter.EXECUTOR_NAME);
        this.actionService.executeAction(extracterAction, contentNode, true,
                !effectiveExtractAttachments || !this.copyEmailMetadataToAttachments);

        if (effectiveExtractAttachments)
        {
            final boolean effectiveExtractAttachmentsAsDirectChildren = folderExtractAttachmentsAsDirectChildren != null
                    ? Boolean.TRUE.equals(folderExtractAttachmentsAsDirectChildren)
                    : this.extractAttachmentsAsDirectChildren;
            this.extractAttachments(folderNode, contentNode, message, effectiveExtractAttachmentsAsDirectChildren);
        }

    }

    protected void writeMailContent(final NodeRef contentNode, final EmailMessage message) throws IOException
    {
        if (message instanceof ImprovedEmailMessage)
        {
            final ContentWriter writer = this.contentService.getWriter(contentNode, ContentModel.PROP_CONTENT, true);
            writer.setMimetype(MimetypeMap.MIMETYPE_RFC822);
            try (OutputStream os = writer.getContentOutputStream())
            {
                MESSAGE_HELPER.writeTo(((ImprovedEmailMessage) message).getMimeMessage(), os);
            }
            catch (final IOException e)
            {
                LOGGER.error("Error writing content of mime message", e);
                throw new AlfrescoRuntimeException("Failure storing original RFC 822 email", e);
            }
        }
        else
        {
            final EmailMessagePart body = message.getBody();
            if (body.getSize() == -1)
            {
                LOGGER.debug("Writing single space as content on {} for empty email body from {}", contentNode, message.getFrom());
                final ContentWriter writer = this.contentService.getWriter(contentNode, ContentModel.PROP_CONTENT, true);
                writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
                writer.setEncoding("UTF-8");
                writer.putContent(" ");
            }
            else
            {
                final InputStream contentIs = body.getContent();
                String mimetype = this.mimetypeService.guessMimetype(message.getSubject());
                if (mimetype.equals(MimetypeMap.MIMETYPE_BINARY))
                {
                    mimetype = MimetypeMap.MIMETYPE_TEXT_PLAIN;
                }
                final String encoding = body.getEncoding();

                this.writeContent(contentNode, contentIs, mimetype, encoding);
            }
        }
    }

    protected void extractAttachments(final NodeRef folderRef, final NodeRef mailNodeRef, final EmailMessage message,
            final boolean extractAttachmentsAsDirectChildren) throws IOException
    {
        final QName childAssocType = extractAttachmentsAsDirectChildren ? EmailModel.ASSOC_ATTACHMENTS : ContentModel.ASSOC_CONTAINS;
        final NodeRef attachmentParent = extractAttachmentsAsDirectChildren ? mailNodeRef : folderRef;

        final Map<QName, Serializable> mailProperties = this.nodeService.getProperties(mailNodeRef);

        if (message instanceof ImprovedEmailMessage)
        {
            final Collection<de.acosix.alfresco.utility.repo.email.server.EmailMessageHelper.EmailMessagePart> attachments = new ArrayList<>();
            this.collectRelevantAttachments(MESSAGE_HELPER.getMessagePart(((ImprovedEmailMessage) message).getMimeMessage()), attachments);
            for (final de.acosix.alfresco.utility.repo.email.server.EmailMessageHelper.EmailMessagePart attachment : attachments)
            {
                this.writeAttachment(mailNodeRef, mailProperties, childAssocType, EmailModel.ASSOC_ATTACHMENTS, attachmentParent,
                        attachment);
            }
        }
        else
        {
            for (final EmailMessagePart attachment : message.getAttachments())
            {
                if (this.isRelevantAttachment(message.getSubject(), attachment))
                {
                    this.writeAttachment(mailNodeRef, mailProperties, childAssocType, EmailModel.ASSOC_ATTACHMENTS, attachmentParent,
                            attachment);
                }
            }
        }
    }

    protected void writeAttachment(final NodeRef mailNodeRef, final Map<QName, Serializable> mailProperties, final QName childAssocType,
            final QName mailNodeRefChildAssocType, final NodeRef attachmentParent, final EmailMessagePart attachment) throws IOException
    {
        final String fileName = attachment.getFileName();

        final String contentType = attachment.getContentType();
        String mimetype = contentType;
        if (mimetype.indexOf(';') != -1)
        {
            mimetype = mimetype.substring(0, mimetype.indexOf(';'));
        }
        final Matcher encodingMatcher = ENCODING_EXTRACTOR.matcher(contentType);
        String encoding = null;
        if (encodingMatcher.find())
        {
            encoding = encodingMatcher.group(1);
        }
        else
        {
            encoding = attachment.getEncoding();
        }

        if (MimetypeMap.MIMETYPE_BINARY.equals(mimetype))
        {
            mimetype = this.mimetypeService.guessMimetype(fileName);
        }

        final Map<QName, Serializable> properties = new HashMap<>();

        if (this.copyEmailMetadataToAttachments)
        {
            mailProperties.forEach((key, value) -> {
                if (ImapModel.IMAP_MODEL_1_0_URI.equals(key.getNamespaceURI()) && KNOWN_EMAIL_PROPERTIES.contains(key))
                {
                    properties.put(key, value);
                }
            });
        }

        final NodeRef attachmentNodeRef = this.getOrCreateContentNode(attachmentParent, fileName, childAssocType, false, properties);

        this.nodeService.addAspect(mailNodeRef, ContentModel.ASPECT_ATTACHABLE, Collections.<QName, Serializable> emptyMap());
        this.nodeService.createAssociation(mailNodeRef, attachmentNodeRef, ContentModel.ASSOC_ATTACHMENTS);

        if (!childAssocType.equals(mailNodeRefChildAssocType))
        {
            this.nodeService.addChild(mailNodeRef, attachmentNodeRef, mailNodeRefChildAssocType,
                    this.nodeService.getPrimaryParent(attachmentNodeRef).getQName());
        }

        try (final InputStream attachmentStream = attachment.getContent())
        {
            this.writeContent(attachmentNodeRef, attachmentStream, mimetype, encoding);
        }

        final Action extracterAction = this.actionService.createAction(ContentMetadataExtracter.EXECUTOR_NAME);
        this.actionService.executeAction(extracterAction, attachmentNodeRef, true, true);
    }

    protected void writeAttachment(final NodeRef mailNodeRef, final Map<QName, Serializable> mailProperties, final QName childAssocType,
            final QName mailNodeRefChildAssocType, final NodeRef attachmentParent,
            final de.acosix.alfresco.utility.repo.email.server.EmailMessageHelper.EmailMessagePart attachment) throws IOException
    {
        try
        {
            final String fileName = attachment.getFileName(true);
            if (fileName == null)
            {
                throw new EmailMessageException("Cannot handle attachment without a file name");
            }

            final String contentType = attachment.getContentType();
            String mimetype = contentType;
            if (mimetype.indexOf(';') != -1)
            {
                mimetype = mimetype.substring(0, mimetype.indexOf(';'));
            }
            final Matcher encodingMatcher = ENCODING_EXTRACTOR.matcher(contentType);
            String encoding = null;
            if (encodingMatcher.find())
            {
                encoding = encodingMatcher.group(1);
            }

            if (MimetypeMap.MIMETYPE_BINARY.equals(mimetype))
            {
                mimetype = this.mimetypeService.guessMimetype(fileName);
            }

            final Map<QName, Serializable> properties = new HashMap<>();

            if (this.copyEmailMetadataToAttachments)
            {
                mailProperties.forEach((key, value) -> {
                    if (ImapModel.IMAP_MODEL_1_0_URI.equals(key.getNamespaceURI()) && KNOWN_EMAIL_PROPERTIES.contains(key))
                    {
                        properties.put(key, value);
                    }
                });
            }

            final NodeRef attachmentNodeRef = this.getOrCreateContentNode(attachmentParent, fileName, childAssocType, false, properties);

            this.nodeService.addAspect(mailNodeRef, ContentModel.ASPECT_ATTACHABLE, Collections.<QName, Serializable> emptyMap());
            this.nodeService.createAssociation(mailNodeRef, attachmentNodeRef, ContentModel.ASSOC_ATTACHMENTS);

            if (!childAssocType.equals(mailNodeRefChildAssocType))
            {
                this.nodeService.addChild(mailNodeRef, attachmentNodeRef, mailNodeRefChildAssocType,
                        this.nodeService.getPrimaryParent(attachmentNodeRef).getQName());
            }

            try (final InputStream attachmentStream = attachment.getInputStream())
            {
                this.writeContent(attachmentNodeRef, attachmentStream, mimetype, encoding);
            }

            final Action extracterAction = this.actionService.createAction(ContentMetadataExtracter.EXECUTOR_NAME);
            this.actionService.executeAction(extracterAction, attachmentNodeRef, true, true);
        }
        catch (final RuntimeException e)
        {
            LOGGER.error("Error processing mail attachment part", e);
            if (e instanceof AlfrescoRuntimeException)
            {
                throw e;
            }
            throw new AlfrescoRuntimeException("Failed to store mail attachment", e);
        }
    }

    protected void collectRelevantAttachments(
            final de.acosix.alfresco.utility.repo.email.server.EmailMessageHelper.EmailMessagePart messagePart,
            final Collection<de.acosix.alfresco.utility.repo.email.server.EmailMessageHelper.EmailMessagePart> attachmentParts)
    {
        try
        {
            if (messagePart.isMultipart())
            {
                messagePart.processBodyParts(p -> this.collectRelevantAttachments(p, attachmentParts));
            }
            else if (messagePart.isAttachmentDisposition() && messagePart.getFileName(false) != null)
            {
                attachmentParts.add(messagePart);
            }
        }
        catch (final RuntimeException e)
        {
            LOGGER.error("Error processing mail message parts", e);
            if (e instanceof AlfrescoRuntimeException)
            {
                throw e;
            }
            throw new AlfrescoRuntimeException("Error evaluating message parts for attachments", e);
        }
    }

    protected boolean isRelevantAttachment(final String subject, final EmailMessagePart attachment)
    {
        // alternative parts don't have a file name, but Alfresco implicitly sets it equal to the subject
        final String fileName = attachment.getFileName();

        boolean isAttachment = fileName != null;
        // check for Alfresco special cases of fileName == subject + extension / fileName == subject
        // see SubethaEmailMessage.getPartFileName for logic that we need to guard against here
        if (isAttachment
                && ((fileName.matches(".+\\.(txt|html|xml|gif)$") && fileName.substring(0, fileName.lastIndexOf('.')).equals(subject))
                        || fileName.equals(subject)))
        {
            isAttachment = false;
        }

        return isAttachment;
    }
}
