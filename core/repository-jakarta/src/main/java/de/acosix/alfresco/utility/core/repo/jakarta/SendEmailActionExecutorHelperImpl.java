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
package de.acosix.alfresco.utility.core.repo.jakarta;

import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.alfresco.service.cmr.action.ActionServiceException;
import org.alfresco.util.ParameterCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import de.acosix.alfresco.utility.repo.action.SendEmailActionExecuterHelper;
import de.acosix.alfresco.utility.repo.email.EmailAddress;
import de.acosix.alfresco.utility.repo.email.EmailMessage;
import jakarta.mail.Address;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

/**
 * Instances of this class provide the API bridge for {@code jakarta.mail-api}-based email handling.
 *
 * @author Axel Faust
 */
public class SendEmailActionExecutorHelperImpl implements SendEmailActionExecuterHelper
{

    /**
     * Variant of a {@link Consumer} that exposes the messaging exception throws by mail related operations.
     *
     * @author Axel Faust
     */
    @FunctionalInterface
    private interface MessagingConsumer<T>
    {

        /**
         * Accepts and processes data.
         *
         * @param t
         *     the data to process
         * @throws MessagingException
         *     if an error occurs
         */
        void accept(T t) throws MessagingException;
    }

    /**
     * Instances of this class handle the address information of emails.
     *
     * @author Axel Faust
     */
    private static class AddressHandlerImpl implements AddressHandler
    {

        private final MimeMessageHelper mmh;

