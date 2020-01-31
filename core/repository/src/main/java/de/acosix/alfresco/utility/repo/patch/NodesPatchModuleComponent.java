/*
 * Copyright 2016 - 2019 Acosix GmbH
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
package de.acosix.alfresco.utility.repo.patch;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.batch.BatchProcessWorkProvider;
import org.alfresco.repo.batch.BatchProcessor;
import org.alfresco.repo.batch.BatchProcessor.BatchProcessWorker;
import org.alfresco.repo.batch.BatchProcessor.BatchProcessWorkerAdaptor;
import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.dictionary.ClassDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.ParameterCheck;
import org.alfresco.util.PropertyCheck;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Axel Faust
 */
public class NodesPatchModuleComponent extends AbstractModuleComponent implements InitializingBean
{

    private static final Logger LOGGER = LoggerFactory.getLogger(NodesPatchModuleComponent.class);

    protected TransactionService transactionService;

    protected NamespaceService namespaceService;

    protected DictionaryService dictionaryService;

    protected NodeService nodeService;

    protected BehaviourFilter behaviourFilter;

    protected BatchProcessWorkProvider<NodeRef> workProvider;

    protected List<NodePropertiesPatchRule> propertiesPatchRules = Collections.emptyList();

    protected List<NodePatchRule> complexPatchRules = Collections.emptyList();

    protected boolean disableAuditableBehaviour = true;

    protected List<String> disableBehaviourClassNames = Collections.emptyList();

    protected Set<QName> disableBehaviourClassQNames;

    protected int batchSize = 10;

    protected int workerThreads = 4;

    protected int loggingInterval = 100;

