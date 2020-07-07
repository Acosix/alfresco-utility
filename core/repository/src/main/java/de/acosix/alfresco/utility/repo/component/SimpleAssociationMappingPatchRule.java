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
package de.acosix.alfresco.utility.repo.component;

import java.util.Collections;
import java.util.List;

import org.alfresco.service.cmr.dictionary.AssociationDefinition;
import org.alfresco.service.cmr.dictionary.ClassDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.AssociationRef;
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
public class SimpleAssociationMappingPatchRule implements NodePatchRule, InitializingBean
{

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleAssociationMappingPatchRule.class);

    protected NamespaceService namespaceService;

    protected DictionaryService dictionaryService;

    protected NodeService nodeService;

    protected String fromAssociationName;

    protected QName fromAssociationQName;

    protected String toAssociationName;

    protected QName toAssociationQName;

    protected boolean removeOldAssociation;

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

        PropertyCheck.mandatory(this, "fromAssociationName", this.fromAssociationName);
        PropertyCheck.mandatory(this, "toAssociationName", this.toAssociationName);

        this.fromAssociationQName = QName.resolveToQName(this.namespaceService, this.fromAssociationName);
        if (this.fromAssociationQName == null)
        {
            throw new IllegalStateException("Association  name " + this.fromAssociationName + " cannot be resolved to a QName");
        }

        this.toAssociationQName = QName.resolveToQName(this.namespaceService, this.toAssociationName);
        if (this.toAssociationQName == null)
        {
            throw new IllegalStateException("Association  name " + this.toAssociationName + " cannot be resolved to a QName");
        }

        AssociationDefinition associationDef = this.dictionaryService.getAssociation(this.fromAssociationQName);
        if (associationDef == null)
        {
            throw new IllegalStateException("The association " + this.fromAssociationName + " is not defined in the data model");
        }

        associationDef = this.dictionaryService.getAssociation(this.toAssociationQName);
        if (associationDef == null)
        {
            throw new IllegalStateException("The association " + this.toAssociationName + " is not defined in the data model");
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
     * @param fromAssociationName
     *            the fromAssociationName to set
     */
    public void setFromAssociationName(final String fromAssociationName)
    {
        this.fromAssociationName = fromAssociationName;
    }

    /**
     * @param toAssociationName
     *            the toAssociationName to set
     */
    public void setToAssociationName(final String toAssociationName)
    {
        this.toAssociationName = toAssociationName;
    }

    /**
     * @param removeOldAssociation
     *            the removeOldAssociation to set
     */
    public void setRemoveOldAssociation(final boolean removeOldAssociation)
    {
        this.removeOldAssociation = removeOldAssociation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void apply(final NodeRef node)
    {

        final List<AssociationRef> assocs = this.nodeService.getTargetAssocs(node, this.fromAssociationQName);
        if (!assocs.isEmpty())
        {
            final AssociationDefinition associationDef = this.dictionaryService.getAssociation(this.fromAssociationQName);
            final ClassDefinition sourceClass = associationDef.getSourceClass();

            final QName sourceClassQName = sourceClass.getName();
            if (sourceClass.isAspect() && this.nodeService.hasAspect(node, sourceClassQName))
            {
                LOGGER.debug("Applying aspect {} to {} as required source class for {} association", sourceClassQName, node,
                        this.toAssociationQName);
                this.nodeService.addAspect(node, sourceClassQName, Collections.emptyMap());
            }

            LOGGER.debug("Mapping {} associations on node {} from {} to {}", assocs.size(), node, this.fromAssociationQName,
                    this.toAssociationQName);
            assocs.forEach(assoc -> {
                this.nodeService.createAssociation(node, assoc.getTargetRef(), this.toAssociationQName);
                if (this.removeOldAssociation)
                {
                    this.nodeService.removeAssociation(node, assoc.getTargetRef(), assoc.getTypeQName());
                }
            });
        }
        else
        {
            LOGGER.debug("Node {} does not have any association for {}", node, this.fromAssociationQName);
        }
    }

}
