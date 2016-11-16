/*
 * Copyright 2016 Acosix GmbH
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
package de.acosix.alfresco.utility.common.spring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;

/**
 * {@link BeanFactoryPostProcessor Bean factory post processor} to alter a property of a bean definition with adapted configuration before
 * instantiation without requiring an override that may conflict with custom Spring configuration.
 *
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class PropertyAlteringBeanFactoryPostProcessor implements BeanFactoryPostProcessor, BeanNameAware
{

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyAlteringBeanFactoryPostProcessor.class);

    protected String beanName;

    protected String targetBeanName;

    protected boolean enabled;

    protected String propertyName;

    protected Object value;

    protected String beanReferenceName;

    protected List<Object> values;

    protected List<String> beanReferenceNames;

    protected Map<Object, Object> mappedValues;

    protected Map<Object, String> mappedBeanReferenceNames;

    protected boolean addAsFirst = false;

    protected int addAtIndex = -1;

    protected boolean merge;

    protected boolean mergeParent;

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBeanName(final String name)
    {
        this.beanName = name;
    }

    /**
     * @param targetBeanName
     *            the targetBeanName to set
     */
    public void setTargetBeanName(final String targetBeanName)
    {
        this.targetBeanName = targetBeanName;
    }

    /**
     * @param enabled
     *            the enabled to set
     */
    public void setEnabled(final boolean enabled)
    {
        this.enabled = enabled;
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
     * @param value
     *            the value to set
     */
    public void setValue(final Object value)
    {
        this.value = value;
    }

    /**
     * @param beanReferenceName
     *            the beanReferenceName to set
     */
    public void setBeanReferenceName(final String beanReferenceName)
    {
        this.beanReferenceName = beanReferenceName;
    }

    /**
     * @param values
     *            the values to set
     */
    public void setValues(final List<Object> values)
    {
        this.values = values;
    }

    /**
     * @param beanReferenceNames
     *            the beanReferenceNames to set
     */
    public void setBeanReferenceNames(final List<String> beanReferenceNames)
    {
        this.beanReferenceNames = beanReferenceNames;
    }

    /**
     * @param mappedValues
     *            the mappedValues to set
     */
    public void setMappedValues(final Map<Object, Object> mappedValues)
    {
        this.mappedValues = mappedValues;
    }

    /**
     * @param mappedBeanReferenceNames
     *            the mappedBeanReferenceNames to set
     */
    public void setMappedBeanReferenceNames(final Map<Object, String> mappedBeanReferenceNames)
    {
        this.mappedBeanReferenceNames = mappedBeanReferenceNames;
    }

    /**
     * @param addAsFirst
     *            the addAsFirst to set
     */
    public void setAddAsFirst(final boolean addAsFirst)
    {
        this.addAsFirst = addAsFirst;
    }

    /**
     * @param addAtIndex
     *            the addAtIndex to set
     */
    public void setAddAtIndex(final int addAtIndex)
    {
        this.addAtIndex = addAtIndex;
    }

    /**
     * @param merge
     *            the merge to set
     */
    public void setMerge(final boolean merge)
    {
        this.merge = merge;
    }

    /**
     * @param mergeParent
     *            the mergeParent to set
     */
    public void setMergeParent(final boolean mergeParent)
    {
        this.mergeParent = mergeParent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) throws BeansException
    {
        if (this.enabled && this.targetBeanName != null && this.propertyName != null)
        {
            this.applyChange(beanName -> {
                return beanFactory.getBeanDefinition(beanName);
            });
        }
        else if (!this.enabled)
        {
            LOGGER.info("[{}] patch will not be applied as it has been marked as inactive", this.beanName);
        }
        else
        {
            LOGGER.warn("[{}] patch cannnot be applied as its configuration is incomplete", this.beanName);
        }
    }

    protected void applyChange(final Function<String, BeanDefinition> getBeanDefinition)
    {
        final BeanDefinition beanDefinition = getBeanDefinition.apply(this.targetBeanName);
        if (beanDefinition != null)
        {
            LOGGER.info("[{}] Patching property {} of Spring bean {}", this.beanName, this.propertyName, this.targetBeanName);

            final MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();
            final PropertyValue configuredValue = propertyValues.getPropertyValue(this.propertyName);

            final Object value;

            if (this.values != null || this.beanReferenceNames != null)
            {
                value = this.handleListValues(configuredValue);
            }
            else if (this.mappedValues != null || this.mappedBeanReferenceNames != null)
            {
                value = this.handleMapValues(configuredValue);
            }
            else if (this.value != null)
            {
                LOGGER.debug("[{}] Setting new value {} for {} of {}", this.beanName, this.value, this.propertyName, this.targetBeanName);
                value = this.value;
            }
            else if (this.beanReferenceName != null)
            {
                LOGGER.debug("[{}] Setting new bean reference to {} for {} of {}", this.beanName, this.beanReferenceName, this.propertyName,
                        this.targetBeanName);
                value = new RuntimeBeanReference(this.beanReferenceName);
            }
            else
            {
                value = null;
            }

            if (value != null)
            {
                final PropertyValue newValue = new PropertyValue(this.propertyName, value);
                propertyValues.addPropertyValue(newValue);
            }
            else if (configuredValue != null)
            {
                LOGGER.debug("[{}] Removing {} property definition from Spring bean {}", this.propertyName, this.targetBeanName);
                propertyValues.removePropertyValue(configuredValue);
            }
        }
        else
        {
            LOGGER.info("[{}] patch cannot be applied - no bean with name {} has been defined", this.beanName, this.targetBeanName);
        }
    }

    protected Object handleListValues(final PropertyValue configuredValue)
    {
        final Object value;
        LOGGER.debug("[{}] List of values / bean reference names has been configured - treating property {} of {} as <list>", this.beanName,
                this.propertyName, this.targetBeanName);

        final ManagedList<Object> list = new ManagedList<>();

        if (this.merge && configuredValue != null)
        {
            final Object configuredValueDefinition = configuredValue.getValue();
            if (configuredValueDefinition instanceof ManagedList<?>)
            {
                final ManagedList<?> oldList = (ManagedList<?>) configuredValueDefinition;
                list.setElementTypeName(oldList.getElementTypeName());
                list.setMergeEnabled(oldList.isMergeEnabled());
                list.setSource(oldList.getSource());

                list.addAll(oldList);

                LOGGER.debug("[{}] Merged existing value list values: {}", this.beanName, oldList);
            }
        }

        List<Object> valuesToAdd;
        if (this.values != null)
        {
            LOGGER.debug("[{}] List of configured values for {} of {}: ", this.beanName, this.propertyName, this.targetBeanName,
                    this.values);
            valuesToAdd = this.values;
        }
        else
        {
            LOGGER.debug("[{}] List of configured bean reference names for {} of {}: ", this.beanName, this.propertyName,
                    this.targetBeanName, this.beanReferenceNames);
            valuesToAdd = new ArrayList<>();
            for (final String beanReferenceName : this.beanReferenceNames)
            {
                valuesToAdd.add(new RuntimeBeanReference(beanReferenceName));
            }
        }

        if (this.addAsFirst)
        {
            LOGGER.debug("[{}] Adding new entries at start of list for {} of {}", this.beanName, this.propertyName, this.targetBeanName);
            list.addAll(0, valuesToAdd);
        }
        else if (this.addAtIndex >= 0 && this.addAtIndex < list.size())
        {
            LOGGER.debug("[{}] Adding new entries at position {} of list for {} of {}", this.beanName, String.valueOf(this.addAtIndex),
                    this.propertyName, this.targetBeanName);
            list.addAll(this.addAtIndex, valuesToAdd);
        }
        else
        {
            LOGGER.debug("[{}] Adding new entries at end of list for {} of {}", this.beanName, this.propertyName, this.targetBeanName);
            list.addAll(valuesToAdd);
        }

        if (!list.isMergeEnabled() && this.mergeParent)
        {
            LOGGER.debug("[{}] Enabling \"merge\" for <list> on {} of {}", this.beanName, this.propertyName, this.targetBeanName);
        }
        list.setMergeEnabled(list.isMergeEnabled() || this.mergeParent);
        value = list;
        return value;
    }

    protected Object handleMapValues(final PropertyValue configuredValue)
    {
        final Object value;
        LOGGER.debug("[{}] Map of values / bean reference names has been configured - treating property {} of {} as <map>", this.beanName,
                this.propertyName, this.targetBeanName);

        final ManagedMap<Object, Object> map = new ManagedMap<>();

        if (this.merge && configuredValue != null)
        {
            final Object configuredValueDefinition = configuredValue.getValue();
            if (configuredValueDefinition instanceof ManagedMap<?, ?>)
            {
                final ManagedMap<?, ?> oldMap = (ManagedMap<?, ?>) configuredValueDefinition;

                map.setKeyTypeName(oldMap.getKeyTypeName());
                map.setValueTypeName(oldMap.getValueTypeName());
                map.setMergeEnabled(oldMap.isMergeEnabled());
                map.setSource(oldMap.getSource());

                map.putAll(oldMap);

                LOGGER.debug("[{}] Merged existing map values: {}", this.beanName, oldMap);
            }
        }

        Map<Object, Object> valuesToMap;
        if (this.mappedValues != null)
        {
            LOGGER.debug("[{}] Map of configured values for {} of {}: ", this.beanName, this.propertyName, this.targetBeanName,
                    this.mappedValues);
            valuesToMap = this.mappedValues;
        }
        else
        {
            LOGGER.debug("[{}] Map of configured bean reference names for {} of {}: ", this.beanName, this.propertyName,
                    this.targetBeanName, this.mappedBeanReferenceNames);
            valuesToMap = new HashMap<>();
            for (final Entry<Object, String> beanReferenceEntry : this.mappedBeanReferenceNames.entrySet())
            {
                valuesToMap.put(beanReferenceEntry.getKey(), new RuntimeBeanReference(beanReferenceEntry.getValue()));
            }
        }

        LOGGER.debug("[{}] Putting new entries into map list for {} of {}", this.beanName, this.propertyName, this.targetBeanName);
        map.putAll(valuesToMap);

        if (!map.isMergeEnabled() && this.mergeParent)
        {
            LOGGER.debug("[{}] Enabling \"merge\" for <map> on {} of {}", this.beanName, this.propertyName, this.targetBeanName);
        }
        map.setMergeEnabled(map.isMergeEnabled() || this.mergeParent);
        value = map;
        return value;
    }
}
