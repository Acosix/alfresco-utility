/*
 * Copyright 2016, 2017 Acosix GmbH
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import org.alfresco.util.EqualsHelper;
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
import org.springframework.beans.factory.support.ManagedSet;

/**
 * {@link BeanFactoryPostProcessor Bean factory post processor} to alter a property of a bean definition with adapted configuration before
 * instantiation without requiring an override that may conflict with custom Spring configuration.
 *
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class PropertyAlteringBeanFactoryPostProcessor<D extends BeanFactoryPostProcessor> implements BeanFactoryPostProcessor, BeanNameAware
{

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyAlteringBeanFactoryPostProcessor.class);

    protected String beanName;

    protected List<D> dependsOn;

    protected boolean executed;

    protected String targetBeanName;

    protected String expectedClassName;

    protected Boolean enabled;

    protected String propertyName;

    protected Object value;

    protected String beanReferenceName;

    protected List<Object> valueList;

    protected List<String> beanReferenceNameList;

    protected Set<Object> valueSet;

    protected Set<String> beanReferenceNameSet;

    protected Map<Object, Object> valueMap;

    protected Map<Object, String> beanReferenceNameMap;

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
     * @param dependsOn
     *            the dependsOn to set
     */
    public void setDependsOn(final List<D> dependsOn)
    {
        this.dependsOn = dependsOn;
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
     * @param expectedClassName
     *            the expectedClassName to set
     */
    public void setExpectedClassName(final String expectedClassName)
    {
        this.expectedClassName = expectedClassName;
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
     *
     * @param values
     *            the values to set
     *
     * @deprecated Included only for backwards compatibility - use {@link #setValueList(List) setValueList} instead
     */
    @Deprecated
    public void setValues(final List<Object> values)
    {
        this.setValueList(values);
    }

    /**
     * @param valueList
     *            the valueList to set
     */
    public void setValueList(final List<Object> valueList)
    {
        this.valueList = valueList;
    }

    /**
     *
     * @param beanReferenceNames
     *            the beanReferenceNames to set
     *
     * @deprecated Included only for backwards compatibility - use {@link #setBeanReferenceNameList(List) setBeanReferenceNameList} instead
     */
    @Deprecated
    public void setBeanReferenceNames(final List<String> beanReferenceNames)
    {
        this.setBeanReferenceNameList(beanReferenceNames);
    }

    /**
     * @param beanReferenceNameList
     *            the beanReferenceNameList to set
     */
    public void setBeanReferenceNameList(final List<String> beanReferenceNameList)
    {
        this.beanReferenceNameList = beanReferenceNameList;
    }

    /**
     * @param valueSet
     *            the valueSet to set
     */
    public void setValueSet(final Set<Object> valueSet)
    {
        this.valueSet = valueSet;
    }

    /**
     * @param beanReferenceNameSet
     *            the beanReferenceNameSet to set
     */
    public void setBeanReferenceNameSet(final Set<String> beanReferenceNameSet)
    {
        this.beanReferenceNameSet = beanReferenceNameSet;
    }

    /**
     * @param mappedValues
     *            the mappedValues to set
     * @deprecated Included only for backwards compatibility - use {@link #setValueMap(Map) setValueMap} instead
     */
    @Deprecated
    public void setMappedValues(final Map<Object, Object> mappedValues)
    {
        this.setValueMap(mappedValues);
    }

    /**
     * @param valueMap
     *            the valueMap to set
     */
    public void setValueMap(final Map<Object, Object> valueMap)
    {
        this.valueMap = valueMap;
    }

    /**
     * @param mappedBeanReferenceNames
     *            the mappedBeanReferenceNames to set
     * @deprecated Included only for backwards compatibility - use {@link #setBeanReferenceNameMap(Map) setBeanReferenceNameMap} instead
     */
    @Deprecated
    public void setMappedBeanReferenceNames(final Map<Object, String> mappedBeanReferenceNames)
    {
        this.setBeanReferenceNameMap(mappedBeanReferenceNames);
    }

    /**
     * @param beanReferenceNameMap
     *            the beanReferenceNameMap to set
     */
    public void setBeanReferenceNameMap(final Map<Object, String> beanReferenceNameMap)
    {
        this.beanReferenceNameMap = beanReferenceNameMap;
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
        if (!this.executed)
        {
            if (this.enabled)
            {
                if (this.dependsOn != null)
                {
                    this.dependsOn.forEach(x -> {
                        x.postProcessBeanFactory(beanFactory);
                    });
                }

                if (this.targetBeanName != null && this.propertyName != null)
                {
                    this.applyChange(beanName -> {
                        return beanFactory.getBeanDefinition(beanName);
                    });
                }
                else
                {
                    LOGGER.warn("[{}] patch cannnot be applied as its configuration is incomplete", this.beanName);
                }

                this.executed = true;
            }
            else
            {
                LOGGER.info("[{}] patch will not be applied as it has been marked as inactive", this.beanName);
            }
        }
    }

    protected void applyChange(final Function<String, BeanDefinition> getBeanDefinition)
    {
        final BeanDefinition beanDefinition = getBeanDefinition.apply(this.targetBeanName);
        if (beanDefinition != null)
        {
            if (this.expectedClassName == null || EqualsHelper.nullSafeEquals(this.expectedClassName, beanDefinition.getBeanClassName()))
            {
                LOGGER.debug("[{}] Patching property {} of Spring bean {}", this.beanName, this.propertyName, this.targetBeanName);

                final MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();
                final PropertyValue configuredValue = propertyValues.getPropertyValue(this.propertyName);

                final Object value;

                if (this.valueList != null || this.beanReferenceNameList != null)
                {
                    value = this.handleListValues(configuredValue);
                }
                else if (this.valueSet != null || this.beanReferenceNameSet != null)
                {
                    value = this.handleSetValues(configuredValue);
                }
                else if (this.valueMap != null || this.beanReferenceNameMap != null)
                {
                    value = this.handleMapValues(configuredValue);
                }
                else if (this.value != null)
                {
                    LOGGER.debug("[{}] Setting new value {} for {} of {}", this.beanName, this.value, this.propertyName,
                            this.targetBeanName);
                    value = this.value;
                }
                else if (this.beanReferenceName != null)
                {
                    LOGGER.debug("[{}] Setting new bean reference to {} for {} of {}", this.beanName, this.beanReferenceName,
                            this.propertyName, this.targetBeanName);
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
                LOGGER.info("[{}] patch cannot be applied - bean with name {} does not match expected class {}", this.beanName,
                        this.targetBeanName, this.expectedClassName);
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
        if (this.valueList != null)
        {
            LOGGER.debug("[{}] List of configured values for {} of {}: ", this.beanName, this.propertyName, this.targetBeanName,
                    this.valueList);
            valuesToAdd = this.valueList;
        }
        else
        {
            LOGGER.debug("[{}] List of configured bean reference names for {} of {}: ", this.beanName, this.propertyName,
                    this.targetBeanName, this.beanReferenceNameList);
            valuesToAdd = new ArrayList<>();
            for (final String beanReferenceName : this.beanReferenceNameList)
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

    protected Object handleSetValues(final PropertyValue configuredValue)
    {
        final Object value;
        LOGGER.debug("[{}] Set of values / bean reference names has been configured - treating property {} of {} as <set>", this.beanName,
                this.propertyName, this.targetBeanName);

        final ManagedSet<Object> set = new ManagedSet<>();

        if (this.merge && configuredValue != null)
        {
            final Object configuredValueDefinition = configuredValue.getValue();
            if (configuredValueDefinition instanceof ManagedSet<?>)
            {
                final ManagedSet<?> oldSet = (ManagedSet<?>) configuredValueDefinition;
                set.setElementTypeName(oldSet.getElementTypeName());
                set.setMergeEnabled(oldSet.isMergeEnabled());
                set.setSource(oldSet.getSource());

                set.addAll(oldSet);

                LOGGER.debug("[{}] Merged existing value set values: {}", this.beanName, oldSet);
            }
        }

        Set<Object> valuesToAdd;
        if (this.valueSet != null)
        {
            LOGGER.debug("[{}] Set of configured values for {} of {}: ", this.beanName, this.propertyName, this.targetBeanName,
                    this.valueSet);
            valuesToAdd = this.valueSet;
        }
        else
        {
            LOGGER.debug("[{}] Set of configured bean reference names for {} of {}: ", this.beanName, this.propertyName,
                    this.targetBeanName, this.beanReferenceNameSet);
            valuesToAdd = new HashSet<>();
            for (final String beanReferenceName : this.beanReferenceNameSet)
            {
                valuesToAdd.add(new RuntimeBeanReference(beanReferenceName));
            }
        }

        LOGGER.debug("[{}] Adding new entries to set for {} of {}", this.beanName, this.propertyName, this.targetBeanName);
        set.addAll(valuesToAdd);

        if (!set.isMergeEnabled() && this.mergeParent)
        {
            LOGGER.debug("[{}] Enabling \"merge\" for <set> on {} of {}", this.beanName, this.propertyName, this.targetBeanName);
        }
        set.setMergeEnabled(set.isMergeEnabled() || this.mergeParent);
        value = set;
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
        if (this.valueMap != null)
        {
            LOGGER.debug("[{}] Map of configured values for {} of {}: ", this.beanName, this.propertyName, this.targetBeanName,
                    this.valueMap);
            valuesToMap = this.valueMap;
        }
        else
        {
            LOGGER.debug("[{}] Map of configured bean reference names for {} of {}: ", this.beanName, this.propertyName,
                    this.targetBeanName, this.beanReferenceNameMap);
            valuesToMap = new HashMap<>();
            for (final Entry<Object, String> beanReferenceEntry : this.beanReferenceNameMap.entrySet())
            {
                valuesToMap.put(beanReferenceEntry.getKey(), new RuntimeBeanReference(beanReferenceEntry.getValue()));
            }
        }

        LOGGER.debug("[{}] Putting new entries into map for {} of {}", this.beanName, this.propertyName, this.targetBeanName);
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
