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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.ManagedMap;

/**
 * Instances of this post processor class can be used to merge additional map
 * entries into existing bean definitions without having to override the entire
 * bean definition via Spring XML. While Spring supports merging of map values
 * for parent-child bean constructs it does not support merging of map values in
 * arbitrary beans by some component detached from the affected bean. Overriding
 * the original Alfresco bean definition via Spring XML is not recommended as it
 * can affect the upgradeability of an Alfresco system.
 *
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class MapPropertyValueEnhancingPostProcessor implements BeanDefinitionRegistryPostProcessor, BeanNameAware
{

    private static final Logger LOGGER = LoggerFactory.getLogger(MapPropertyValueEnhancingPostProcessor.class);

    protected String beanName;

    protected boolean enabled;

    protected String enabledPropertyKey;

    protected Properties propertiesSource;

    protected String targetBeanName;

    protected String mapPropertyName;

    protected Map<String, String> simpleValuesToMerge;

    protected Map<String, String> beanReferencesToMerge;

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBeanName(final String name)
    {
        this.beanName = name;
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
     * @param enabledPropertyKey
     *            the enabledPropertyKey to set
     */
    public void setEnabledPropertyKey(final String enabledPropertyKey)
    {
        this.enabledPropertyKey = enabledPropertyKey;
    }

    /**
     * @param propertiesSource
     *            the propertiesSource to set
     */
    public void setPropertiesSource(final Properties propertiesSource)
    {
        this.propertiesSource = propertiesSource;
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
     * @param mapPropertyName
     *            the mapPropertyName to set
     */
    public void setMapPropertyName(final String mapPropertyName)
    {
        this.mapPropertyName = mapPropertyName;
    }

    /**
     * @param simpleValuesToMerge
     *            the simpleValuesToMerge to set
     */
    public void setSimpleValuesToMerge(final Map<String, String> simpleValuesToMerge)
    {
        this.simpleValuesToMerge = simpleValuesToMerge;
    }

    /**
     * @param beanReferencesToMerge
     *            the beanReferencesToMerge to set
     */
    public void setBeanReferencesToMerge(final Map<String, String> beanReferencesToMerge)
    {
        this.beanReferencesToMerge = beanReferencesToMerge;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) throws BeansException
    {
        // no-op but required by interface class hierarchy
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postProcessBeanDefinitionRegistry(final BeanDefinitionRegistry registry) throws BeansException
    {
        boolean enabled = this.enabled;
        if (this.enabledPropertyKey != null && !this.enabledPropertyKey.isEmpty() && this.propertiesSource != null)
        {
            final String property = this.propertiesSource.getProperty(this.enabledPropertyKey);
            enabled = enabled || (property != null ? Boolean.parseBoolean(property) : false);
        }

        if (enabled && this.targetBeanName != null)
        {
            this.applyChange(beanName -> {
                return registry.getBeanDefinition(beanName);
            });
        }
        else if (!enabled)
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
        final MutablePropertyValues values = beanDefinition.getPropertyValues();

        PropertyValue propertyValue = values.getPropertyValue(this.mapPropertyName);
        Map<Object, Object> map;
        if (propertyValue == null)
        {
            LOGGER.debug("[{}] Value of {} is not defined - initializing new map", this.beanName, this.mapPropertyName);
            map = new ManagedMap<>();
            values.add(this.mapPropertyName, map);
            propertyValue = values.getPropertyValue(this.mapPropertyName);
        }
        else
        {
            final Object value = propertyValue.getValue();
            if (value instanceof Map<?, ?>)
            {
                LOGGER.warn("[{}] Value of {} is defined as a map - continueing with merge", this.beanName, this.mapPropertyName);
                @SuppressWarnings("unchecked")
                final Map<Object, Object> map1 = (Map<Object, Object>) value;
                map = map1;
            }
            else if (value == null)
            {
                LOGGER.warn("[{}] Value of {} is defined as null - initializing new map", this.beanName, this.mapPropertyName);
                map = new ManagedMap<>();
                values.add(this.mapPropertyName, map);
                propertyValue = values.getPropertyValue(this.mapPropertyName);
            }
            else
            {
                LOGGER.warn("[{}] Value of {} is already defined and not a map", this.beanName, this.mapPropertyName);
                throw new IllegalStateException(
                        "Cannot adapt " + this.mapPropertyName + " of " + this.targetBeanName + " - existing value is not a map");
            }
        }

        if (this.simpleValuesToMerge != null)
        {
            LOGGER.warn("[{}] Merging simple values {} into map", this.beanName, this.simpleValuesToMerge);
            map.putAll(this.simpleValuesToMerge);
        }

        if (this.beanReferencesToMerge != null)
        {
            LOGGER.warn("[{}] Merging bean references {} into map", this.beanName, this.beanReferencesToMerge);
            for (final Entry<String, String> beanReferenceEntry : this.beanReferencesToMerge.entrySet())
            {
                map.put(beanReferenceEntry.getKey(), new RuntimeBeanReference(beanReferenceEntry.getValue()));
            }
        }
    }
}
