/*
 * Copyright 2017 Acosix GmbH
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

import org.alfresco.model.ContentModel;
import org.alfresco.repo.batch.BatchProcessWorkProvider;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.LimitBy;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.util.ParameterCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides a generic work provider for batch operations that person {@link NodeRef NodeRefs} as units of work, querying with
 * transactional metadata query-compatible CMIS queries and avoiding problematic preloading of node data, which can delay batch processing
 * and in the worst case overwhelm transactional caches. The queries make use of pagination using the user name as from/to restrictions.
 *
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class PersonBatchWorkProvider implements BatchProcessWorkProvider<NodeRef>
{

    private static final Logger LOGGER = LoggerFactory.getLogger(PersonBatchWorkProvider.class);

    private final NamespaceService namespaceService;

    private final PersonService personService;

    private final SearchService searchService;

    private char maxCharacter = '\0';

    private boolean done;

    private boolean useCharacterUpperBound = true;

    private final Set<NodeRef> retrievedNodes = new HashSet<>();

    private final String runAsUser;

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
    }

    // for backwards compatibility
    public PersonBatchWorkProvider(final NamespaceService namespaceService, final NodeService nodeService,
            final PersonService personService, final SearchService searchService)
    {
        this(namespaceService, personService, searchService, AuthenticationUtil.getRunAsUser());
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
                    query = mf.format(new Object[] { ContentModel.TYPE_PERSON.toPrefixString(this.namespaceService),
                            ContentModel.PROP_USERNAME.toPrefixString(this.namespaceService), String.valueOf(this.maxCharacter),
                            String.valueOf(lastMaxCharacter) });
                }
                else
                {
                    // for user names in unicode and beyond ASCII range we can't really do name-based pagination anymore
                    final MessageFormat mf = new MessageFormat(
                            lastMaxCharacter != '\0' ? "SELECT * FROM {0} P WHERE P.{1} > ''{2}''" : "SELECT * FROM {0}", Locale.ENGLISH);
                    query = mf.format(new Object[] { ContentModel.TYPE_PERSON.toPrefixString(this.namespaceService),
                            ContentModel.PROP_USERNAME.toPrefixString(this.namespaceService), String.valueOf(lastMaxCharacter) });
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
                LOGGER.debug("Determined unique, unprocessed people nodes {}", nextWork);
                this.retrievedNodes.addAll(nextWork);

                // if we did a query without an upper bound we are done now
                this.done = !this.useCharacterUpperBound && nextWork.isEmpty();

                if (this.done)
                {
                    LOGGER.debug("Done loading person work items");
                }
            }

            return null;
        }, this.runAsUser);

        return nextWork;
    }
}
