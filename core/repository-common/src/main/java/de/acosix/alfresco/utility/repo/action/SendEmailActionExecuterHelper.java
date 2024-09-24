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
package de.acosix.alfresco.utility.repo.action;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

import org.alfresco.service.cmr.action.ActionServiceException;
import org.springframework.mail.javamail.JavaMailSender;

import de.acosix.alfresco.utility.repo.email.EmailAddress;
import de.acosix.alfresco.utility.repo.email.EmailMessage;

/**
 * Instances of this interface implement API bridging helper functionality for handling the sending of emails.
 * 
 * @author Axel Faust
 */
public interface SendEmailActionExecuterHelper
{

    /**
     * Instances of this class handle that setting of addresses in emails.
     * 
     * @author Axel Faust
     */
    interface AddressHandler
    {

        /**
         * Sets the direct recipient addresses
         * 
         * @param addresses
         *     the collection of addresses
         */
        void setTo(Collection<EmailAddress> addresses);

        /**
         * Sets the carbon-copy recipient addresses
         * 
         * @param addresses
         *     the collection of addresses
         */
        void setCc(Collection<EmailAddress> addresses);

        /**
         * Sets the blind carbon-copy recipient addresses
         * 
         * @param addresses
         *     the collection of addresses
         */
        void setBcc(Collection<EmailAddress> addresses);

        /**
         * Sets the from address
         * 
         * @param address
         *     the address
         */
        void setFrom(EmailAddress address);

        /**
         * Sets the reply-to address
         * 
         * @param address
         *     the address
         */
        void setReplyTo(EmailAddress address);
    }

    /**
     * Instances of this class handle that inclusion of attachments in emails.
     * 
     * @author Axel Faust
     */
    interface AttachmentHandler
    {

        /**
         * Adds an inline attachment.
         * 
         * @param id
         *     the ID of the attachment
         * @param mimetype
         *     the mimetype of the attachment
         * @param is
         *     the supplier for the input stream to the content of the attachment
         */
        void addInline(String id, String mimetype, Supplier<InputStream> is);

        /**
         * Adds a regular attachment.
         * 
         * @param name
         *     the name of the attachment
         * @param mimetype
         *     the mimetype of the attachment
         * @param is
         *     the supplier for the input stream to the content of the attachment
         */
        void addAttachment(String name, String mimetype, Supplier<InputStream> is);
    }

    /**
     * Converts and wraps an email address for later use.
     * 
     * @param address
     *     the address string to convert and wrap
     * @return the implementation specific wrapper for the address
     */
    EmailAddress toEmailAddress(String address);

    /**
     * Converts and wraps an email address for later use.
     * 
     * @param address
     *     the address string to convert and wrap
     * @param personalName
     *     the personal name of the addressee
     * @return the implementation specific wrapper for the address
     */
    EmailAddress toEmailAddress(String address, String personalName);

    /**
     * Builds a basic email message with a subject and content.
     * 
     * @param subject
     *     the subject line
     * @param text
     *     the text content
     * @param html
     *     the HTML content
     * @param headers
     *     the email headers
     * @param mailSender
     *     the Spring Java mail sender instance to use for the actual sending
     * @return the email message
     */
    EmailMessage buildEmail(String subject, String text, String html, Map<String, String> headers, JavaMailSender mailSender);

    /**
     * Gets an address handler for an email message.
     * 
     * @param message
     *     the message for which to get the address handler
     * @return the address handler
     */
    AddressHandler getAddressHandler(EmailMessage message);

    /**
     * Gets an attachment handler for an email message.
     * 
     * @param message
     *     the message for which to get the attachment handler
     * @return the attachment handler
     */
    AttachmentHandler getAttachmentHandler(EmailMessage message);

    /**
     * Sends a prepared email message
     * 
     * @param message
     *     the email message to send
     * @param originalToParam
     *     the (original) value of the {@code PARAM_TO} action parameter
     * @param ignoreError
     *     {@code Boolean#TRUE} if errors should be ignored, otherwise a {@link ActionServiceException} will be raised
     * @param mailSender
     *     the Spring Java mail sender instance to use for the actual sending
     */
    void sendMail(EmailMessage message, Serializable originalToParam, Boolean ignoreError, JavaMailSender mailSender);
}
