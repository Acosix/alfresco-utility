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
package de.acosix.alfresco.utility.repo.module;

import org.alfresco.repo.domain.node.NodeDAO;
import org.alfresco.repo.module.ModuleStarter;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.PropertyCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;

/**
 * This sub-class ensures that before modules are actually started, a server entity for the current server is actually created / inserted
 * into the database. This works around the issue described in https://issues.alfresco.com/jira/browse/ALF-22091 . This sub-class does this
 * by forcing the creation of an alf_transaction entity, which in turn - in versions prior to Alfresco 6.3 affected by the referenced issue
 * - will include the creation of an alf_server entity, if none exists yet for the current server.
 *
 * @author Axel Faust
 */
public class ServerEnsuringModuleStarter extends ModuleStarter
{

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerEnsuringModuleStarter.class);

    protected NodeDAO nodeDAO;

    protected TransactionService transactionService;

    /**
     * @param nodeDAO
     *            the nodeDAO to set
     */
    public void setNodeDAO(final NodeDAO nodeDAO)
    {
        this.nodeDAO = nodeDAO;
    }

    /**
     * @param transactionService
     *            the transactionService to set
     */
    @Override
    public void setTransactionService(final TransactionService transactionService)
    {
        this.transactionService = transactionService;
        super.setTransactionService(transactionService);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void onBootstrap(final ApplicationEvent event)
    {
        PropertyCheck.mandatory(this, "nodeDAO", this.nodeDAO);
        PropertyCheck.mandatory(this, "transactionService", this.transactionService);
        this.transactionService.getRetryingTransactionHelper().doInTransaction(() -> {
            final Long currentTransactionId = this.nodeDAO.getCurrentTransactionId(true);
            LOGGER.debug("Created empty transaction {} to ensure alf_server is initialised for current server", currentTransactionId);
            return null;
        });

        super.onBootstrap(event);
    }
}
