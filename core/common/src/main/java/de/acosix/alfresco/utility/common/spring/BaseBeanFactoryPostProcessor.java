/*
 * Copyright 2016 - 2021 Acosix GmbH
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

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.util.PropertyPlaceholderHelper;

import de.acosix.alfresco.utility.common.spring.condition.BeanDefinitionPostProcessorCondition;

/**
 * @author Axel Faust
 */
public abstract class BaseBeanFactoryPostProcessor<D extends BeanFactoryPostProcessor>
        implements BeanFactoryPostProcessor, InitializingBean, BeanNameAware
{

    /**
     *
     * @author Axel Faust
     */
    @FunctionalInterface
    protected static interface PostProcessorOperation
    {

        /**
         * Applies the change of the enclosing post processor on the affected bean and any additional beans, whose definition can be
         * retrieved using the provided function callback.
         *
         * @param affectedBeanDefinition
         *            the definition of the bean primarily affected by the enclosing post processor
         * @param beanDefinitionRetriever
         *            the callback to retrieve other (referenced) bean definitions
         * @throws BeansException
         *             if any modification of the Spring beans context fails
         */
        void applyChange(BeanDefinition affectedBeanDefinition, Function<String, BeanDefinition> beanDefinitionRetriever)
                throws BeansException;
    }

    protected String beanName;

    protected List<D> dependsOn;

    protected boolean executed;

    protected String targetBeanName;

    protected String targetBeanNamePattern;

    protected Boolean enabled;

    protected String enabledPropertyKey;

    protected List<String> enabledPropertyKeys;

    protected Properties propertiesSource;

    protected String placeholderPrefix = PlaceholderConfigurerSupport.DEFAULT_PLACEHOLDER_PREFIX;

    protected String placeholderSuffix = PlaceholderConfigurerSupport.DEFAULT_PLACEHOLDER_SUFFIX;

    protected String valueSeparator = PlaceholderConfigurerSupport.DEFAULT_VALUE_SEPARATOR;

    protected PropertyPlaceholderHelper placeholderHelper;

    protected boolean failIfTargetBeanMissing = true;

    protected BeanDefinitionPostProcessorCondition condition;

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
     * @param targetBeanNamePattern
     *            the targetBeanNamePattern to set
     */
    public void setTargetBeanNamePattern(final String targetBeanNamePattern)
    {
        this.targetBeanNamePattern = targetBeanNamePattern;
    }

    /**
     * @param enabled
     *            the enabled to set
     */
    public void setEnabled(final Boolean enabled)
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
     * @param enabledPropertyKeys
     *            the enabledPropertyKeys to set
     */
    public void setEnabledPropertyKeys(final List<String> enabledPropertyKeys)
    {
        this.enabledPropertyKeys = enabledPropertyKeys;
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

    /**
     * @param failIfTargetBeanMissing
     *            the failIfTargetBeanMissing to set
     */
    public void setFailIfTargetBeanMissing(final boolean failIfTargetBeanMissing)
    {
        this.failIfTargetBeanMissing = failIfTargetBeanMissing;
    }

    /**
     * @return the condition
     */
    public BeanDefinitionPostProcessorCondition getCondition()
    {
        return this.condition;
    }

    /**
     * @param condition
     *            the condition to set
     */
    public void setCondition(final BeanDefinitionPostProcessorCondition condition)
    {
        this.condition = condition;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        this.placeholderHelper = new PropertyPlaceholderHelper(this.placeholderPrefix, this.placeholderSuffix, this.valueSeparator, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) throws BeansException
    {
        this.execute(beanFactory, this.dependsOn, this::applyChange);
    }

    abstract protected void applyChange(BeanDefinition affectedBeanDefinition, Function<String, BeanDefinition> getBeanDefinition);

    protected <D1 extends BeanFactoryPostProcessor> void execute(final ConfigurableListableBeanFactory beanFactory,
            final List<D1> dependsOn, final PostProcessorOperation operation) throws BeansException
    {
        final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
        if (!this.executed)
        {
            if (this.isEnabled())
            {
                if (dependsOn != null)
                {
                    dependsOn.forEach(x -> x.postProcessBeanFactory(beanFactory));
                }

                if (this.condition == null || this.condition.applies(beanFactory))
                {
                    final Set<String> touchedBeanDefinitionNames = new HashSet<>();

                    if (this.targetBeanName != null)
                    {
                        final boolean containsDefinition = beanFactory.containsBeanDefinition(this.targetBeanName);
                        if (!containsDefinition && this.failIfTargetBeanMissing)
                        {
                            throw new IllegalStateException("Target bean '" + this.targetBeanName + "' has not been defined");
                        }

                        if (containsDefinition)
                        {
                            operation.applyChange(beanFactory.getBeanDefinition(this.targetBeanName), beanName -> {
                                touchedBeanDefinitionNames.add(beanName);
                                return beanFactory.getBeanDefinition(beanName);
                            });
                            touchedBeanDefinitionNames.add(this.targetBeanName);
                        }
                        else
                        {
                            LOGGER.info("[{}] patch cannnot be applied as the affected bean {} has not been defined", this.beanName,
                                    this.targetBeanName);
                        }
                    }
                    else if (this.targetBeanNamePattern != null)
                    {
                        final Pattern beanNamePattern = Pattern.compile(this.targetBeanNamePattern);
                        final String[] beanDefinitionNames = beanFactory.getBeanDefinitionNames();
                        for (final String beanDefinitionName : beanDefinitionNames)
                        {
                            if (beanNamePattern.matcher(beanDefinitionName).matches())
                            {
                                operation.applyChange(beanFactory.getBeanDefinition(beanDefinitionName), beanName -> {
                                    touchedBeanDefinitionNames.add(beanName);
                                    return beanFactory.getBeanDefinition(beanName);
                                });

                                touchedBeanDefinitionNames.add(this.beanName);
                            }
                        }
                    }
                    else
                    {
                        LOGGER.info("[{}] patch cannnot be applied as it does not define the name or name pattern of affected bean(s)",
                                this.beanName);
                    }

                    if (beanFactory instanceof BeanDefinitionRegistry)
                    {
                        // any beans we modified may have already had a "merged bean definition" cached in the factory
                        // need to re-register / "fake" override to clear that cache to ensure changes are properly reflected
                        // (no public reset / clear cache API; removing + register may mess up bean order)
                        touchedBeanDefinitionNames.stream().forEach(beanName -> {
                            ((BeanDefinitionRegistry) beanFactory).registerBeanDefinition(beanName,
                                    beanFactory.getBeanDefinition(beanName));
                        });
                    }
                }
                else
                {
                    LOGGER.info("[{}] patch will not be applied as its prerequisite condition does not apply", this.beanName);
                }

                this.executed = true;
            }
            else
            {
                LOGGER.info("[{}] patch will not be applied as it has been marked as inactive", this.beanName);
            }
        }
    }

    protected <D1 extends BeanDefinitionRegistryPostProcessor> void execute(final BeanDefinitionRegistry registry, final List<D1> dependsOn,
            final PostProcessorOperation operation) throws BeansException
    {
        final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
        if (!this.executed)
        {
            if (this.isEnabled())
            {
                if (dependsOn != null)
                {
                    dependsOn.forEach(x -> x.postProcessBeanDefinitionRegistry(registry));
                }

                if (this.condition == null || this.condition.applies(registry))
                {
                    if (this.targetBeanName != null)
                    {
                        final boolean containsDefinition = registry.containsBeanDefinition(this.targetBeanName);
                        if (!containsDefinition && this.failIfTargetBeanMissing)
                        {
                            throw new IllegalStateException("Target bean '" + this.targetBeanName + "' has not been defined");
                        }

                        if (containsDefinition)
                        {
                            operation.applyChange(registry.getBeanDefinition(this.targetBeanName),
                                    beanName -> registry.getBeanDefinition(beanName));
                        }
                        else
                        {
                            LOGGER.info("[{}] patch cannnot be applied as the affected bean {} has not been defined", this.beanName,
                                    this.targetBeanName);
                        }
                    }
                    else if (this.targetBeanNamePattern != null)
                    {
                        final Pattern beanNamePattern = Pattern.compile(this.targetBeanNamePattern);
                        final String[] beanDefinitionNames = registry.getBeanDefinitionNames();
                        for (final String beanDefinitionName : beanDefinitionNames)
                        {
                            if (beanNamePattern.matcher(beanDefinitionName).matches())
                            {
                                operation.applyChange(registry.getBeanDefinition(beanDefinitionName),
                                        beanName -> registry.getBeanDefinition(beanName));
                            }
                        }
                    }
                    else
                    {
                        LOGGER.info("[{}] patch cannnot be applied as it does not define the name or name pattern of affected bean(s)",
                                this.beanName);
                    }
                }
                else
                {
                    LOGGER.info("[{}] patch will not be applied as its prerequisite condition does not apply", this.beanName);
                }

                this.executed = true;
            }
            else
            {
                LOGGER.info("[{}] patch will not be applied as it has been marked as inactive", this.beanName);
            }
        }
    }

    protected boolean isEnabled()
    {
        Boolean enabled = this.enabled;
        if (this.propertiesSource != null)
        {
            if (!Boolean.FALSE.equals(enabled) && this.enabledPropertyKey != null && !this.enabledPropertyKey.isEmpty())
            {
                String property = this.propertiesSource.getProperty(this.enabledPropertyKey);
                property = property != null ? this.placeholderHelper.replacePlaceholders(property, this.propertiesSource) : null;
                enabled = (property != null ? Boolean.valueOf(property) : Boolean.FALSE);
            }

            if (!Boolean.FALSE.equals(enabled) && this.enabledPropertyKeys != null && !this.enabledPropertyKeys.isEmpty())
            {
                final AtomicBoolean enabled2 = new AtomicBoolean(true);
                this.enabledPropertyKeys.forEach(key -> {
                    String property = this.propertiesSource.getProperty(key);
                    property = property != null ? this.placeholderHelper.replacePlaceholders(property, this.propertiesSource) : null;
                    enabled2.compareAndSet(true, property != null ? Boolean.parseBoolean(property) : false);
                });
                enabled = Boolean.valueOf(enabled2.get());
            }
        }

        return Boolean.TRUE.equals(enabled);
    }
}
