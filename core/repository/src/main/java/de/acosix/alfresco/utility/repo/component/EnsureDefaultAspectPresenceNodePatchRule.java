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
package de.acosix.alfresco.utility.repo.component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.PropertyCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * Simple patch rule which ensures that all default aspects are applied to a node. This patch rule should only be used on nodes which can
 * only have aspects without mandatory non-default properties / associations.
 *
 * @author Axel Faust
 */
public class EnsureDefaultAspectPresenceNodePatchRule implements NodePatchRule, InitializingBean
{

    private static final Logger LOGGER = LoggerFactory.getLogger(EnsureDefaultAspectPresenceNodePatchRule.class);

    protected DictionaryService dictionaryService;

    protected NodeService nodeService;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "dictionaryService", this.dictionaryService);
        PropertyCheck.mandatory(this, "nodeService", this.nodeService);
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
     * {@inheritDoc}
     */
    @Override
    public void apply(final NodeRef node)
    {
        final QName type = this.nodeService.getType(node);
        final Set<QName> aspectQNames = this.nodeService.getAspects(node);
        final TypeDefinition anonymousType = this.dictionaryService.getAnonymousType(type, aspectQNames);
        final Set<QName> defaultAspectQNames = anonymousType.getDefaultAspectNames();

        final Set<QName> missingAspectQnames = new HashSet<>(defaultAspectQNames);
        missingAspectQnames.removeAll(aspectQNames);

        if (!missingAspectQnames.isEmpty())
        {
            LOGGER.debug("Applying missing aspects {} to {}", missingAspectQnames, node);

            missingAspectQnames.forEach(aspectQName -> this.nodeService.addAspect(node, aspectQName, Collections.emptyMap()));
        }
    }
}