    protected boolean skip;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "transactionService", this.transactionService);
        PropertyCheck.mandatory(this, "namespaceService", this.namespaceService);
        PropertyCheck.mandatory(this, "dictionaryService", this.dictionaryService);
        PropertyCheck.mandatory(this, "nodeService", this.nodeService);
        PropertyCheck.mandatory(this, "behaviourFilter", this.behaviourFilter);
        PropertyCheck.mandatory(this, "workProvider", this.workProvider);

        this.disableBehaviourClassQNames = this.disableBehaviourClassNames.stream().map(className -> {
            final QName classQName = QName.resolveToQName(this.namespaceService, className);
            if (classQName == null)
            {
                throw new IllegalStateException("Class name " + className + " cannot be resolved to a QName");
            }
            final ClassDefinition classDef = this.dictionaryService.getClass(classQName);
            if (classDef == null)
            {
                throw new IllegalStateException("Class name " + className + " is not defined in the data model");
            }
            return classQName;

        }).collect(Collectors.toSet());

        if (this.skip)
        {
            // use lowest possible version number to effectively disable this component
            this.setAppliesFromVersion("0");
            this.setAppliesToVersion("0");
        }
    }

    /**
     * @param transactionService
     *            the transactionService to set
     */
    public void setTransactionService(final TransactionService transactionService)
    {
        this.transactionService = transactionService;
    }

    /**
     * @param namespaceService
     *            the namespaceService to set
     */
    public void setNamespaceService(final NamespaceService namespaceService)
    {
        this.namespaceService = namespaceService;
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
     * @param behaviourFilter
     *            the behaviourFilter to set
     */
    public void setBehaviourFilter(final BehaviourFilter behaviourFilter)
    {
        this.behaviourFilter = behaviourFilter;
    }

    /**
     * @param workProvider
     *            the workProvider to set
     */
    public void setWorkProvider(final BatchProcessWorkProvider<NodeRef> workProvider)
    {
        this.workProvider = workProvider;
    }

    /**
     * @param propertiesPatchRules
     *            the propertiesPatchRules to set
     */
    public void setPropertiesPatchRules(final List<NodePropertiesPatchRule> propertiesPatchRules)
    {
        ParameterCheck.mandatoryCollection("propertiesPatchRules", propertiesPatchRules);
        this.propertiesPatchRules = new ArrayList<>(propertiesPatchRules);
    }

    /**
     * @param complexPatchRules
     *            the complexPatchRules to set
     */
    public void setComplexPatchRules(final List<NodePatchRule> complexPatchRules)
    {
        ParameterCheck.mandatoryCollection("complexPatchRules", complexPatchRules);
        this.complexPatchRules = new ArrayList<>(complexPatchRules);
    }

    /**
     * @param disableAuditableBehaviour
     *            the disableAuditableBehaviour to set
     */
    public void setDisableAuditableBehaviour(final boolean disableAuditableBehaviour)
    {
        this.disableAuditableBehaviour = disableAuditableBehaviour;
    }

    /**
     * @param disableBehaviourClassNames
     *            the disableBehaviourClassNames to set
     */
    public void setDisableBehaviourClassNames(final List<String> disableBehaviourClassNames)
    {
        ParameterCheck.mandatory("disableBehaviourClassNames", disableBehaviourClassNames);
        this.disableBehaviourClassNames = disableBehaviourClassNames;
    }

    /**
     * @param batchSize
     *            the batchSize to set
     */
    public void setBatchSize(final int batchSize)
    {
        if (batchSize <= 0)
        {
            throw new IllegalArgumentException("'batchSize' must be a positive integer value");
        }
        this.batchSize = batchSize;
    }

    /**
     * @param workerThreads
     *            the workerThreads to set
     */
    public void setWorkerThreads(final int workerThreads)
    {
        if (workerThreads <= 0)
        {
            throw new IllegalArgumentException("'workerThreads' must be a positive integer value");
        }
        this.workerThreads = workerThreads;
    }

    /**
     * @param loggingInterval
     *            the loggingInterval to set
     */
    public void setLoggingInterval(final int loggingInterval)
    {
        if (loggingInterval <= 0)
        {
            throw new IllegalArgumentException("'loggingInterval' must be a positive integer value");
        }
        this.loggingInterval = loggingInterval;
    }

    /**
     * @param skip
     *            the skip to set
     */
    public void setSkip(final boolean skip)
    {
        this.skip = skip;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void executeInternal() throws Throwable
    {
        if (!this.propertiesPatchRules.isEmpty() || !this.complexPatchRules.isEmpty())
        {
            final BatchProcessor<NodeRef> patchBatchProcessor = new BatchProcessor<>(this.getName(),
                    this.transactionService.getRetryingTransactionHelper(), this.workProvider, this.workerThreads, this.batchSize, null,
                    LogFactory.getLog(NodesPatchModuleComponent.class), this.loggingInterval);

            final BatchProcessWorker<NodeRef> worker = new NodesPatchModuleComponentWorker();
            patchBatchProcessor.process(worker, true);
        }
        else
        {
            LOGGER.warn("Not running patch {} as neither properties nor complex patch rules have been defined", this.getName());
        }
    }

    /**
     *
     * @author Axel Faust
     */
    protected class NodesPatchModuleComponentWorker extends BatchProcessWorkerAdaptor<NodeRef>
    {

        /**
         * {@inheritDoc}
         */
        @Override
        public void beforeProcess() throws Throwable
        {
            AuthenticationUtil.pushAuthentication();
            AuthenticationUtil.setRunAsUserSystem();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void afterProcess() throws Throwable
        {
            AuthenticationUtil.popAuthentication();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void process(final NodeRef entry) throws Throwable
        {
            final Map<QName, Serializable> properties = NodesPatchModuleComponent.this.nodeService.getProperties(entry);
            final Map<QName, Serializable> unmodProperties = Collections.unmodifiableMap(properties);

            final Map<QName, Serializable> propertyUpdates = new HashMap<>();
            LOGGER.debug("Applying {} properties patch rules to {}", NodesPatchModuleComponent.this.propertiesPatchRules.size(), entry);
            NodesPatchModuleComponent.this.propertiesPatchRules.forEach(rule -> propertyUpdates.putAll(rule.apply(entry, unmodProperties)));

            if (NodesPatchModuleComponent.this.disableAuditableBehaviour)
            {
                NodesPatchModuleComponent.this.behaviourFilter.disableBehaviour(entry, ContentModel.ASPECT_AUDITABLE);
            }

            NodesPatchModuleComponent.this.disableBehaviourClassQNames
                    .forEach(classQName -> NodesPatchModuleComponent.this.behaviourFilter.disableBehaviour(entry, classQName));
            try
            {
                if (!propertyUpdates.isEmpty())
                {
                    // need to handle nulls differently since addProperties unfortunately stores null instead of removing the properties
                    final Set<QName> propertiesToRemove = propertyUpdates.entrySet().stream()
                            .filter(mapEntry -> mapEntry.getValue() == null).map(Map.Entry::getKey).collect(Collectors.toSet());
                    propertyUpdates.keySet().removeAll(propertiesToRemove);
                    if (!propertyUpdates.isEmpty())
                    {
                        LOGGER.debug("Updating properties on {} with updates: {}", entry, propertyUpdates);
                        NodesPatchModuleComponent.this.nodeService.addProperties(entry, propertyUpdates);
                    }
                    if (!propertiesToRemove.isEmpty())
                    {
                        LOGGER.debug("Removing properties {} from {}", propertiesToRemove, entry);
                    }
                }

                LOGGER.debug("Applying {} complex patch rules to {}", NodesPatchModuleComponent.this.complexPatchRules.size(), entry);
                NodesPatchModuleComponent.this.complexPatchRules.forEach(rule -> rule.apply(entry));
            }
            finally
            {
                if (NodesPatchModuleComponent.this.disableAuditableBehaviour)
                {
                    NodesPatchModuleComponent.this.behaviourFilter.enableBehaviour(entry, ContentModel.ASPECT_AUDITABLE);
                }

                NodesPatchModuleComponent.this.disableBehaviourClassQNames
                        .forEach(classQName -> NodesPatchModuleComponent.this.behaviourFilter.enableBehaviour(entry, classQName));
            }
        }

    }
}
