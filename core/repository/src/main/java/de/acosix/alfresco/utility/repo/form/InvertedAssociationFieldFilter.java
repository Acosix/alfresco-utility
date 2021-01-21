/*
 * Copyright 2016 - 2021 Acosix GmbH
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
package de.acosix.alfresco.utility.repo.form;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.alfresco.repo.forms.AssociationFieldDefinition;
import org.alfresco.repo.forms.AssociationFieldDefinition.Direction;
import org.alfresco.repo.forms.Form;
import org.alfresco.repo.forms.FormData;
import org.alfresco.repo.forms.processor.AbstractFilter;
import org.alfresco.repo.forms.processor.node.FormFieldConstants;
import org.alfresco.service.cmr.dictionary.AssociationDefinition;
import org.alfresco.service.cmr.dictionary.ClassDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PublicServiceAccessService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.PropertyCheck;
import org.springframework.beans.factory.InitializingBean;

/**
 * Instances of this form filter provide structural definitions and persistence handling for association form fields in reversed direction
 * (target to source).
 *
 * @param <ItemType>
 *            the type of item being processed as the basis for the structural definition of the form
 *
 * @author Axel Faust
 */
public class InvertedAssociationFieldFilter<ItemType> extends AbstractFilter<ItemType, NodeRef> implements InitializingBean
{

    protected NamespaceService namespaceService;

    protected DictionaryService dictionaryService;

    protected NodeService nodeService;

    protected PublicServiceAccessService publicServiceAccessService;

    protected String associationName;

    protected String reversedFieldName;

    protected boolean treatAsReadOnly;

    protected QName associationQName;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "namespaceServive", this.namespaceService);
        PropertyCheck.mandatory(this, "dictionaryServive", this.dictionaryService);
        PropertyCheck.mandatory(this, "nodeServive", this.nodeService);
        PropertyCheck.mandatory(this, "publicServiceAccessService", this.publicServiceAccessService);

        PropertyCheck.mandatory(this, "associationName", this.associationName);

        this.associationQName = QName.resolveToQName(this.namespaceService, this.associationName);
        if (this.associationQName == null)
        {
            throw new IllegalStateException(this.associationName + " cannot be resolved to a qualified name");
        }

        final AssociationDefinition association = this.dictionaryService.getAssociation(this.associationQName);
        if (association == null)
        {
            throw new IllegalStateException(this.associationName + " cannot be resolved to a association in the data model");
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
     * @param publicServiceAccessService
     *            the publicServiceAccessService to set
     */
    public void setPublicServiceAccessService(final PublicServiceAccessService publicServiceAccessService)
    {
        this.publicServiceAccessService = publicServiceAccessService;
    }

    /**
     * @param associationName
     *            the associationName to set
     */
    public void setAssociationName(final String associationName)
    {
        this.associationName = associationName;
    }

    /**
     * @param reversedFieldName
     *            the reversedFieldName to set
     */
    public void setReversedFieldName(final String reversedFieldName)
    {
        this.reversedFieldName = reversedFieldName;
    }

    /**
     * @param treatAsReadOnly
     *            the treatAsReadOnly to set
     */
    public void setTreatAsReadOnly(final boolean treatAsReadOnly)
    {
        this.treatAsReadOnly = treatAsReadOnly;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeGenerate(final ItemType item, final List<String> fields, final List<String> forcedFields, final Form form,
            final Map<String, Object> context)
    {
        // NO-OP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterGenerate(final ItemType item, final List<String> fields, final List<String> forcedFields, final Form form,
            final Map<String, Object> context)
    {
        boolean relevant = false;

        final AssociationDefinition association = this.dictionaryService.getAssociation(this.associationQName);
        final ClassDefinition classDefinition = association.getTargetClass();
        final QName classQName = classDefinition.getName();

        if (item instanceof TypeDefinition)
        {
            final TypeDefinition realItem = (TypeDefinition) item;
            relevant = classDefinition.isAspect() ? realItem.getDefaultAspectNames().contains(classQName)
                    : this.dictionaryService.isSubClass(realItem.getName(), classQName);
        }
        else if (item instanceof NodeRef)
        {
            final NodeRef realItem = (NodeRef) item;
            relevant = classDefinition.isAspect() ? this.nodeService.hasAspect(realItem, classQName)
                    : this.dictionaryService.isSubClass(this.nodeService.getType(realItem), classQName);
        }

        if (relevant)
        {
            final String name = this.reversedFieldName;
            final String endpointType = association.getSourceClass().getName().toPrefixString(this.namespaceService);

            final AssociationFieldDefinition fieldDef = new AssociationFieldDefinition(name, endpointType, Direction.SOURCE);
            fieldDef.setLabel(name);
            fieldDef.setProtectedField(this.treatAsReadOnly);
            fieldDef.setEndpointMandatory(association.isSourceMandatory());
            fieldDef.setEndpointMany(association.isSourceMany());
            final String dataKeyName = FormFieldConstants.ASSOC_DATA_PREFIX + name.replaceFirst(":", FormFieldConstants.DATA_KEY_SEPARATOR);
            fieldDef.setDataKeyName(dataKeyName);

            form.addFieldDefinition(fieldDef);

            if (item instanceof NodeRef)
            {
                final NodeRef realItem = (NodeRef) item;

                final List<NodeRef> value = this.nodeService.getSourceAssocs(realItem, this.associationQName).stream()
                        .map(AssociationRef::getSourceRef)
                        // not all Alfresco versions have proper ACL checks on getSourceAssocs
                        .filter(n -> this.publicServiceAccessService.hasAccess(NodeService.class.getSimpleName(), "getProperties",
                                n) == AccessStatus.ALLOWED)
                        .collect(Collectors.toList());
                form.addData(dataKeyName, value);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforePersist(final ItemType item, final FormData data)
    {
        // NO-OP
        // TODO Support persistence
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPersist(final ItemType item, final FormData data, final NodeRef persistedObject)
    {
        // NO-OP
        // TODO Support persistence
    }
}
