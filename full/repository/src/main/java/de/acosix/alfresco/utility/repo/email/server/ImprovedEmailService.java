/*
 * Copyright 2016 - 2020 Acosix GmbH
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

import org.alfresco.email.server.handler.EmailMessageHandler;
import org.alfresco.service.cmr.email.EmailService;
import org.alfresco.service.namespace.QName;

/**
 * @author Axel Faust
 */
public interface ImprovedEmailService extends EmailService
{

    /**
     * Registers an email message handler with a specific node type
     *
     * @param nodeTypeName
     *            the name of the node type for which to register
     * @param messageHandler
     *            the message handler to register
     */
    void register(String nodeTypeName, EmailMessageHandler messageHandler);

    /**
     * Registers an email message handler with a specific node type
     *
     * @param nodeTypeQName
     *            the qualified name of the node type for which to register
     * @param messageHandler
     *            the message handler to register
     */
    void register(QName nodeTypeQName, EmailMessageHandler messageHandler);
}
