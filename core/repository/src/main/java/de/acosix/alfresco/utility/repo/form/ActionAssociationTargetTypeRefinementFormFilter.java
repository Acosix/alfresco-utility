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
package de.acosix.alfresco.utility.repo.form;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.forms.AssociationFieldDefinition;
import org.alfresco.repo.forms.FieldDefinition;
import org.alfresco.repo.forms.Form;
import org.alfresco.repo.forms.FormData;
import org.alfresco.repo.forms.processor.AbstractFilter;
import org.alfresco.repo.forms.processor.action.ActionFormResult;
import org.alfresco.service.cmr.action.ActionDefinition;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.PropertyCheck;
import org.springframework.beans.factory.InitializingBean;

/**
 * This filter enhances the association parameter definitions of actions with the expected association target types. This is necessary since
 * default action form association fields only specify {@link ContentModel#TYPE_CMOBJECT cm:cmobject} as the required type.
 *
 * @author Axel Faust
 */
public class ActionAssociationTargetTypeRefinementFormFilter extends AbstractFilter<ActionDefinition, ActionFormResult>
        implements InitializingBean
{

    protected NamespaceService namespaceService;

    protected String actionName;

    protected Map<String, String> typeNamesByField;

    protected final Map<String, QName> typeQNamesByField = new HashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "namespaceService", this.namespaceService);
        PropertyCheck.mandatory(this, "actionName", this.actionName);
        PropertyCheck.mandatory(this, "typeNamesByField", this.typeNamesByField);

        this.typeNamesByField.forEach((k, v) -> {
            final QName qn = QName.resolveToQName(this.namespaceService, v);
            if (qn == null)
            {
                throw new IllegalStateException(v + " cannot be resolved to a qualified name");
            }
            this.typeQNamesByField.put(k, qn);
        });

        super.register();
    }

    /**
     * @param namespaceService
     *     the namespaceService to set
     */
    public void setNamespaceService(final NamespaceService namespaceService)
    {
        this.namespaceService = namespaceService;
    }

    /**
     * @param actionName
     *     the actionName to set
     */
    public void setActionName(final String actionName)
    {
        this.actionName = actionName;
    }

    /**
     * @param typeNamesByField
     *     the typeNamesByField to set
     */
    public void setTypeNamesByField(final Map<String, String> typeNamesByField)
    {
        this.typeNamesByField = typeNamesByField;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void beforeGenerate(final ActionDefinition item, final List<String> fields, final List<String> forcedFields, final Form form,
            final Map<String, Object> context)
    {
        // NO-OP
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterGenerate(final ActionDefinition item, final List<String> fields, final List<String> forcedFields, final Form form,
            final Map<String, Object> context)
    {
        if (this.actionName.equals(item.getName()))
        {
            final List<FieldDefinition> fieldDefs = new ArrayList<>(form.getFieldDefinitions());
            final String cmObject = ContentModel.TYPE_CMOBJECT.toPrefixString(this.namespaceService);
            final List<AssociationFieldDefinition> fieldsToHandle = fieldDefs.stream()
                    .filter(f -> this.typeQNamesByField.containsKey(f.getName())).filter(AssociationFieldDefinition.class::isInstance)
                    .map(AssociationFieldDefinition.class::cast).filter(f -> cmObject.equals(f.getEndpointType()))
                    .collect(Collectors.toList());

            form.getFieldDefinitions().removeAll(fieldsToHandle);

            fieldsToHandle.stream().map(f -> {
                final AssociationFieldDefinition newF = new AssociationFieldDefinition(f.getName(),
                        this.typeQNamesByField.get(f.getName()).toPrefixString(this.namespaceService), f.getEndpointDirection());
                newF.setEndpointMandatory(f.isEndpointMandatory());
                newF.setEndpointMany(f.isEndpointMany());
                newF.setLabel(f.getLabel());
                newF.setDescription(f.getDescription());
                newF.setDataKeyName(f.getDataKeyName());
                return newF;
            }).forEach(form::addFieldDefinition);
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void beforePersist(final ActionDefinition item, final FormData data)
    {
        // NO-OP
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPersist(final ActionDefinition item, final FormData data, final ActionFormResult persistedObject)
    {
        // NO-OP
    }
}
