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
package de.acosix.alfresco.utility.repo.batch;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.util.ParameterCheck;
import org.alfresco.util.PropertyCheck;

/**
 * This class extends the .
 *
 * @author Axel Faust
 */
public class PersonBatchWorkProvider extends PropertyOrderedTransactionalNodeBatchWorkProvider
{

    protected PersonService personService;

    /**
     * Creates a new instance of this work provider with preset values for
     * {@link PropertyOrderedTransactionalNodeBatchWorkProvider#typeQName selecting cm:person nodes} and ordering based on
     * {@link PropertyOrderedTransactionalNodeBatchWorkProvider#propertyQName the user name}. This constructor is primarily intended to
     * support use cases where an instance is configured via Spring.
     */
    public PersonBatchWorkProvider()
    {
        this.typeQName = ContentModel.TYPE_PERSON;
        this.propertyQName = ContentModel.PROP_USERNAME;
    }

    /**
     * Creates a new instance of this work provider with preset values for
     * {@link PropertyOrderedTransactionalNodeBatchWorkProvider#typeQName selecting cm:person nodes} and ordering based on
     * {@link PropertyOrderedTransactionalNodeBatchWorkProvider#propertyQName the user name}.
     *
     * @param namespaceService
     *            the namespace service to use for resolving qualified names to prefixed String values
     * @param personService
     *            the person service to use for estimating the number of nodes to be processed
     * @param searchService
     *            the search service to use for incrementally loading ordered sub-sets of person nodes
     * @param runAsUser
     *            the user to use for the authentication context of any search operations
     */
    public PersonBatchWorkProvider(final NamespaceService namespaceService, final PersonService personService,
            final SearchService searchService, final String runAsUser)
    {
        ParameterCheck.mandatory("namespaceService", namespaceService);
        ParameterCheck.mandatory("personService", personService);
        ParameterCheck.mandatory("searchService", searchService);
        ParameterCheck.mandatoryString("runAsUser", runAsUser);

        this.namespaceService = namespaceService;
        this.personService = personService;
        this.searchService = searchService;
        this.runAsUser = runAsUser;

        this.typeQName = ContentModel.TYPE_PERSON;
        this.propertyQName = ContentModel.PROP_USERNAME;
    }

    /**
     * Creates a new instance of this work provider with preset values for
     * {@link PropertyOrderedTransactionalNodeBatchWorkProvider#typeQName selecting cm:person nodes} and ordering based on
     * {@link PropertyOrderedTransactionalNodeBatchWorkProvider#propertyQName the user name}. The constructed instance will default to use
     * the {@link AuthenticationUtil#getRunAsUser() current user} for any search operations.
     *
     * @param namespaceService
     *            the namespace service to use for resolving qualified names to prefixed String values
     * @param personService
     *            the person service to use for estimating the number of nodes to be processed
     * @param searchService
     *            the search service to use for incrementally loading ordered sub-sets of person nodes
     *
     */
    public PersonBatchWorkProvider(final NamespaceService namespaceService, final PersonService personService,
            final SearchService searchService)
    {
        this(namespaceService, personService, searchService, AuthenticationUtil.getRunAsUser());
    }

    /**
     * Creates a new instance of this work provider with preset values for
     * {@link PropertyOrderedTransactionalNodeBatchWorkProvider#typeQName selecting cm:person nodes} and ordering based on
     * {@link PropertyOrderedTransactionalNodeBatchWorkProvider#propertyQName the user name}. The constructed instance will default to use
     * the {@link AuthenticationUtil#getRunAsUser() current user} for any search operations.
     *
     * @param namespaceService
     *            the namespace service to use for resolving qualified names to prefixed String values
     * @param nodeService
     *            a legacy parameter for backwards compatiblity with previous versions - this parameter will be ignored
     * @param personService
     *            the person service to use for estimating the number of nodes to be processed
     * @param searchService
     *            the search service to use for incrementally loading ordered sub-sets of person nodes
     *
     */
    public PersonBatchWorkProvider(final NamespaceService namespaceService, final NodeService nodeService,
            final PersonService personService, final SearchService searchService)
    {
        this(namespaceService, personService, searchService, AuthenticationUtil.getRunAsUser());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "personService", this.personService);
        super.afterPropertiesSet();
    }

    /**
     * @param personService
     *            the personService to set
     */
    public void setPersonService(final PersonService personService)
    {
        this.personService = personService;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public int getTotalEstimatedWorkSize()
    {
        // called also at end of TxnCallback
        final Integer estimate = AuthenticationUtil.runAs(() -> {
            return Integer.valueOf(this.personService.countPeople());
        }, this.runAsUser);
        return estimate.intValue();
    }
}
