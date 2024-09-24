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

import org.alfresco.email.server.EmailServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.AuthenticationHandlerFactory;
import org.subethamail.smtp.MessageContext;
import org.subethamail.smtp.auth.EasyAuthenticationHandlerFactory;
import org.subethamail.smtp.auth.LoginFailedException;
import org.subethamail.smtp.server.SMTPServer;
import org.subethamail.smtp.server.SMTPServer.Builder;

/**
 * @author Axel Faust
 */
public class ImprovedSubethaEmailServer extends EmailServer
{

    private final static Logger LOGGER = LoggerFactory.getLogger(ImprovedSubethaEmailServer.class);

    private SMTPServer serverImpl;

    protected ImprovedSubethaEmailServer()
    {
        super();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void startup()
    {
        final Builder serverBuilder = SMTPServer.port(this.getPort()).hostName(this.getDomain()).maxConnections(this.getMaxConnections())
                .hideTLS(this.isHideTLS()).enableTLS(this.isEnableTLS()).requireTLS(this.isRequireTLS())
                .messageHandlerFactory(messageContext -> new ImprovedSubethaEmailMessageHandler(messageContext, this.getEmailService(),
                        this::filterSender));

        if (this.isAuthenticate())
        {
            final AuthenticationHandlerFactory authenticationHandler = new EasyAuthenticationHandlerFactory(this::login);
            serverBuilder.authenticationHandlerFactory(authenticationHandler);
        }

        this.serverImpl = serverBuilder.build();

        this.serverImpl.start();
        LOGGER.info("Inbound SMTP Email Server has started successfully, on hostName:{} port:{}", this.getDomain(), this.getPort());
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void shutdown()
    {
        this.serverImpl.stop();
        LOGGER.info("Inbound SMTP Email Server has stopped successfully");
    }

    protected void login(final String userName, final String password, final MessageContext context) throws LoginFailedException
    {
        if (!ImprovedSubethaEmailServer.this.authenticateUserNamePassword(userName, password.toCharArray()))
        {
            throw new LoginFailedException("unable to log on");
        }
        LOGGER.debug("User authenticated successfully {}", userName);
    }
}
