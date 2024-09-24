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
package de.acosix.alfresco.utility.repo.email.server;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.email.server.EmailServiceImpl;
import org.alfresco.email.server.handler.EmailMessageHandler;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ParameterCheck;
import org.alfresco.util.PropertyCheck;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Axel Faust
 */
public class ImprovedEmailServiceImpl extends EmailServiceImpl implements InitializingBean, ImprovedEmailService
{

    protected NamespaceService namespaceService;

    protected final Map<String, EmailMessageHandler> messageHandlers = new HashMap<>();

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "namespaceService", this.namespaceService);
        super.setEmailMessageHandlerMap(this.messageHandlers);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void register(final String nodeTypeName, final EmailMessageHandler messageHandler)
    {
        ParameterCheck.mandatoryString("nodeTypeName", nodeTypeName);
        ParameterCheck.mandatory("messageHandler", messageHandler);
        this.messageHandlers.put(nodeTypeName, messageHandler);
        super.setEmailMessageHandlerMap(this.messageHandlers);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void register(final QName nodeTypeQName, final EmailMessageHandler messageHandler)
    {
        ParameterCheck.mandatory("nodeTypeQName", nodeTypeQName);
        ParameterCheck.mandatory("messageHandler", messageHandler);
        this.messageHandlers.put(nodeTypeQName.toPrefixString(this.namespaceService), messageHandler);
        super.setEmailMessageHandlerMap(this.messageHandlers);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNamespaceService(final NamespaceService namespaceService)
    {
        super.setNamespaceService(namespaceService);
        this.namespaceService = namespaceService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, EmailMessageHandler> getEmailMessageHandlerMap()
    {
        final Map<String, EmailMessageHandler> copy = new HashMap<>(this.messageHandlers);
        return copy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEmailMessageHandlerMap(final Map<String, EmailMessageHandler> emailMessageHandlerMap)
    {
        ParameterCheck.mandatory("emailMessageHandlerMap", emailMessageHandlerMap);
        this.messageHandlers.clear();
        this.messageHandlers.putAll(emailMessageHandlerMap);
        super.setEmailMessageHandlerMap(this.messageHandlers);
    }

}
