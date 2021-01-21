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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

/**
 * {@link BeanDefinitionRegistryPostProcessor Bean definition registry post processor} to alter a property of a bean definition with adapted
 * configuration before instantiation without requiring an override that may conflict with custom Spring configuration. Instances of this
 * class can be used to adapt Spring lifecycle interfaces-implementing beans before they are discovered (except other bean definition
 * registry post processors). Due to the early Spring lifecycle phase that instances of this class are used in, property placeholders can
 * not be used to configure properties.
 *
 * @author Axel Faust
 */
public class PropertyAlteringBeanDefinitionRegistryPostProcessor
        extends PropertyAlteringBeanFactoryPostProcessor<BeanDefinitionRegistryPostProcessor> implements BeanDefinitionRegistryPostProcessor
{

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
        this.execute(registry, this.dependsOn, this::applyChange);
    }
}
