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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.PropertyCheck;
import org.springframework.beans.factory.InitializingBean;

/**
 * Instances of this class perform a simple, straightforward mapping of a node property from {@link QName} A to B.
 *
 * @author Axel Faust
 */
public class SimpleNodePropertyMappingPatchRule implements NodePropertiesPatchRule, InitializingBean
{

    protected NamespaceService namespaceService;

    protected DictionaryService dictionaryService;

    protected String fromPropertyName;

    protected String toPropertyName;

    protected boolean removeFromProperty;

    protected boolean defaultToDictionaryDefaultValue;

    protected QName fromPropertyQName;

    protected QName toPropertyQName;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "namespaceService", this.namespaceService);
        PropertyCheck.mandatory(this, "dictionaryService", this.dictionaryService);

        PropertyCheck.mandatory(this, "fromPropertyName", this.fromPropertyName);
        PropertyCheck.mandatory(this, "toPropertyName", this.toPropertyName);

        this.fromPropertyQName = QName.resolveToQName(this.namespaceService, this.fromPropertyName);
        if (this.fromPropertyQName == null)
        {
            throw new IllegalStateException("Property name " + this.fromPropertyName + " cannot be resolved to a QName");
        }

        this.toPropertyQName = QName.resolveToQName(this.namespaceService, this.toPropertyName);
        if (this.toPropertyQName == null)
        {
            throw new IllegalStateException("Property name " + this.toPropertyName + " cannot be resolved to a QName");
        }

        PropertyDefinition propertyDef = this.dictionaryService.getProperty(this.fromPropertyQName);
        if (propertyDef == null)
        {
            throw new IllegalStateException("The property " + this.fromPropertyName + " is not defined in the data model");
        }

        propertyDef = this.dictionaryService.getProperty(this.toPropertyQName);
        if (propertyDef == null)
        {
            throw new IllegalStateException("The property " + this.toPropertyName + " is not defined in the data model");
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
     * @param fromPropertyName
     *            the fromPropertyName to set
     */
    public void setFromPropertyName(final String fromPropertyName)
    {
        this.fromPropertyName = fromPropertyName;
    }

    /**
     * @param toPropertyName
     *            the toPropertyName to set
     */
    public void setToPropertyName(final String toPropertyName)
    {
        this.toPropertyName = toPropertyName;
    }

    /**
     * @param removeFromProperty
     *            the removeFromProperty to set
     */
    public void setRemoveFromProperty(final boolean removeFromProperty)
    {
        this.removeFromProperty = removeFromProperty;
    }

    /**
     * @param defaultToDictionaryDefaultValue
     *            the defaultToDictionaryDefaultValue to set
     */
    public void setDefaultToDictionaryDefaultValue(final boolean defaultToDictionaryDefaultValue)
    {
        this.defaultToDictionaryDefaultValue = defaultToDictionaryDefaultValue;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Map<QName, Serializable> apply(final NodeRef node, final Map<QName, Serializable> currentProperties)
    {
        Map<QName, Serializable> updates = Collections.emptyMap();

        if (currentProperties.containsKey(this.fromPropertyQName))
        {
            updates = new HashMap<>(2);
            final Serializable propertyValue = currentProperties.get(this.fromPropertyQName);
            if (propertyValue != null)
            {
                updates.put(this.toPropertyQName, propertyValue);
            }
            else if (this.defaultToDictionaryDefaultValue)
            {
                final String defaultValue = this.dictionaryService.getProperty(this.toPropertyQName).getDefaultValue();
                updates.put(this.toPropertyQName, defaultValue);
            }

            if (this.removeFromProperty)
            {
                updates.put(this.fromPropertyQName, null);
            }
        }
        else if (this.defaultToDictionaryDefaultValue)
        {
            final String defaultValue = this.dictionaryService.getProperty(this.toPropertyQName).getDefaultValue();
            updates = Collections.singletonMap(this.toPropertyQName, defaultValue);
        }

        return updates;
    }

}
