/*
 * Copyright 2016 - 2021 Acosix GmbH
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

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.alfresco.email.server.handler.EmailMessageHandler;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.encoding.ContentCharsetFinder;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.email.EmailService;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ParameterCheck;
import org.alfresco.util.PropertyCheck;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import de.acosix.alfresco.utility.repo.email.server.ImprovedEmailService;
import de.acosix.alfresco.utility.repo.util.ImprovedFileNameValidator;

/**
 * @author Axel Faust
 */
public abstract class AbstractEmailMessageHandler implements InitializingBean, EmailMessageHandler
{

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEmailMessageHandler.class);

    private static final Collection<String> CHARS_TO_ENCODE_IN_SUBJECT = Collections
            .unmodifiableList(Arrays.asList("\\", "/", "*", "|", ":", "\"", "<", ">", "?"));

    /**
     * Encodes the subject line in order for it to be acceptable as a {@link ImprovedFileNameValidator#isValid(String) valid file name}.
     * Instead of simply replacing invalid characters with the underline character (as the file name validator does), this operation will
     * encode a set of typical characters found in subjects with their URL-encoded form. Only as a last resort is the subject line processed
     * via the {@link ImprovedFileNameValidator#getValidFileName(String) default valid file name determination operation}.
     *
     * @param subject
     *            the subject to encode
     * @return the encoded subject
     */
    protected static String encodeSubject(final String subject)
    {
        ParameterCheck.mandatoryString("subject", subject);

        final StringBuilder encodedSubjectBuilder = new StringBuilder(subject.trim());
        CHARS_TO_ENCODE_IN_SUBJECT.forEach((character) -> {
            int idx = encodedSubjectBuilder.indexOf(character);
            String replacement = null;
            while (idx != -1)
            {
                if (replacement == null)
                {
                    // we know all characters are in the single unicode codepoint range
                    final int codepoint = character.codePointAt(0);
                    replacement = '#' + Integer.toHexString(codepoint);
                }
                encodedSubjectBuilder.replace(idx, idx + 1, replacement);
                idx = encodedSubjectBuilder.indexOf(character, idx + 3);
            }
        });

        if (encodedSubjectBuilder.charAt(encodedSubjectBuilder.length() - 1) == '.')
        {
            encodedSubjectBuilder.replace(encodedSubjectBuilder.length() - 1, encodedSubjectBuilder.length(), "%2e");
        }

        final String encodedSubject = ImprovedFileNameValidator.getValidFileName(encodedSubjectBuilder.toString());
        return encodedSubject;
    }

    protected DictionaryService dictionaryService;

    protected NodeService nodeService;

    protected ContentService contentService;

    protected MimetypeService mimetypeService;

    protected ActionService actionService;

    protected EmailService emailService;

    protected String nodeType;

    protected boolean enabled;

    protected int maxAttemptsAtUniqueName = 10000;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "dictionaryService", this.dictionaryService);
        PropertyCheck.mandatory(this, "nodeService", this.nodeService);
        PropertyCheck.mandatory(this, "contentService", this.contentService);
        PropertyCheck.mandatory(this, "mimetypeService", this.mimetypeService);
        PropertyCheck.mandatory(this, "actionService", this.actionService);

        if (this.nodeType != null && this.enabled)
        {
            PropertyCheck.mandatory(this, "emailService", this.emailService);
            if (this.emailService instanceof ImprovedEmailService)
            {
                ((ImprovedEmailService) this.emailService).register(this.nodeType, this);
            }
            else
            {
                throw new IllegalStateException("The EmailService has not been enhanced");
            }
        }
    }

    /**
     * @param dictionaryService
     *            the dictionaryService to set
     */
    public void setDictionaryService(final DictionaryService dictionaryService)
    {
        this.dictionaryService = dictionaryService;
    }

    /**
     * @param nodeService
     *            the nodeService to set
     */
    public void setNodeService(final NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    /**
     * @param contentService
     *            the contentService to set
     */
    public void setContentService(final ContentService contentService)
    {
        this.contentService = contentService;
    }

    /**
     * @param mimetypeService
     *            the mimetypeService to set
     */
    public void setMimetypeService(final MimetypeService mimetypeService)
    {
        this.mimetypeService = mimetypeService;
    }

    /**
     * @param actionService
     *            the actionService to set
     */
    public void setActionService(final ActionService actionService)
    {
        this.actionService = actionService;
    }

    /**
     * @param emailService
     *            the emailService to set
     */
    public void setEmailService(final EmailService emailService)
    {
        this.emailService = emailService;
    }

    /**
     * @param nodeType
     *            the nodeType to set
     */
    public void setNodeType(final String nodeType)
    {
        this.nodeType = nodeType;
    }

    /**
     * @param enabled
     *            the enabled to set
     */
    public void setEnabled(final boolean enabled)
    {
        this.enabled = enabled;
    }

    /**
     * @param maxAttemptsAtUniqueName
     *            the maxAttemptsAtUniqueName to set
     */
    public void setMaxAttemptsAtUniqueName(final int maxAttemptsAtUniqueName)
    {
        this.maxAttemptsAtUniqueName = maxAttemptsAtUniqueName;
    }

    protected NodeRef getOrCreateContentNode(final NodeRef parent, final String name, final QName assocType, final boolean overwrite,
            final Map<QName, Serializable> properties)
    {
        String workingName = encodeSubject(name);

        LOGGER.debug("Retrieving / creating content node below {} for name {} (overwrite: {})", parent, workingName, overwrite);

        NodeRef contentNode = null;
        final StringBuilder workingNameBuilder = new StringBuilder(workingName);
        final String baseName = FilenameUtils.getBaseName(workingName);
        final int baseNameEndIdx = baseName.length();
        int postFixEndIdx = -1;

        for (int counter = 1; counter < this.maxAttemptsAtUniqueName && contentNode == null; counter++)
        {
            final QName safeQName = QName.createQNameWithValidLocalName(NamespaceService.CONTENT_MODEL_1_0_URI, workingName);
            final NodeRef childNodeRef = this.nodeService.getChildByName(parent, ContentModel.ASSOC_CONTAINS, workingName);

            if (childNodeRef != null)
            {
                if (overwrite)
                {
                    LOGGER.debug("Overwriting existing node {} with name {}", childNodeRef, workingName);
                    properties.put(ContentModel.PROP_NAME, workingName);
                    this.nodeService.addProperties(childNodeRef, properties);
                    contentNode = childNodeRef;
                }
                else
                {
                    final String newCounter = String.valueOf(counter);
                    if (postFixEndIdx == -1)
                    {
                        workingNameBuilder.insert(baseNameEndIdx, '(');
                        workingNameBuilder.insert(baseNameEndIdx + 1, newCounter);
                        workingNameBuilder.insert(baseNameEndIdx + 2, ')');
                        postFixEndIdx = baseNameEndIdx + 3;
                    }
                    else
                    {
                        final String oldCounter = String.valueOf(counter - 1);
                        workingNameBuilder.replace(baseNameEndIdx + 1, postFixEndIdx - 1, newCounter);
                        postFixEndIdx += newCounter.length() - oldCounter.length();
                    }

                    workingName = workingNameBuilder.toString();
                }
            }
            else
            {
                properties.put(ContentModel.PROP_NAME, workingName);
                contentNode = this.nodeService.createNode(parent, assocType, safeQName, ContentModel.TYPE_CONTENT, properties)
                        .getChildRef();
                LOGGER.debug("Child node with name {} did not exist - created new node {}", workingName, contentNode);
            }
        }

        if (contentNode == null)
        {
            throw new AlfrescoRuntimeException("Unable to add new file");
        }

        return contentNode;
    }

    protected void writeContent(final NodeRef nodeRef, final InputStream content, final String mimetype, String encoding)
    {
        final InputStream bis = new BufferedInputStream(content, 4092);

        // Only guess the encoding if it has not been supplied
        if (encoding == null)
        {
            if (this.mimetypeService.isText(mimetype))
            {
                final ContentCharsetFinder charsetFinder = this.mimetypeService.getContentCharsetFinder();
                encoding = charsetFinder.getCharset(bis, mimetype).name();
            }
            else
            {
                encoding = "UTF-8";
            }
        }

        LOGGER.debug("Writing content of mimetype {} and encoding {} to {}", mimetype, encoding, nodeRef);

        final ContentWriter writer = this.contentService.getWriter(nodeRef, ContentModel.PROP_CONTENT, true);
        writer.setMimetype(mimetype);
        writer.setEncoding(encoding);
        writer.putContent(bis);
    }
}
