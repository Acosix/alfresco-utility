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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.EqualsHelper;
import org.alfresco.util.PropertyCheck;
import org.springframework.beans.factory.InitializingBean;

/**
 * Instances of this class perform a simple, straightforward adaption of the values of a property, using a mapping table for values in
 * textual form (for any non-text value types it is assumed that {@link DefaultTypeConverter appropriate converters} exist).
 *
 * @author Axel Faust
 */
public class SimpleNodePropertyValueMappingPatchRule implements NodePropertiesPatchRule, InitializingBean
{

    protected NamespaceService namespaceService;

    protected DictionaryService dictionaryService;

    protected String propertyName;

    protected Map<String, String> valueMappings;

    protected QName propertyQName;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "namespaceService", this.namespaceService);
        PropertyCheck.mandatory(this, "dictionaryService", this.dictionaryService);

        PropertyCheck.mandatory(this, "propertyName", this.propertyName);
        PropertyCheck.mandatory(this, "valueMappings", this.valueMappings);

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
     * @param propertyName
     *            the propertyName to set
     */
    public void setPropertyName(final String propertyName)
    {
        this.propertyName = propertyName;
    }

    /**
     * @param valueMappings
     *            the valueMappings to set
     */
    public void setValueMappings(final Map<String, String> valueMappings)
    {
        this.valueMappings = valueMappings;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Map<QName, Serializable> apply(final NodeRef node, final Map<QName, Serializable> currentProperties)
    {
        Map<QName, Serializable> updates = Collections.emptyMap();

        if (currentProperties.containsKey(this.propertyQName))
        {
            updates = new HashMap<>(2);

            final Serializable value = currentProperties.get(this.propertyQName);

            if (value instanceof Collection<?>)
            {
                final Collection<String> values = DefaultTypeConverter.INSTANCE.convert(String.class, (Collection<?>) value);
                final List<String> newValues = values.stream().map(s -> this.valueMappings.getOrDefault(s, s))
                        .filter(s -> s != null && !s.isEmpty()).collect(Collectors.toList());

                if (!EqualsHelper.nullSafeEquals(values, newValues))
                {
                    updates.put(this.propertyQName, new ArrayList<>(newValues));
                }
            }
            else
            {
                final String sValue = DefaultTypeConverter.INSTANCE.convert(String.class, value);
                final String newValue = this.valueMappings.getOrDefault(sValue, sValue);

                if (!EqualsHelper.nullSafeEquals(sValue, newValue))
                {
                    updates.put(this.propertyQName, newValue != null && !newValue.isEmpty() ? newValue : null);
                }
            }
        }

        return updates;
    }

}
