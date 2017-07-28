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

import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

/**
 * {@link BeanDefinitionRegistryPostProcessor Bean definition registry post processor} to alter the implementation class of a bean
 * definition without requiring an override that may conflict with custom Spring configuration. Instances of this class can be used to adapt
 * Spring lifecycle interfaces-implementing beans before they are discovered (except other bean definition
 * registry post processors). Due to the early Spring lifecycle phase that instances of this class are used in, property placeholders can
 * not be used to configure properties.
 *
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class ImplementationClassReplacingBeanDefinitionRegistryPostProcessor
        extends ImplementationClassReplacingBeanFactoryPostProcessor<BeanDefinitionRegistryPostProcessor>
        implements BeanDefinitionRegistryPostProcessor
{

    private static final Logger LOGGER = LoggerFactory.getLogger(ImplementationClassReplacingBeanDefinitionRegistryPostProcessor.class);

    protected Boolean enabled;

    protected String enabledPropertyKey;

    protected List<String> enabledPropertyKeys;

    protected Properties propertiesSource;

    /**
     * @param enabled
     *            the enabled to set
     */
    @Override
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
        if (!this.executed)
        {
            final boolean enabled = this.isEnabled();

            if (enabled)
            {
                if (this.dependsOn != null)
                {
                    this.dependsOn.forEach(x -> {
                        x.postProcessBeanDefinitionRegistry(registry);
                    });
                }

                if (this.targetBeanName != null && this.replacementClassName != null)
                {
                    this.applyChange(beanName -> {
                        return registry.getBeanDefinition(beanName);
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

    protected boolean isEnabled()
    {
        Boolean enabled = this.enabled;
        if (!Boolean.FALSE.equals(enabled) && this.enabledPropertyKey != null && !this.enabledPropertyKey.isEmpty())
        {
            final String property = this.propertiesSource.getProperty(this.enabledPropertyKey);
            enabled = (property != null ? Boolean.valueOf(property) : Boolean.FALSE);
        }

        if (!Boolean.FALSE.equals(enabled) && this.enabledPropertyKeys != null && !this.enabledPropertyKeys.isEmpty())
        {
            final AtomicBoolean enabled2 = new AtomicBoolean(true);
            this.enabledPropertyKeys.forEach(key -> {
                final String property = this.propertiesSource.getProperty(key);
                enabled2.compareAndSet(true, property != null ? Boolean.parseBoolean(property) : false);
            });
            enabled = Boolean.valueOf(enabled2.get());
        }

        return Boolean.TRUE.equals(enabled);
    }
}
