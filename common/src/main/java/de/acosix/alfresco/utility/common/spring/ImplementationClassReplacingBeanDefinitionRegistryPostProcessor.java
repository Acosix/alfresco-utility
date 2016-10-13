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

import java.util.Properties;

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
public class ImplementationClassReplacingBeanDefinitionRegistryPostProcessor extends ImplementationClassReplacingBeanFactoryPostProcessor
        implements BeanDefinitionRegistryPostProcessor
{

    private static final Logger LOGGER = LoggerFactory.getLogger(ImplementationClassReplacingBeanDefinitionRegistryPostProcessor.class);

    protected String activePropertyKey;

    protected Properties propertiesSource;

    /**
     * @param activePropertyKey
     *            the activePropertyKey to set
     */
    public void setActivePropertyKey(final String activePropertyKey)
    {
        this.activePropertyKey = activePropertyKey;
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
        if (this.activePropertyKey != null && this.propertiesSource != null)
        {
            final boolean active = Boolean.parseBoolean(this.propertiesSource.getProperty(this.activePropertyKey));

            if (active && this.targetBeanName != null && this.replacementClassName != null)
            {
                applyChange(beanName -> {
                    return registry.getBeanDefinition(beanName);
                });
            }
            else if (!active)
            {
                LOGGER.info("[{}] patch will not be applied as it has been marked as inactive", this.beanName);
            }
            else
            {
                LOGGER.warn("[{}] patch cannnot be applied as its configuration is incomplete", this.beanName);
            }
        }
        else
        {
            LOGGER.warn("[{}] patch cannnot be applied as its configuration is incomplete", this.beanName);
        }
    }
}
