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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.util.PropertyPlaceholderHelper;

/**
 * Base class for post processors that need to emit, enhance or remove bean definitions based on simple configuration in properties files
 * (i.e. alfresco-global.properties) to provide more dynamic configuration options than via pre-defined XML bean definitions.
 *
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class BeanDefinitionFromPropertiesPostProcessor implements BeanDefinitionRegistryPostProcessor, InitializingBean, BeanNameAware
{

    private static final String SUFFIX_PROPERTY_NULL = "null";

    private static final String SUFFIX_PROPERTY_REF = "ref";

    private static final String SUFFIX_CSV_PROPERTY = "csv";

    private static final String DOT = ".";

    private static final String SUFFIX_PROPERTY_REMOVE = "_remove";

    private static final String PREFIX_MAP = "map.";

    private static final String PREFIX_LIST = "list.";

    private static final String PREFIX_SET = "set.";

    private static final String FRAGMENT_PROPERTY = ".property.";

    private static final String SUFFIX_BEAN_REMOVE = "._remove";

    private static final String SUFFIX_CLASS_NAME = "._className";

    private static final String SUFFIX_PARENT = "._parent";

    private static final String SUFFIX_SCOPE = "._scope";

    private static final String SUFFIX_ABSTRACT = "._abstract";

    private static final String SUFFIX_DEPENDS_ON = "._dependsOn";

    private static final Logger LOGGER = LoggerFactory.getLogger(BeanDefinitionFromPropertiesPostProcessor.class);

    protected String beanName;

    protected boolean enabled;

    protected String enabledPropertyKey;

    protected String propertyPrefix;

    protected List<String> beanTypes;

    protected Properties propertiesSource;

    protected String placeholderPrefix = PlaceholderConfigurerSupport.DEFAULT_PLACEHOLDER_PREFIX;

    protected String placeholderSuffix = PlaceholderConfigurerSupport.DEFAULT_PLACEHOLDER_SUFFIX;

    protected String valueSeparator = PlaceholderConfigurerSupport.DEFAULT_VALUE_SEPARATOR;

    protected PropertyPlaceholderHelper placeholderHelper;

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

    /**
     * @param placeholderPrefix
     *            the placeholderPrefix to set
     */
    public void setPlaceholderPrefix(final String placeholderPrefix)
    {
        this.placeholderPrefix = placeholderPrefix;
    }

    /**
     * @param placeholderSuffix
     *            the placeholderSuffix to set
     */
    public void setPlaceholderSuffix(final String placeholderSuffix)
    {
        this.placeholderSuffix = placeholderSuffix;
    }

    /**
     * @param valueSeparator
     *            the valueSeparator to set
     */
    public void setValueSeparator(final String valueSeparator)
    {
        this.valueSeparator = valueSeparator;
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
            throw new IllegalStateException("beanTypes has not been set");
        }

        if (this.propertiesSource == null || this.propertiesSource.isEmpty())
        {
            throw new IllegalStateException("propertiesSource has not been set");
        }

        this.placeholderHelper = new PropertyPlaceholderHelper(this.placeholderPrefix, this.placeholderSuffix, this.valueSeparator, true);
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
        boolean enabled = this.enabled;
        if (this.enabledPropertyKey != null && !this.enabledPropertyKey.isEmpty())
        {
            final String property = this.propertiesSource.getProperty(this.enabledPropertyKey);
            enabled = enabled || (property != null ? Boolean.parseBoolean(property) : false);
        }

        if (enabled)
        {
            LOGGER.info("[{}] Processing beans defined via properties files using prefix {}", this.beanName, this.propertyPrefix);

            final Map<String, BeanDefinition> beanDefinitions = new HashMap<>();
            final Function<String, BeanDefinition> getOrCreateBeanDefinition = beanName -> {
                BeanDefinition definition;
                if (beanDefinitions.containsKey(beanName))
                {
                    definition = beanDefinitions.get(beanName);
                }
                else if (registry.containsBeanDefinition(beanName))
                {
                    LOGGER.debug("[{}] Customizing pre-defined bean {}", this.beanName, beanName);
                    definition = registry.getBeanDefinition(beanName);
                    beanDefinitions.put(beanName, definition);
                }
                else
                {
                    LOGGER.debug("[{}] Defining new bean {}", this.beanName, beanName);
                    definition = new GenericBeanDefinition();
                    beanDefinitions.put(beanName, definition);
                    registry.registerBeanDefinition(beanName, definition);
                }
                return definition;
            };
            final Function<String, BeanDefinition> removeBeanDefinition = beanName -> {
                BeanDefinition definition;
                if (beanDefinitions.containsKey(beanName))
                {
                    definition = beanDefinitions.remove(beanName);
                    registry.removeBeanDefinition(beanName);
                }
                else if (registry.containsBeanDefinition(beanName))
                {
                    definition = registry.getBeanDefinition(beanName);
                    registry.removeBeanDefinition(beanName);
                }
                else
                {
                    definition = null;
                }

                return definition;
            };

            final Collection<ManagedList<?>> paddedLists = new ArrayList<>();
            final Consumer<ManagedList<?>> paddedListRegistrator = list -> {
                if (!paddedLists.contains(list))
                {
                    paddedLists.add(list);
                }
            };

            this.processBeanDefinitions(getOrCreateBeanDefinition, removeBeanDefinition, paddedListRegistrator);

            this.compressPaddedLists(paddedLists);
        }
    }

    protected void compressPaddedLists(final Collection<ManagedList<?>> paddedLists)
    {
        paddedLists.forEach(list -> {
            final Iterator<?> iterator = list.iterator();
            while (iterator.hasNext())
            {
                if (iterator.next() == null)
                {
                    iterator.remove();
                }
            }
        });
    }

    protected void processBeanDefinitions(final Function<String, BeanDefinition> getOrCreateBeanDefinition,
            final Function<String, BeanDefinition> removeBeanDefinition, final Consumer<ManagedList<?>> paddedListRegistrator)
    {
        final String effectivePropertyPrefix = this.propertyPrefix + DOT;
        final int propertyPrefixLength = effectivePropertyPrefix.length();

        this.propertiesSource.forEach((key, value) -> {
            LOGGER.debug("[{}] Evaluating property key {}", this.beanName, key);
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
                            String resolvedValue = this.placeholderHelper.replacePlaceholders(String.valueOf(value), this.propertiesSource);
                            if (resolvedValue != null)
                            {
                                resolvedValue = resolvedValue.trim();
                            }

                            LOGGER.trace("[{}] Processing entry {} = {}", this.beanName, key, resolvedValue);

                            final int propertyFragmentIdx = beanDefinitionKey.indexOf(FRAGMENT_PROPERTY);

                            if (propertyFragmentIdx == -1)
                            {
                                if (beanDefinitionKey.endsWith(SUFFIX_BEAN_REMOVE))
                                {
                                    final String beanName = beanDefinitionKey.substring(0,
                                            beanDefinitionKey.length() - SUFFIX_BEAN_REMOVE.length());
                                    if (Boolean.parseBoolean(resolvedValue))
                                    {
                                        LOGGER.debug("[{}] Removing bean {}", this.beanName, beanName);
                                        removeBeanDefinition.apply(beanName);
                                    }
                                    else
                                    {
                                        LOGGER.debug("[{}] Not removing bean {} due to non-true property value", this.beanName, beanName);
                                    }
                                }
                                else if (beanDefinitionKey.endsWith(SUFFIX_CLASS_NAME))
                                {
                                    final String beanName = beanDefinitionKey.substring(0,
                                            beanDefinitionKey.length() - SUFFIX_CLASS_NAME.length());
                                    LOGGER.debug("[{}] Setting class name of bean {} to {}", this.beanName, beanName, resolvedValue);
                                    getOrCreateBeanDefinition.apply(beanName).setBeanClassName(resolvedValue);
                                }
                                else if (keyStr.endsWith(SUFFIX_PARENT))
                                {
                                    final String beanName = beanDefinitionKey.substring(0,
                                            beanDefinitionKey.length() - SUFFIX_PARENT.length());
                                    LOGGER.debug("[{}] Setting parent of bean {} to {}", this.beanName, beanName, resolvedValue);
                                    getOrCreateBeanDefinition.apply(beanName).setParentName(resolvedValue);
                                }
                                else if (keyStr.endsWith(SUFFIX_SCOPE))
                                {
                                    final String beanName = beanDefinitionKey.substring(0,
                                            beanDefinitionKey.length() - SUFFIX_SCOPE.length());
                                    LOGGER.debug("[{}] Setting scope of bean {} to {}", this.beanName, beanName, resolvedValue);
                                    getOrCreateBeanDefinition.apply(beanName).setScope(resolvedValue);
                                }
                                else if (keyStr.endsWith(SUFFIX_DEPENDS_ON))
                                {
                                    final String beanName = beanDefinitionKey.substring(0,
                                            beanDefinitionKey.length() - SUFFIX_DEPENDS_ON.length());
                                    LOGGER.debug("[{}] Setting dependsOn of bean {} to {}", this.beanName, beanName, resolvedValue);
                                    getOrCreateBeanDefinition.apply(beanName).setDependsOn(resolvedValue.split(","));
                                }
                                else if (keyStr.endsWith(SUFFIX_ABSTRACT))
                                {
                                    final String beanName = beanDefinitionKey.substring(0,
                                            beanDefinitionKey.length() - SUFFIX_ABSTRACT.length());
                                    LOGGER.debug("[{}] Setting abstract of bean {} to {}", this.beanName, beanName, resolvedValue);
                                    final BeanDefinition beanDefinition = getOrCreateBeanDefinition.apply(beanName);
                                    if (beanDefinition instanceof AbstractBeanDefinition)
                                    {
                                        ((AbstractBeanDefinition) beanDefinition).setAbstract(Boolean.parseBoolean(resolvedValue));
                                    }
                                }
                            }
                            else
                            {
                                final String beanName = beanDefinitionKey.substring(0, propertyFragmentIdx);
                                final String propertyDefinitionKey = beanDefinitionKey
                                        .substring(propertyFragmentIdx + FRAGMENT_PROPERTY.length());
                                this.processPropertyDefinition(beanName, propertyDefinitionKey, resolvedValue,
                                        getOrCreateBeanDefinition.apply(beanName), paddedListRegistrator);
                            }
                        }
                    }
                }
            }
        });
    }

    protected void processPropertyDefinition(final String beanName, final String propertyDefinitionKey, final String value,
            final BeanDefinition beanDefinition, final Consumer<ManagedList<?>> paddedListRegistrator)
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

        final boolean isRemove = definitionKeyRemainder.equals(SUFFIX_PROPERTY_REMOVE);
        final boolean isList = definitionKeyRemainder.startsWith(PREFIX_LIST);
        final boolean isSet = definitionKeyRemainder.startsWith(PREFIX_LIST);
        final boolean isMap = definitionKeyRemainder.startsWith(PREFIX_MAP);

        if (isList)
        {
            definitionKeyRemainder = definitionKeyRemainder.substring(PREFIX_LIST.length());
            this.processListPropertyDefinition(beanName, propertyName, definitionKeyRemainder, value, propertyValues,
                    paddedListRegistrator);
        }
        else if (isSet)
        {
            definitionKeyRemainder = definitionKeyRemainder.substring(PREFIX_SET.length());
            this.processSetPropertyDefinition(beanName, propertyName, definitionKeyRemainder, value, propertyValues);
        }
        else if (isMap)
        {
            definitionKeyRemainder = definitionKeyRemainder.substring(PREFIX_MAP.length());
            this.processMapPropertryDefinition(beanName, propertyName, definitionKeyRemainder, value, propertyValues);
        }
        else if (isRemove)
        {
            if (value instanceof String && Boolean.parseBoolean(value))
            {
                LOGGER.debug("[{}] Removing property {} from {}", this.beanName, propertyName, beanName);
                propertyValues.removePropertyValue(propertyName);
            }
            else
            {
                LOGGER.debug("[{}] Not removing property {} from [} due to non-true property value", this.beanName, propertyName, beanName);
            }
        }
        else
        {
            PropertyValue propertyValue = propertyValues.getPropertyValue(propertyName);
            final Object valueToSet = this.getAsValue(beanName, propertyName, definitionKeyRemainder, value);

            if (propertyValue != null && propertyValue.getValue() != null)
            {
                LOGGER.info("[{}] Property {} on {} already defined with value {} - overriding with different value", this.beanName,
                        beanName, propertyName, propertyValue.getValue());
            }
            propertyValue = new PropertyValue(propertyName, valueToSet);
            propertyValues.addPropertyValue(propertyValue);
        }
    }

    protected void processListPropertyDefinition(final String beanName, final String propertyName, final String definitionKey,
            final String value, final MutablePropertyValues propertyValues, final Consumer<ManagedList<?>> paddedListRegistrator)
    {
        boolean isCsv = false;

        String definitionKeyRemainder = definitionKey;

        int nextDot = definitionKeyRemainder.indexOf('.');
        if (definitionKeyRemainder.startsWith(SUFFIX_CSV_PROPERTY) && (nextDot == -1 || nextDot == SUFFIX_CSV_PROPERTY.length()))
        {
            isCsv = true;
            definitionKeyRemainder = nextDot != -1 ? definitionKeyRemainder.substring(nextDot + 1) : "";
        }

        nextDot = definitionKeyRemainder.indexOf('.');
        final String potentialIndex;
        if (nextDot != -1)
        {
            potentialIndex = definitionKeyRemainder.substring(0, nextDot);
            definitionKeyRemainder = definitionKeyRemainder.substring(nextDot + 1);
        }
        else
        {
            potentialIndex = definitionKeyRemainder;
            definitionKeyRemainder = "";
        }

        // potentialIndex may just be used as a differentiator for multiple list additions / removals for the same property
        final int index;
        if (potentialIndex.matches("^\\d+$"))
        {
            index = Integer.parseInt(potentialIndex);
        }
        else
        {
            if (definitionKeyRemainder.isEmpty())
            {
                definitionKeyRemainder = potentialIndex;
            }
            else
            {
                definitionKeyRemainder = potentialIndex + DOT + definitionKeyRemainder;
            }
            index = -1;
        }

        final ManagedList<Object> valueList = this.initListPropertyValue(beanName, propertyName, propertyValues);

        if (definitionKeyRemainder.endsWith(DOT + SUFFIX_PROPERTY_REMOVE))
        {
            if (index != -1)
            {
                valueList.remove(index);
            }
            else
            {
                if (isCsv)
                {
                    if (!value.isEmpty())
                    {
                        final String[] strValues = value.split("\\s*(?<!\\\\),\\s*");
                        for (final String singleValue : strValues)
                        {
                            final Object valueToRemove = this.getAsValue(beanName, propertyName, definitionKeyRemainder, singleValue);
                            valueList.remove(valueToRemove);
                        }
                    }
                }
                else
                {
                    final Object valueToRemove = this.getAsValue(beanName, propertyName, definitionKeyRemainder, value);
                    valueList.remove(valueToRemove);
                }
            }
        }
        else
        {
            if (valueList.size() < index)
            {
                paddedListRegistrator.accept(valueList);

                while (valueList.size() < index)
                {
                    // pad with null values
                    // may be replaced with actual value if another property defines value for index
                    valueList.add(null);
                }
            }

            if (isCsv)
            {
                if (!value.isEmpty())
                {
                    final String[] strValues = value.split("\\s*(?<!\\\\),\\s*");

                    for (final String singleValue : strValues)
                    {
                        final Object valueToSet = this.getAsValue(beanName, propertyName, definitionKeyRemainder, singleValue);
                        if (index == -1 || valueList.size() == index)
                        {
                            valueList.add(valueToSet);
                        }
                        else
                        {
                            valueList.set(index, valueToSet);
                        }
                    }
                }
            }
            else
            {
                final Object valueToSet = this.getAsValue(beanName, propertyName, definitionKeyRemainder, value);
                if (index == -1 || valueList.size() == index)
                {
                    valueList.add(valueToSet);
                }
                else
                {
                    valueList.set(index, valueToSet);
                }
            }
        }
    }

    protected void processSetPropertyDefinition(final String beanName, final String propertyName, final String definitionKey,
            final String value, final MutablePropertyValues propertyValues)
    {
        boolean isCsv = false;

        String definitionKeyRemainder = definitionKey;

        int nextDot = definitionKeyRemainder.indexOf('.');
        if (definitionKeyRemainder.startsWith(SUFFIX_CSV_PROPERTY) && (nextDot == -1 || nextDot == SUFFIX_CSV_PROPERTY.length()))
        {
            isCsv = true;
            definitionKeyRemainder = nextDot != -1 ? definitionKeyRemainder.substring(nextDot + 1) : "";
        }

        // we support having a dummy differentiator between multiple set additions / removals for the same property
        nextDot = definitionKeyRemainder.indexOf('.');
        if (nextDot != -1)
        {
            definitionKeyRemainder = definitionKeyRemainder.substring(nextDot + 1);
        }
        else
        {
            definitionKeyRemainder = "";
        }

        final ManagedSet<Object> valueSet = this.initSetPropertyValue(beanName, propertyName, propertyValues);

        if (definitionKeyRemainder.endsWith(DOT + SUFFIX_PROPERTY_REMOVE))
        {
            if (isCsv)
            {
                if (!value.isEmpty())
                {
                    final String[] strValues = value.split("\\s*(?<!\\\\),\\s*");
                    for (final String singleValue : strValues)
                    {
                        final Object valueToRemove = this.getAsValue(beanName, propertyName, definitionKeyRemainder, singleValue);
                        valueSet.remove(valueToRemove);
                    }
                }
            }
            else
            {
                final Object valueToRemove = this.getAsValue(beanName, propertyName, definitionKeyRemainder, value);
                valueSet.remove(valueToRemove);
            }
        }
        else
        {
            if (isCsv)
            {
                if (!value.isEmpty())
                {
                    final String[] strValues = value.split("\\s*(?<!\\\\),\\s*");

                    for (final String singleValue : strValues)
                    {
                        final Object valueToAdd = this.getAsValue(beanName, propertyName, definitionKeyRemainder, singleValue);
                        valueSet.add(valueToAdd);
                    }
                }
            }
            else
            {
                final Object valueToAdd = this.getAsValue(beanName, propertyName, definitionKeyRemainder, value);
                valueSet.add(valueToAdd);
            }
        }
    }

    protected void processMapPropertryDefinition(final String beanName, final String propertyName, final String definitionKey,
            final String value, final MutablePropertyValues propertyValues)
    {
        String key;

        String definitionKeyRemainder;
        int keySeparator;

        if (definitionKey.endsWith(DOT + SUFFIX_PROPERTY_NULL))
        {
            keySeparator = definitionKey.lastIndexOf(DOT + SUFFIX_PROPERTY_NULL);
        }
        else if (definitionKey.endsWith(DOT + SUFFIX_PROPERTY_REF))
        {
            keySeparator = definitionKey.lastIndexOf(DOT + SUFFIX_PROPERTY_REF);
        }
        else
        {
            keySeparator = -1;
        }

        if (keySeparator != -1)
        {
            key = definitionKey.substring(0, keySeparator);
            definitionKeyRemainder = definitionKey.substring(keySeparator + 1);
        }
        else
        {
            key = definitionKey;
            definitionKeyRemainder = "";
        }

        final ManagedMap<Object, Object> valueMap = this.initMapPropertyValue(beanName, propertyName, propertyValues);

        if (definitionKeyRemainder.endsWith(DOT + SUFFIX_PROPERTY_REMOVE))
        {
            valueMap.remove(key);
        }
        else
        {
            final Object valueToSet = this.getAsValue(beanName, propertyName, definitionKeyRemainder, value);
            valueMap.put(key, valueToSet);
        }
    }

    @SuppressWarnings("unchecked")
    protected ManagedList<Object> initListPropertyValue(final String beanName, final String propertyName,
            final MutablePropertyValues propertyValues)
    {
        ManagedList<Object> valueList;
        PropertyValue propertyValue = propertyValues.getPropertyValue(propertyName);
        if (propertyValue == null)
        {
            LOGGER.trace("[{}] Property {} on {} not defined yet - initializing new managed list", this.beanName, beanName, propertyName);
            valueList = new ManagedList<>();
            propertyValue = new PropertyValue(propertyName, valueList);
            propertyValues.addPropertyValue(propertyValue);
        }
        else if (propertyValue.getValue() instanceof ManagedList<?>)
        {
            LOGGER.trace("[{}] Property {} on {} already has a list value - amending", this.beanName, beanName, propertyName);
            valueList = (ManagedList<Object>) propertyValue.getValue();
        }
        else
        {
            LOGGER.info("[{}] Property {} on {} already defined with value {} - overriding with list value based on properties",
                    this.beanName, beanName, propertyName, propertyValue.getValue());
            valueList = new ManagedList<>();
            propertyValue = new PropertyValue(propertyName, valueList);
            propertyValues.addPropertyValue(propertyValue);
        }
        return valueList;
    }

    @SuppressWarnings("unchecked")
    protected ManagedSet<Object> initSetPropertyValue(final String beanName, final String propertyName,
            final MutablePropertyValues propertyValues)
    {
        ManagedSet<Object> valueSet;
        PropertyValue propertyValue = propertyValues.getPropertyValue(propertyName);
        if (propertyValue == null)
        {
            LOGGER.trace("[{}] Property {} on {} not defined yet - initializing new managed set", this.beanName, beanName, propertyName);
            valueSet = new ManagedSet<>();
            propertyValue = new PropertyValue(propertyName, valueSet);
            propertyValues.addPropertyValue(propertyValue);
        }
        else if (propertyValue.getValue() instanceof ManagedList<?>)
        {
            LOGGER.trace("[{}] Property {} on {} already has a set value - amending", this.beanName, beanName, propertyName);
            valueSet = (ManagedSet<Object>) propertyValue.getValue();
        }
        else
        {
            LOGGER.info("[{}] Property {} on {} already defined with value {} - overriding with set value based on properties",
                    this.beanName, beanName, propertyName, propertyValue.getValue());
            valueSet = new ManagedSet<>();
            propertyValue = new PropertyValue(propertyName, valueSet);
            propertyValues.addPropertyValue(propertyValue);
        }
        return valueSet;
    }

    @SuppressWarnings("unchecked")
    protected ManagedMap<Object, Object> initMapPropertyValue(final String beanName, final String propertyName,
            final MutablePropertyValues propertyValues)
    {
        ManagedMap<Object, Object> valueMap;
        PropertyValue propertyValue = propertyValues.getPropertyValue(propertyName);
        if (propertyValue == null)
        {
            LOGGER.trace("[{}] Property {} on {} not defined yet - initializing new managed map", this.beanName, beanName, propertyName);
            valueMap = new ManagedMap<>();
            propertyValue = new PropertyValue(propertyName, valueMap);
            propertyValues.addPropertyValue(propertyValue);
        }
        else if (propertyValue.getValue() instanceof ManagedMap<?, ?>)
        {
            LOGGER.trace("[{}] Property {} on {} already has a map value - amending", this.beanName, beanName, propertyName);
            valueMap = (ManagedMap<Object, Object>) propertyValue.getValue();
        }
        else
        {
            LOGGER.info("[{}] Property {} on {} already defined with value {} - overriding with map value based on properties",
                    this.beanName, beanName, propertyName, propertyValue.getValue());
            valueMap = new ManagedMap<>();
            propertyValue = new PropertyValue(propertyName, valueMap);
            propertyValues.addPropertyValue(propertyValue);
        }
        return valueMap;
    }

    protected Object getAsValue(final String beanName, final String propertyName, final String definitionKey, final String value)
    {
        final Object result;
        if (SUFFIX_PROPERTY_REF.equals(definitionKey))
        {
            LOGGER.trace("[{}] Treating value of property {} on {} as reference to bean {}", this.beanName, beanName, propertyName, value);
            result = new RuntimeBeanReference(value);
        }
        else if (SUFFIX_PROPERTY_NULL.equals(definitionKey) && Boolean.parseBoolean(value))
        {
            LOGGER.trace("[{}] Treating value of property {} on {} as null", this.beanName, beanName, propertyName);
            result = null;
        }
        else if (definitionKey.isEmpty())
        {
            LOGGER.trace("[{}] Treating value of property {} on {} as literal value {}", this.beanName, beanName, propertyName, value);
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
