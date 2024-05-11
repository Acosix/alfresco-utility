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
package de.acosix.alfresco.utility.repo.virtual;

import java.util.Map;

import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.TransactionalResourceHelper;
import org.alfresco.repo.virtual.ActualEnvironment;
import org.alfresco.repo.virtual.AlfrescoEnviroment;
import org.alfresco.repo.virtual.VirtualizationException;
import org.alfresco.repo.virtual.ref.Reference;
import org.alfresco.repo.virtual.store.VirtualStoreImpl;
import org.alfresco.repo.virtual.template.VirtualFolderDefinition;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * @author Axel Faust
 */
public class RelaxedVirtualStoreImpl extends VirtualStoreImpl
{

    // copied from base class due to visibility restrictions
    private static final String VIRTUAL_FOLDER_DEFINITION = "virtualfolder.definition";

    protected ActualEnvironment environment;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void setEnvironment(final ActualEnvironment environment)
    {
        super.setEnvironment(environment);
        this.environment = environment;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public VirtualFolderDefinition resolveVirtualFolderDefinition(final Reference reference) throws VirtualizationException
    {
        final ServiceRegistry serviceRegistry = ((AlfrescoEnviroment) this.environment).getServiceRegistry();
        final RetryingTransactionHelper transactionHelper = serviceRegistry.getRetryingTransactionHelper();

        return transactionHelper.doInTransaction(() -> {
            final NodeRef key = reference.toNodeRef();

            final Map<NodeRef, VirtualFolderDefinition> definitionsCache = TransactionalResourceHelper.getMap(VIRTUAL_FOLDER_DEFINITION);

            VirtualFolderDefinition virtualFolderDefinition = definitionsCache.get(key);

            if (virtualFolderDefinition == null)
            {
                virtualFolderDefinition = reference.execute(new RelaxedApplyTemplateMethod(RelaxedVirtualStoreImpl.this.environment));
                definitionsCache.put(key, virtualFolderDefinition);
            }

            return virtualFolderDefinition;
        }, true, false);
    }
}
