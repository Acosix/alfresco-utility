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
package de.acosix.alfresco.utility.repo.batch;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.alfresco.repo.batch.BatchProcessWorkProvider;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.LimitBy;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.PropertyCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * This class provides a generic work provider for batch operations that process generic {@link NodeRef NodeRefs} as units of work, querying
 * with transactional metadata query-compatible CMIS queries and avoiding problematic preloading of node data, which can delay batch
 * processing and in the worst case overwhelm transactional caches. The queries make use of pagination using a configured textual property
 * as from/to restrictions.
 *
 * @author Axel Faust
 */
public class PropertyOrderedTransactionalNodeBatchWorkProvider implements BatchProcessWorkProvider<NodeRef>, InitializingBean
{

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyOrderedTransactionalNodeBatchWorkProvider.class);

    protected NamespaceService namespaceService;

    protected DictionaryService dictionaryService;

    protected SearchService searchService;

    protected String typeName;

    protected String propertyName;

    protected String runAsUser = AuthenticationUtil.getSystemUserName();

    protected QName typeQName;

    protected QName propertyQName;

    protected char maxCharacter = '\0';

    protected boolean done;

    protected boolean useCharacterUpperBound = true;

    protected final Set<NodeRef> retrievedNodes = new HashSet<>();

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "namespaceService", this.namespaceService);
        PropertyCheck.mandatory(this, "searchService", this.searchService);

        if (this.typeQName == null)
        {
            PropertyCheck.mandatory(this, "dictionaryService", this.dictionaryService);
            PropertyCheck.mandatory(this, "typeName", this.typeName);

            this.typeQName = QName.resolveToQName(this.namespaceService, this.typeName);

            if (this.typeQName == null)
            {
                throw new IllegalStateException("Type name " + this.typeName + " cannot be resolved to a QName");
            }

            final TypeDefinition typeDef = this.dictionaryService.getType(this.typeQName);
            if (typeDef == null)
            {
                throw new IllegalStateException("The type " + this.typeName + " is not defined in the data model");
            }
        }

        if (this.propertyQName == null)
        {
            PropertyCheck.mandatory(this, "dictionaryService", this.dictionaryService);
            PropertyCheck.mandatory(this, "propertyName", this.propertyName);

            this.propertyQName = QName.resolveToQName(this.namespaceService, this.propertyName);

            if (this.propertyQName == null)
            {
                throw new IllegalStateException("Property name " + this.propertyName + " cannot be resolved to a QName");
            }

            final PropertyDefinition propertyDef = this.dictionaryService.getProperty(this.propertyQName);
            if (propertyDef == null)
            {
                throw new IllegalStateException("The property " + this.propertyName + " is not defined in the data model");
            }
            if (!DataTypeDefinition.TEXT.equals(propertyDef.getDataType().getName()))
            {
                throw new IllegalStateException("The property " + this.propertyName + " is not defined as a d:text property");
            }
        }
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
     * @param searchService
     *            the searchService to set
     */
    public void setSearchService(final SearchService searchService)
    {
        this.searchService = searchService;
    }

    /**
     * @param typeName
     *            the typeName to set
     */
    public void setTypeName(final String typeName)
    {
        this.typeName = typeName;
    }

    /**
     * @param propertyName
     *            the propertyName to set
     */
    public void setPropertyName(final String propertyName)
    {
        this.propertyName = propertyName;
    }

    /**
     * @param runAsUser
     *            the runAsUser to set
     */
    public void setRunAsUser(final String runAsUser)
    {
        this.runAsUser = runAsUser;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public int getTotalEstimatedWorkSize()
    {
        // can't efficiently determine number of affected nodes without doing an actual (expensive) query
        return 0;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Collection<NodeRef> getNextWork()
    {
        final List<NodeRef> nextWork = new ArrayList<>();
        AuthenticationUtil.runAs(() -> {
            while (!this.done && nextWork.isEmpty())
            {
                final char lastMaxCharacter = this.maxCharacter;
                // check the last limit character (for name-based pagination to keep DB query highly selective)
                switch (this.maxCharacter)
                {
                    case '\0':
                        this.maxCharacter = '0';
                        break;
                    case '9':
                        this.maxCharacter = 'A';
                        break;
                    case 'Z':
                        this.maxCharacter = 'a';
                        break;
                    case 'z':
                        this.useCharacterUpperBound = false;
                        break;
                    default:
                        this.maxCharacter += 1;
                }

                final SearchParameters sp = new SearchParameters();
                sp.setLanguage(SearchService.LANGUAGE_CMIS_ALFRESCO);
                sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);

                final String query;
                if (this.useCharacterUpperBound)
                {
                    // query with upper bound as long as we are in a sensible ASCII range
                    final MessageFormat mf = new MessageFormat(
                            lastMaxCharacter != '\0' ? "SELECT * FROM {0} P WHERE P.{1} <= ''{2}'' AND P.{1} > ''{3}''"
                                    : "SELECT * FROM {0} P WHERE P.{1} <= ''{2}''",
                            Locale.ENGLISH);
                    query = mf.format(new Object[] { this.typeQName.toPrefixString(this.namespaceService),
                            this.propertyQName.toPrefixString(this.namespaceService), String.valueOf(this.maxCharacter),
                            String.valueOf(lastMaxCharacter) });
                }
                else
                {
                    // for user names in unicode and beyond ASCII range we can't really do name-based pagination anymore
                    final MessageFormat mf = new MessageFormat(
                            lastMaxCharacter != '\0' ? "SELECT * FROM {0} P WHERE P.{1} > ''{2}''" : "SELECT * FROM {0}", Locale.ENGLISH);
                    query = mf.format(new Object[] { this.typeQName.toPrefixString(this.namespaceService),
                            this.propertyQName.toPrefixString(this.namespaceService), String.valueOf(lastMaxCharacter) });
                }
                LOGGER.debug("Generated query: {}", query);

                sp.setQuery(query);
                sp.setBulkFetchEnabled(false);

                // since we can't sort by cm:userName (default model lacks tokenise false/both, so CMIS rejects ORDER BY) we have to fetch
                // all results per iteration in one go
                sp.setLimitBy(LimitBy.UNLIMITED);
                sp.setMaxPermissionChecks(Integer.MAX_VALUE);
                sp.setMaxPermissionCheckTimeMillis(Long.MAX_VALUE);

                final ResultSet results = this.searchService.query(sp);
                try
                {
                    final List<NodeRef> resultNodes = results.getNodeRefs();
                    nextWork.addAll(resultNodes);
                }
                finally
                {
                    results.close();
                }

                // depending on DB collation (case sensitive or insensitive) we may get duplicates when we query for lower/upper case
                // initial characters
                nextWork.removeAll(this.retrievedNodes);
                LOGGER.debug("Determined unique, unprocessed nodes {}", nextWork);
                this.retrievedNodes.addAll(nextWork);

                // if we did a query without an upper bound we are done now
                this.done = !this.useCharacterUpperBound && nextWork.isEmpty();

                if (this.done)
                {
                    LOGGER.debug("Done loading work items");
                }
            }

            return null;
        }, this.runAsUser);

        return nextWork;
    }
}
