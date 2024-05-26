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
package de.acosix.alfresco.utility.repo.subetha3.email.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.alfresco.service.cmr.email.EmailDelivery;
import org.alfresco.service.cmr.email.EmailMessage;
import org.alfresco.service.cmr.email.EmailMessageException;
import org.alfresco.service.cmr.email.EmailService;
import org.alfresco.util.ParameterCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.AuthenticationHandler;
import org.subethamail.smtp.MessageContext;
import org.subethamail.smtp.MessageHandler;
import org.subethamail.smtp.RejectException;
import org.subethamail.smtp.io.DeferredFileOutputStream;

/**
 * @author Axel Faust
 */
public class ImprovedSubethaEmailMessageHandler implements MessageHandler
{

    private final static Logger LOGGER = LoggerFactory.getLogger(ImprovedSubethaEmailMessageHandler.class);

    /**
     * 7 megs by default. The server will buffer incoming messages to disk when they hit this limit in the DATA received.
     */
    protected final int DEFAULT_DATA_DEFERRED_SIZE = 1024 * 1024 * 7;

    protected final MessageContext messageContext;

    protected final EmailService emailService;

    protected final Consumer<String> filterMatcher;

    protected final List<EmailDelivery> deliveries = new ArrayList<>();

    protected String from;

    public ImprovedSubethaEmailMessageHandler(final MessageContext messageContext, final EmailService emailService,
            final Consumer<String> filterMatcher)
    {
        ParameterCheck.mandatory("messageContext", messageContext);
        ParameterCheck.mandatory("emailService", emailService);
        ParameterCheck.mandatory("filterMatcher", filterMatcher);
        this.messageContext = messageContext;
        this.emailService = emailService;
        this.filterMatcher = filterMatcher;
    }

    public MessageContext getMessageContext()
    {
        return this.messageContext;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void from(final String fromString) throws RejectException
    {
        try
        {
            final InternetAddress a = new InternetAddress(fromString);
            this.from = a.getAddress();
        }
        catch (final AddressException e)
        {
        }

        try
        {
            LOGGER.debug("Checking whether user is allowed to send email from {}", this.from);
            this.filterMatcher.accept(this.from);
        }
        catch (final EmailMessageException e)
        {
            throw new RejectException(554, e.getMessage());
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void recipient(final String recipient) throws RejectException
    {
        final AuthenticationHandler auth = this.messageContext.getAuthenticationHandler();
        this.deliveries.add(new EmailDelivery(recipient, this.from, auth != null ? (String) auth.getIdentity() : null));
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void data(final InputStream data) throws IOException, RejectException
    {
        if (this.deliveries.size() == 1)
        {
            final EmailDelivery delivery = this.deliveries.get(0);
            this.processDelivery(delivery, data);
        }
        else if (this.deliveries.size() > 1)
        {
            DeferredFileOutputStream dfos = null;
            try
            {
                dfos = new DeferredFileOutputStream(this.DEFAULT_DATA_DEFERRED_SIZE);
                final byte[] bytes = new byte[1024 * 8];
                int bytesRead;
                while ((bytesRead = data.read(bytes)) != -1)
                {
                    dfos.write(bytes, 0, bytesRead);
                }

                for (final EmailDelivery delivery : this.deliveries)
                {
                    this.processDelivery(delivery, dfos.getInputStream());
                }
            }
            finally
            {
                try
                {
                    dfos.close();
                }
                catch (final Exception ignore)
                {
                }
            }
        }
    }

    protected void processDelivery(final EmailDelivery delivery, final InputStream data) throws RejectException
    {
        EmailMessage emailMessage;
        try
        {
            emailMessage = new ImprovedSubethaEmailMessage(data);
            this.emailService.importMessage(delivery, emailMessage);
        }
        catch (final EmailMessageException e)
        {
            LOGGER.debug("Rejecting email after exception", e);
            throw new RejectException(554, e.getMessage());
        }
        catch (final Throwable e)
        {
            LOGGER.error("Rejecting email after unexpected exception", e);
            throw new RejectException(554, "An internal error prevented mail delivery.");
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void done()
    {
        this.deliveries.clear();
    }
}
