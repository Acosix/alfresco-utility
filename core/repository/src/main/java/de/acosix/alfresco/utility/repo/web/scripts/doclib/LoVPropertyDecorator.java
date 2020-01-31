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
package de.acosix.alfresco.utility.repo.web.scripts.doclib;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.repo.dictionary.constraint.ListOfValuesConstraint;
import org.alfresco.repo.dictionary.constraint.RegisteredConstraint;
import org.alfresco.repo.jscript.app.BasePropertyDecorator;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.ClassDefinition;
import org.alfresco.service.cmr.dictionary.Constraint;
import org.alfresco.service.cmr.dictionary.ConstraintDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.PropertyCheck;
import org.json.simple.JSONArray;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.InitializingBean;

/**
 * This implementation of a property decorator processes {@link ListOfValuesConstraint list-of-values constrained} properties and provides a
 * complex object composed of the raw value and display label.
 *
 * @author Axel Faust
 */
public class LoVPropertyDecorator extends BasePropertyDecorator implements InitializingBean
{

    protected DictionaryService dictionaryService;

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
        PropertyCheck.mandatory(this, "permissionService", this.permissionService);
        PropertyCheck.mandatory(this, "jsonConversionComponent", this.jsonConversionComponent);

        this.init();
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
     * {@inheritDoc}
     */
    @Override
    public JSONAware decorate(final QName propertyName, final NodeRef nodeRef, final Serializable value)
    {
        final QName type = this.nodeService.getType(nodeRef);
        final Set<QName> allAspects = this.nodeService.getAspects(nodeRef);
        final Set<QName> aspects = this.filterAspectParentClasses(allAspects);

        final TypeDefinition typeDefinition = this.dictionaryService.getAnonymousType(type, aspects);
        final PropertyDefinition propertyDefinition = typeDefinition.getProperties().get(propertyName);

        final JSONAware result;

        if (propertyDefinition != null)
        {
            if (value instanceof String)
            {
                @SuppressWarnings("unchecked")
                final Map<String, String> map = new JSONObject();
                map.put("value", (String) value);
                result = (JSONObject) map;

                this.retrieveDisplayLabel((String) value, propertyDefinition, map);
            }
            else if (value instanceof Collection<?>)
            {
                @SuppressWarnings("unchecked")
                final List<Object> list = new JSONArray();

                for (final Object element : (Collection<?>) value)
                {
                    if (element instanceof String)
                    {
                        @SuppressWarnings("unchecked")
                        final Map<String, String> map = new JSONObject();
                        map.put("value", (String) element);
                        this.retrieveDisplayLabel((String) element, propertyDefinition, map);
                        list.add(map);
                    }
                }

                result = (JSONArray) list;
            }
            else
            {
                result = null;
            }
        }
        else
        {
            result = null;
        }

        return result;
    }

    protected Set<QName> filterAspectParentClasses(final Set<QName> aspects)
    {
        final Set<QName> result = new HashSet<>(aspects);

        for (final QName qName : aspects)
        {
            final AspectDefinition aspect = this.dictionaryService.getAspect(qName);
            ClassDefinition parent = aspect.getParentClassDefinition();
            while (parent != null)
            {
                result.remove(parent.getName());
                parent = parent.getParentClassDefinition();
            }
        }

        return result;
    }

    protected void retrieveDisplayLabel(final String value, final PropertyDefinition propertyDefinition, final Map<String, String> map)
    {
        final List<ConstraintDefinition> constraints = propertyDefinition.getConstraints();
        if (constraints != null)
        {
            String label = null;

            for (final ConstraintDefinition constraint : constraints)
            {
                Constraint actualConstraint = constraint.getConstraint();
                if (actualConstraint instanceof RegisteredConstraint)
                {
                    actualConstraint = ((RegisteredConstraint) actualConstraint).getRegisteredConstraint();
                }

                if (actualConstraint instanceof ListOfValuesConstraint)
                {
                    label = ((ListOfValuesConstraint) actualConstraint).getDisplayLabel(value, this.dictionaryService);

                    if (label != null)
                    {
                        break;
                    }
                }
            }

            if (label != null)
            {
                map.put("displayName", label);
            }
        }
    }
}
