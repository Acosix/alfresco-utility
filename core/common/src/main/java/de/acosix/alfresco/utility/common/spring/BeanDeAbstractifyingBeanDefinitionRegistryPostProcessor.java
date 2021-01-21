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

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

/**
 * {@link BeanDefinitionRegistryPostProcessor Bean definition registry post processor} to (bulk) de-abstract bean definitions matching a
 * defined naming pattern. This type of post processor may be relevant for modules that provide a lot of XML-based beans which should be
 * disabled (by being marked as abstract) unless enablement has been configured.
 *
 * @author Axel Faust
 */
public class BeanDeAbstractifyingBeanDefinitionRegistryPostProcessor
        extends BaseBeanFactoryPostProcessor<BeanDefinitionRegistryPostProcessor> implements BeanDefinitionRegistryPostProcessor
{

    private static final Logger LOGGER = LoggerFactory.getLogger(BeanDeAbstractifyingBeanDefinitionRegistryPostProcessor.class);

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

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void applyChange(final BeanDefinition affectedBeanDefinition, final Function<String, BeanDefinition> getBeanDefinition)
    {
        if (affectedBeanDefinition instanceof AbstractBeanDefinition)
        {
            ((AbstractBeanDefinition) affectedBeanDefinition).setAbstract(false);
        }
        else
        {
            LOGGER.warn("[{}] patch cannnot be applied on {} as it does not allow to set the abstract flag", affectedBeanDefinition);
        }
    }
}
