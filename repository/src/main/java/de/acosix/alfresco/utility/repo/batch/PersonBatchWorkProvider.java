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

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.batch.BatchProcessWorkProvider;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.search.LimitBy;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ParameterCheck;

/**
 * This class provides a generic work provider for batch operations that person {@link NodeRef NodeRefs} as units of work, querying with
 * transactional metadata query-compatible CMIS queries and avoiding problematic preloading of node data, which can delay batch processing
 * and in the worst case overwhelm transactional caches. The queries make use of pagination using the user name as from/to restrictions.
 *
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class PersonBatchWorkProvider implements BatchProcessWorkProvider<NodeRef>
{

    private final NamespaceService namespaceService;

    private final NodeService nodeService;

    private final PersonService personService;

    private final SearchService searchService;

    private char lastMaxCharacter = '\0';

    private boolean reuseMaxCharacter = false;

    private String lastName;

    private boolean done;

    private boolean useCharacterUpperBound = true;

    private final Set<NodeRef> retrievedNodes = new HashSet<>();

    public PersonBatchWorkProvider(final NamespaceService namespaceService, final NodeService nodeService,
            final PersonService personService, final SearchService searchService)
    {
        ParameterCheck.mandatory("namespaceService", namespaceService);
        ParameterCheck.mandatory("nodeService", nodeService);
        ParameterCheck.mandatory("personService", personService);
        ParameterCheck.mandatory("searchService", searchService);

        this.namespaceService = namespaceService;
        this.nodeService = nodeService;
        this.personService = personService;
        this.searchService = searchService;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public int getTotalEstimatedWorkSize()
    {
        return this.personService.countPeople();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Collection<NodeRef> getNextWork()
    {
        final List<NodeRef> nextWork = new ArrayList<>();

        while (!this.done && nextWork.isEmpty())
        {
            if (!this.reuseMaxCharacter)
            {
                // check the last limit character (for name-based pagination to keep DB query highly selective)
                switch (this.lastMaxCharacter)
                {
                    case '\0':
                        this.lastMaxCharacter = '0';
                        break;
                    case '9':
                        this.lastMaxCharacter = 'A';
                        break;
                    case 'Z':
                        this.lastMaxCharacter = 'a';
                        break;
                    case 'z':
                        this.useCharacterUpperBound = false;
                        break;
                    default:
                        this.lastMaxCharacter += 1;
                }
            }

            final SearchParameters sp = new SearchParameters();
            sp.setLanguage(SearchService.LANGUAGE_CMIS_ALFRESCO);
            sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
            sp.addSort("@" + ContentModel.PROP_USERNAME.toPrefixString(this.namespaceService), true);

            final String query;
            if (this.useCharacterUpperBound)
            {
                // query with upper bound as long as we are in a sensible ASCII range
                final MessageFormat mf = new MessageFormat(this.lastName != null
                        ? "SELECT * FROM {0} P WHERE P.{1} <= ''{2}'' AND P.{1} > ''{3}''" : "SELECT * FROM {0} P WHERE P.{1} <= ''{2}''",
                        Locale.ENGLISH);
                query = mf.format(new Object[] { ContentModel.TYPE_PERSON.toPrefixString(this.namespaceService),
                        ContentModel.PROP_USERNAME.toPrefixString(this.namespaceService), String.valueOf(this.lastMaxCharacter),
                        this.lastName });
            }
            else
            {
                // for user names in unicode and beyond ASCII range we can't really do name-based pagination anymore
                final MessageFormat mf = new MessageFormat(
                        this.lastName != null ? "SELECT * FROM {0} P WHERE P.{1} > ''{2}''" : "SELECT * FROM {0}", Locale.ENGLISH);
                query = mf.format(new Object[] { ContentModel.TYPE_PERSON.toPrefixString(this.namespaceService),
                        ContentModel.PROP_USERNAME.toPrefixString(this.namespaceService), this.lastName });
            }

            sp.setQuery(query);
            sp.setBulkFetchEnabled(false);
            sp.setLimitBy(LimitBy.FINAL_SIZE);
            sp.setMaxItems(100);
            sp.setLimit(100);

            final ResultSet results = this.searchService.query(sp);
            try
            {
                nextWork.addAll(results.getNodeRefs());
                // if we got exactly the amount we asked for then we should repeat the same query with a different "from" offset
                this.reuseMaxCharacter = nextWork.size() == 100;
            }
            finally
            {
                results.close();
            }

            // depending on DB collation (case sensitive or insensitive) we may get duplicates when we query for lower/upper case initial
            // characters
            nextWork.removeAll(this.retrievedNodes);
            this.retrievedNodes.addAll(nextWork);

            // if we did a query without an upper bound we are done now
            this.done = !this.useCharacterUpperBound && (!this.reuseMaxCharacter || nextWork.isEmpty());

            if (!this.done)
            {
                if (nextWork.isEmpty())
                {
                    this.lastName = String.valueOf(this.lastMaxCharacter);
                }
                else
                {
                    final NodeRef lastPerson = nextWork.get(nextWork.size() - 1);
                    final Map<QName, Serializable> personProperties = this.nodeService.getProperties(lastPerson);
                    this.lastName = DefaultTypeConverter.INSTANCE.convert(String.class, personProperties.get(ContentModel.PROP_USERNAME));
                }
            }
        }

        return nextWork;
    }
}
