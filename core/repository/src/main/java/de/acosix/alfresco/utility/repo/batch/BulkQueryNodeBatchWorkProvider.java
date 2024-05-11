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
package de.acosix.alfresco.utility.repo.batch;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.alfresco.repo.batch.BatchProcessWorkProvider;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.LimitBy;
import org.alfresco.service.cmr.search.QueryConsistency;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.util.PropertyCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * This class provides a generic work provider for batch operations that process generic {@link NodeRef NodeRefs} as units of work, querying
 * relevant work items with a one-off bulk, transactional metadata query.
 *
 * @author Axel Faust
 */
public class BulkQueryNodeBatchWorkProvider implements BatchProcessWorkProvider<NodeRef>, InitializingBean
{

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkQueryNodeBatchWorkProvider.class);

    protected SearchService searchService;

    protected String language = SearchService.LANGUAGE_FTS_ALFRESCO;

    protected String query;

    protected QueryConsistency queryConsistency;

    protected volatile boolean queried = false;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "searchService", this.searchService);
        PropertyCheck.mandatory(this, "language", this.language);
        PropertyCheck.mandatory(this, "query", this.query);
    }

    /**
     * @param searchService
     *     the searchService to set
     */
    public void setSearchService(final SearchService searchService)
    {
        this.searchService = searchService;
    }

    /**
     * @param language
     *     the language to set
     */
    public void setLanguage(final String language)
    {
        this.language = language;
    }

    /**
     * @param query
     *     the query to set
     */
    public void setQuery(final String query)
    {
        this.query = query;
    }

    /**
     * @param queryConsistency
     *     the queryConsistency to set
     */
    public void setQueryConsistency(final QueryConsistency queryConsistency)
    {
        this.queryConsistency = queryConsistency;
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
        final Collection<NodeRef> nextWork;

        if (this.queried)
        {
            nextWork = Collections.emptyList();
        }
        else
        {
            nextWork = AuthenticationUtil.runAsSystem(() -> {
                final SearchParameters sp = new SearchParameters();
                sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
                sp.setLanguage(this.language);
                sp.setQuery(this.query);
                if (this.queryConsistency != null)
                {
                    sp.setQueryConsistency(this.queryConsistency);
                }
                sp.setMaxItems(Integer.MAX_VALUE);
                sp.setMaxPermissionChecks(Integer.MAX_VALUE);
                sp.setMaxPermissionCheckTimeMillis(Long.MAX_VALUE);
                sp.setLimit(Integer.MAX_VALUE);
                sp.setLimitBy(LimitBy.UNLIMITED);
                sp.setBulkFetchEnabled(false);

                final ResultSet results = this.searchService.query(sp);
                try
                {
                    final List<NodeRef> resultNodes = results.getNodeRefs();
                    LOGGER.debug("Loaded {} work items", resultNodes.size());
                    return resultNodes;
                }
                finally
                {
                    this.queried = true;
                    results.close();
                }
            });
        }

        return nextWork;
    }
}
