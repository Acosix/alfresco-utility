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
import java.util.List;
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

    protected boolean active;

    protected String propertyName;

    protected String beanReferenceName;

    protected List<String> beanReferenceNames;

    // can only handle simple values, no maps (except via bean references)

    protected Object value;

    protected List<Object> values;

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
     * @param active
     *            the active to set
     */
    public void setActive(final boolean active)
    {
        this.active = active;
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
     * @param beanReferenceName
     *            the beanReferenceName to set
     */
    public void setBeanReferenceName(final String beanReferenceName)
    {
        this.beanReferenceName = beanReferenceName;
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
     * @param value
     *            the value to set
     */
    public void setValue(final Object value)
    {
        this.value = value;
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
        if (this.active && this.targetBeanName != null && this.propertyName != null)
        {
            applyChange(beanName -> {
                return beanFactory.getBeanDefinition(beanName);
            });
        }
        else if (!this.active)
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
                LOGGER.debug("[{}] List of values / bean reference names has been configured - treating property {} of {} as <list>",
                        this.beanName, this.propertyName, this.targetBeanName);

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
                            this.targetBeanName, this.values);
                    valuesToAdd = new ArrayList<>();
                    for (final String beanReferenceName : this.beanReferenceNames)
                    {
                        valuesToAdd.add(new RuntimeBeanReference(beanReferenceName));
                    }
                }

                if (this.addAsFirst)
                {
                    LOGGER.debug("[{}] Adding new entries at start of list for {} of {}", this.beanName, this.propertyName,
                            this.targetBeanName);
                    list.addAll(0, valuesToAdd);
                }
                else if (this.addAtIndex >= 0 && this.addAtIndex < list.size())
                {
                    LOGGER.debug("[{}] Adding new entries at position {} of list for {} of {}", this.beanName,
                            String.valueOf(this.addAtIndex), this.propertyName, this.targetBeanName);
                    list.addAll(this.addAtIndex, valuesToAdd);
                }
                else
                {
                    LOGGER.debug("[{}] Adding new entries at end of list for {} of {}", this.beanName, this.propertyName,
                            this.targetBeanName);
                    list.addAll(valuesToAdd);
                }

                if (!list.isMergeEnabled() && this.mergeParent)
                {
                    LOGGER.debug("[{}] Enabling \"merge\" for <list> on {} of {}", this.beanName, this.propertyName, this.targetBeanName);
                }
                list.setMergeEnabled(list.isMergeEnabled() || this.mergeParent);
                value = list;
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
}
