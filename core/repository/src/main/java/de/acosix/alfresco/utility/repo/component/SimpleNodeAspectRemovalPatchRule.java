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
package de.acosix.alfresco.utility.repo.component;

import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.PropertyCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Axel Faust
 */
public class SimpleNodeAspectRemovalPatchRule implements NodePatchRule, InitializingBean
{

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleNodeAspectRemovalPatchRule.class);

    protected NamespaceService namespaceService;

    protected DictionaryService dictionaryService;

    protected NodeService nodeService;

    protected String aspectName;

    protected QName aspectQName;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "namespaceService", this.namespaceService);
        PropertyCheck.mandatory(this, "dictionaryService", this.dictionaryService);
        PropertyCheck.mandatory(this, "nodeService", this.nodeService);

        PropertyCheck.mandatory(this, "aspectName", this.aspectName);

        this.aspectQName = QName.resolveToQName(this.namespaceService, this.aspectName);
        if (this.aspectQName == null)
        {
            throw new IllegalStateException("Association  name " + this.aspectName + " cannot be resolved to a QName");
        }

        final AspectDefinition aspectDef = this.dictionaryService.getAspect(this.aspectQName);
        if (aspectDef == null)
        {
            throw new IllegalStateException("The aspect " + this.aspectName + " is not defined in the data model");
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
     * @param nodeService
     *            the nodeService to set
     */
    public void setNodeService(final NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    /**
     * @param aspectName
     *            the aspectName to set
     */
    public void setAspectName(final String aspectName)
    {
        this.aspectName = aspectName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void apply(final NodeRef node)
    {
        if (this.nodeService.hasAspect(node, this.aspectQName))
        {
            LOGGER.debug("Removing aspect {} from node {}", this.aspectQName, node);
            this.nodeService.removeAspect(node, this.aspectQName);
        }
    }

}