        /**
         * Creates a new instance of this class.
         *
         * @param mmh
         *     the mime message helper
         */
        private AddressHandlerImpl(final MimeMessageHelper mmh)
        {
            this.mmh = mmh;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setTo(final Collection<EmailAddress> addresses)
        {
            setAddressArray(this.mmh::setTo, addresses);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setCc(final Collection<EmailAddress> addresses)
        {
            setAddressArray(this.mmh::setCc, addresses);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setBcc(final Collection<EmailAddress> addresses)
        {
            setAddressArray(this.mmh::setBcc, addresses);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setFrom(final EmailAddress address)
        {
            setAddress(this.mmh::setFrom, address);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setReplyTo(final EmailAddress address)
        {
            setAddress(this.mmh::setReplyTo, address);
        }

        private static void setAddress(final MessagingConsumer<InternetAddress> setter, final EmailAddress address)
        {
            if (address != null)
            {
                try
                {
                    setter.accept(mapAddress(address));
                }
                catch (final MessagingException e)
                {
                    throw new ActionServiceException("Failed to set addressee(s)", e);
                }
            }
        }

        private static void setAddressArray(final MessagingConsumer<InternetAddress[]> setter, final Collection<EmailAddress> addresses)
        {
            if (addresses != null && !addresses.isEmpty())
            {
                try
                {
                    setter.accept(addresses.stream().map(AddressHandlerImpl::mapAddress).collect(Collectors.toList())
                            .toArray(new InternetAddress[0]));
                }
                catch (final MessagingException e)
                {
                    throw new ActionServiceException("Failed to set addressee(s)", e);
                }
            }
        }

        private static InternetAddress mapAddress(final EmailAddress address)
        {
            return address.getWrappedAddress(InternetAddress.class);
        }
    }

    /**
     * Instances of this class handle the attachment of content to emails.
     *
     * @author Axel Faust
     */
    private static class AttachmentHandlerImpl implements AttachmentHandler
    {

        private final MimeMessageHelper mmh;

        /**
         * Creates a new instance of this class.
         *
         * @param mmh
         *     the mime message helper
         */
        private AttachmentHandlerImpl(final MimeMessageHelper mmh)
        {
            this.mmh = mmh;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void addInline(final String id, final String mimetype, final Supplier<InputStream> is)
        {
            try
            {
                this.mmh.addInline(id, is::get, mimetype);
            }
            catch (final MessagingException mex)
            {
                throw new ActionServiceException("Failed to handle attachments", mex);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void addAttachment(final String id, final String mimetype, final Supplier<InputStream> is)
        {
            try
            {
                this.mmh.addAttachment(id, is::get, mimetype);
            }
            catch (final MessagingException mex)
            {
                throw new ActionServiceException("Failed to handle attachments", mex);
            }
        }
    }

    public static final SendEmailActionExecuterHelper INSTANCE = new SendEmailActionExecutorHelperImpl();

    private static final Logger LOGGER = LoggerFactory.getLogger("de.acosix.alfresco.utility.repo.action.SendMailActionExecuter");

    /**
     * {@inheritDoc}
     */
    @Override
    public EmailAddress toEmailAddress(final String address)
    {
        try
        {
            return new EmailAddress(new InternetAddress(address));
        }
        catch (final AddressException e)
        {
            throw new ActionServiceException("Failed to handle addressee " + address, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EmailAddress toEmailAddress(final String address, final String personalName)
    {
        try
        {
            return new EmailAddress(new InternetAddress(address, personalName));
        }
        catch (final UnsupportedEncodingException e)
        {
            return this.toEmailAddress(address);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EmailMessage buildEmail(final String subject, final String text, final String html, final Map<String, String> headers,
            final JavaMailSender mailSender)
    {
        ParameterCheck.mandatoryString("subject", subject);
        ParameterCheck.mandatory("headers", headers);
        ParameterCheck.mandatory("mailSender", mailSender);

        try
        {
            final MimeMessage mimeMessage = mailSender.createMimeMessage();

            mimeMessage.setSubject(subject, StandardCharsets.UTF_8.name());

            for (final Entry<String, String> headerEntry : headers.entrySet())
            {
                mimeMessage.setHeader(headerEntry.getKey(), headerEntry.getValue());
            }

            final MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);

            if (text != null && !text.trim().isEmpty() && html != null && !html.trim().isEmpty())
            {
                messageHelper.setText(text, html);
            }
            else if (text != null && !text.trim().isEmpty())
            {
                messageHelper.setText(text, false);
            }
            else if (html != null && !html.trim().isEmpty())
            {
                messageHelper.setText(html, true);
            }
            else
            {
                throw new ActionServiceException("Mail content must be specified");
            }

            return new EmailMessage(mimeMessage);
        }
        catch (final MessagingException e)
        {
            throw new ActionServiceException("Failed to create / prepare the mail message", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AddressHandler getAddressHandler(final EmailMessage message)
    {
        ParameterCheck.mandatory("message", message);

        final MimeMessage mimeMessage = message.getWrappedMessage(MimeMessage.class);
        final MimeMessageHelper mmh = new MimeMessageHelper(mimeMessage);
        return new AddressHandlerImpl(mmh);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AttachmentHandler getAttachmentHandler(final EmailMessage message)
    {
        final MimeMessage mimeMessage = message.getWrappedMessage(MimeMessage.class);
        final MimeMessageHelper mmh = new MimeMessageHelper(mimeMessage);
        return new AttachmentHandlerImpl(mmh);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendMail(final EmailMessage message, final Serializable originalToParam, final Boolean ignoreError,
            final JavaMailSender mailSender)
    {
        final MimeMessage mimeMessage = message.getWrappedMessage(MimeMessage.class);
        Address[] recipients = null;
        try
        {
            recipients = mimeMessage.getRecipients(RecipientType.TO);
            LOGGER.debug("Sending mail to {} with subject: {}", recipients, mimeMessage.getSubject());
        }
        catch (final MessagingException ignore)
        {
            // NO-OP
        }

        try
        {
            mailSender.send(mimeMessage);
            LOGGER.debug("Successfully delivered mail to configured mail server");
        }
        catch (final MailException e)
        {
            LOGGER.error("Failed to send email to {}", recipients != null ? recipients : originalToParam, e);

            if (!Boolean.TRUE.equals(ignoreError))
            {
                throw new ActionServiceException(
                        "Failed to send email to " + (recipients != null ? Arrays.toString(recipients) : originalToParam));
            }
        }
    }
}
