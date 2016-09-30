/*
 * Copyright 2016 Acosix GmbH
 *
 * Licensed under the Eclipse Public License (EPL), Version 1.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package de.acosix.alfresco.utility.common.spring;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Base class for post processors that need to emit bean definitions for beans based on simple configuration in properties files (i.e.
 * alfresco-global.properties) to provide more dynamic configuration options than via pre-defined XML bean definitions.
 *
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class BeanDefinitionFromPropertiesEmitter implements BeanDefinitionRegistryPostProcessor, InitializingBean
{

    private static final String PREFIX_MAP = "map.";

    private static final String PREFIX_LIST = "list.";

    private static final String FRAGMENT_PROPERTY = ".property.";

    private static final String SUFFIX_CLASS_NAME = "._className";

    private static final String SUFFIX_PARENT = "._parent";

    private static final Logger LOGGER = LoggerFactory.getLogger(BeanDefinitionFromPropertiesEmitter.class);

    protected String propertyPrefix;

    protected List<String> beanTypes;

    protected Properties propertiesSource;

    /**
     * @param propertyPrefix
     *            the propertyPrefix to set
     */
    public void setPropertyPrefix(final String propertyPrefix)
    {
        this.propertyPrefix = propertyPrefix;
    }

    /**
     * @param beanTypes
     *            the beanTypes to set
     */
    public void setBeanTypes(final List<String> beanTypes)
    {
        this.beanTypes = beanTypes;
    }

    /**
     * Sets the properties collection to use when emitting bean definitions.
     *
     * @param propertiesSource
     *            the propertiesSource to set
     */
    public void setPropertiesSource(final Properties propertiesSource)
    {
        this.propertiesSource = propertiesSource;
    }

    @Override
    public void afterPropertiesSet()
    {
        if (this.propertyPrefix == null || this.propertyPrefix.trim().isEmpty())
        {
            throw new IllegalStateException("propertyPrefix has not been set");
        }

        if (this.beanTypes == null || this.beanTypes.isEmpty())
        {
            throw new IllegalStateException("baseClassNamesByBeanType has not been set");
        }

        if (this.propertiesSource == null || this.propertiesSource.isEmpty())
        {
            throw new IllegalStateException("propertiesSource has not been set");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) throws BeansException
    {
        // NO-OP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postProcessBeanDefinitionRegistry(final BeanDefinitionRegistry registry) throws BeansException
    {
        LOGGER.info("Processing beans defined via properties files using prefix {}", this.propertyPrefix);

        final Map<String, BeanDefinition> beanDefinitions = new HashMap<>();
        processBeanClassAndParentDefinitions(beanName -> {
            BeanDefinition definition;
            if (beanDefinitions.containsKey(beanName))
            {
                definition = beanDefinitions.get(beanName);
            }
            else if (registry.containsBeanDefinition(beanName))
            {
                LOGGER.debug("Customizing pre-defined bean {}", beanName);
                definition = registry.getBeanDefinition(beanName);
                beanDefinitions.put(beanName, definition);
            }
            else
            {
                LOGGER.debug("Defining new bean {}", beanName);
                definition = new RootBeanDefinition();
                beanDefinitions.put(beanName, definition);
                registry.registerBeanDefinition(beanName, definition);
            }
            return definition;
        });
    }

    protected void processBeanClassAndParentDefinitions(final Function<String, BeanDefinition> getOrCreateBeanDefinition)
    {
        final String effectivePropertyPrefix = this.propertyPrefix + ".";
        final int propertyPrefixLength = effectivePropertyPrefix.length();

        this.propertiesSource.forEach((key, value) -> {
            LOGGER.debug("Evaluating property key {}", key);
            if (key instanceof String)
            {
                final String keyStr = (String) key;
                if (keyStr.startsWith(effectivePropertyPrefix))
                {
                    final String beanDefinitionKey = keyStr.substring(propertyPrefixLength);
                    final int firstDot = beanDefinitionKey.indexOf('.');
                    if (firstDot != -1)
                    {
                        final String beanType = beanDefinitionKey.substring(0, firstDot);
                        if (this.beanTypes.contains(beanType))
                        {
                            LOGGER.trace("Processing entry {} = {}", key, value);
                            if (beanDefinitionKey.endsWith(SUFFIX_CLASS_NAME))
                            {
                                final String beanName = beanDefinitionKey.substring(0,
                                        beanDefinitionKey.length() - SUFFIX_CLASS_NAME.length());
                                LOGGER.trace("Setting class name of bean {} to {}", beanName, value);
                                getOrCreateBeanDefinition.apply(beanName).setBeanClassName(String.valueOf(value));
                            }
                            else if (keyStr.endsWith(SUFFIX_PARENT))
                            {
                                final String beanName = beanDefinitionKey.substring(0, beanDefinitionKey.length() - SUFFIX_PARENT.length());
                                LOGGER.trace("Setting parent of bean {} to {}", beanName, value);
                                getOrCreateBeanDefinition.apply(beanName).setParentName(String.valueOf(value));
                            }
                            else
                            {
                                final int propertyFragmentIdx = beanDefinitionKey.indexOf(FRAGMENT_PROPERTY);
                                if (propertyFragmentIdx != -1)
                                {
                                    final String beanName = beanDefinitionKey.substring(0, propertyFragmentIdx);
                                    final String propertyDefinitionKey = beanDefinitionKey
                                            .substring(propertyFragmentIdx + FRAGMENT_PROPERTY.length());
                                    processPropertyDefinition(beanName, propertyDefinitionKey, value,
                                            getOrCreateBeanDefinition.apply(beanName));
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    protected void processPropertyDefinition(final String beanName, final String propertyDefinitionKey, final Object value,
            final BeanDefinition beanDefinition)
    {
        final String propertyName;

        String definitionKeyRemainder;
        final int nextDot = propertyDefinitionKey.indexOf('.');
        if (nextDot != -1)
        {
            propertyName = propertyDefinitionKey.substring(0, nextDot);
            definitionKeyRemainder = propertyDefinitionKey.substring(nextDot + 1).toLowerCase(Locale.ENGLISH);
        }
        else
        {
            propertyName = propertyDefinitionKey.toString();
            definitionKeyRemainder = "";
        }

        final MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();

        final boolean isList = definitionKeyRemainder.startsWith(PREFIX_LIST);
        final boolean isMap = definitionKeyRemainder.startsWith(PREFIX_MAP);
        if (isList)
        {
            definitionKeyRemainder = definitionKeyRemainder.substring(PREFIX_LIST.length());
            processListPropertyDefinition(beanName, propertyName, definitionKeyRemainder, value, propertyValues);
        }
        else if (isMap)
        {
            definitionKeyRemainder = definitionKeyRemainder.substring(PREFIX_MAP.length());
            processMapPropertryDefinition(beanName, propertyName, definitionKeyRemainder, value, propertyValues);
        }
        else
        {
            PropertyValue propertyValue = propertyValues.getPropertyValue(propertyName);
            final Object valueToSet = getAsValue(beanName, propertyName, definitionKeyRemainder, value);

            if (propertyValue != null && propertyValue.getValue() != null)
            {
                LOGGER.info("Property {} on {} already defined with value {} - overriding with different value", beanName, propertyName,
                        propertyValue.getValue());
            }
            propertyValue = new PropertyValue(propertyName, valueToSet);
            propertyValues.addPropertyValue(propertyValue);
        }
    }

    protected void processListPropertyDefinition(final String beanName, final String propertyName, final String definitionKey,
            final Object value, final MutablePropertyValues propertyValues)
    {
        int index;

        String definitionKeyRemainder;
        final int nextDot = definitionKey.indexOf('.');
        if (nextDot != -1)
        {
            index = Integer.parseInt(definitionKey.substring(0, nextDot));
            definitionKeyRemainder = definitionKey.substring(nextDot + 1);
        }
        else
        {
            index = Integer.parseInt(definitionKey);
            definitionKeyRemainder = "";
        }

        final ManagedList<Object> valueList = initListPropertyValue(beanName, propertyName, propertyValues);

        while (valueList.size() < index)
        {
            // pad with null values
            // may be replaced with actual value if another property defines value for index
            valueList.add(null);
        }

        final Object valueToSet = getAsValue(beanName, propertyName, definitionKeyRemainder, value);
        if (valueList.size() == index)
        {
            valueList.add(valueToSet);
        }
        else
        {
            valueList.set(index, valueToSet);
        }
    }

    protected void processMapPropertryDefinition(final String beanName, final String propertyName, final String definitionKey,
            final Object value, final MutablePropertyValues propertyValues)
    {
        String key;

        String definitionKeyRemainder;
        final int nextDot = definitionKey.indexOf('.');
        if (nextDot != -1)
        {
            key = definitionKey.substring(0, nextDot);
            definitionKeyRemainder = definitionKey.substring(nextDot + 1);
        }
        else
        {
            key = definitionKey;
            definitionKeyRemainder = "";
        }

        final ManagedMap<Object, Object> valueMap = initMapPropertyValue(beanName, propertyName, propertyValues);
        final Object valueToSet = getAsValue(beanName, propertyName, definitionKeyRemainder, value);
        valueMap.put(key, valueToSet);
    }

    @SuppressWarnings("unchecked")
    protected ManagedList<Object> initListPropertyValue(final String beanName, final String propertyName,
            final MutablePropertyValues propertyValues)
    {
        ManagedList<Object> valueList;
        PropertyValue propertyValue = propertyValues.getPropertyValue(propertyName);
        if (propertyValue == null)
        {
            LOGGER.trace("Property {} on {} not defined yet - initializing new managed list", beanName, propertyName);
            valueList = new ManagedList<>();
            propertyValue = new PropertyValue(propertyName, valueList);
            propertyValues.addPropertyValue(propertyValue);
        }
        else if (propertyValue.getValue() instanceof ManagedList<?>)
        {
            LOGGER.trace("Property {} on {} already has a list value - amending", beanName, propertyName);
            valueList = (ManagedList<Object>) propertyValue.getValue();
        }
        else
        {
            LOGGER.info("Property {} on {} already defined with value {} - overriding with list value based on properties", beanName,
                    propertyName, propertyValue.getValue());
            valueList = new ManagedList<>();
            propertyValue = new PropertyValue(propertyName, valueList);
            propertyValues.addPropertyValue(propertyValue);
        }
        return valueList;
    }

    @SuppressWarnings("unchecked")
    protected ManagedMap<Object, Object> initMapPropertyValue(final String beanName, final String propertyName,
            final MutablePropertyValues propertyValues)
    {
        ManagedMap<Object, Object> valueMap;
        PropertyValue propertyValue = propertyValues.getPropertyValue(propertyName);
        if (propertyValue == null)
        {
            LOGGER.trace("Property {} on {} not defined yet - initializing new managed map", beanName, propertyName);
            valueMap = new ManagedMap<>();
            propertyValue = new PropertyValue(propertyName, valueMap);
            propertyValues.addPropertyValue(propertyValue);
        }
        else if (propertyValue.getValue() instanceof ManagedMap<?, ?>)
        {
            LOGGER.trace("Property {} on {} already has a map value - amending", beanName, propertyName);
            valueMap = (ManagedMap<Object, Object>) propertyValue.getValue();
        }
        else
        {
            LOGGER.info("Property {} on {} already defined with value {} - overriding with map value based on properties", beanName,
                    propertyName, propertyValue.getValue());
            valueMap = new ManagedMap<>();
            propertyValue = new PropertyValue(propertyName, valueMap);
            propertyValues.addPropertyValue(propertyValue);
        }
        return valueMap;
    }

    protected Object getAsValue(final String beanName, final String propertyName, final String definitionKey, final Object value)
    {
        final Object result;
        if ("ref".equals(definitionKey))
        {
            LOGGER.trace("Treating value of property {} on {} as reference to bean {}", beanName, propertyName, value);
            result = new RuntimeBeanReference(String.valueOf(value));
        }
        else if (definitionKey.isEmpty())
        {
            LOGGER.trace("Treating value of property {} on {} as literal value {}", beanName, propertyName, value);
            result = value;
        }
        else
        {
            final StringBuilder msgBuilder = new StringBuilder();
            msgBuilder.append("Cannot handle remaining value key ");
            msgBuilder.append(definitionKey);
            msgBuilder.append(" for property ");
            msgBuilder.append(propertyName);
            msgBuilder.append(" on bean ");
            msgBuilder.append(beanName);
            throw new UnsupportedOperationException(msgBuilder.toString());
        }

        return result;
    }
}