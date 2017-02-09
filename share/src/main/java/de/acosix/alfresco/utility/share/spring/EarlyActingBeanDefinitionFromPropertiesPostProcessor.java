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
package de.acosix.alfresco.utility.share.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import de.acosix.alfresco.utility.common.spring.BeanDefinitionFromPropertiesPostProcessor;

/**
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class EarlyActingBeanDefinitionFromPropertiesPostProcessor extends BeanDefinitionFromPropertiesPostProcessor
        implements BeanFactoryAware, ApplicationContextAware
{

    protected ApplicationContext applicationContext;

    protected BeanFactory beanFactory;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        super.afterPropertiesSet();

        if (this.beanFactory instanceof BeanDefinitionRegistry)
        {
            super.postProcessBeanDefinitionRegistry((BeanDefinitionRegistry) this.beanFactory);
        }
        else if (this.applicationContext instanceof BeanDefinitionRegistry)
        {
            super.postProcessBeanDefinitionRegistry((BeanDefinitionRegistry) this.applicationContext);
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException
    {
        this.applicationContext = applicationContext;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void setBeanFactory(final BeanFactory beanFactory) throws BeansException
    {
        this.beanFactory = beanFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postProcessBeanDefinitionRegistry(final BeanDefinitionRegistry beanDefinitionRegistry)
    {
        if (!(this.beanFactory instanceof BeanDefinitionRegistry || this.applicationContext instanceof BeanDefinitionRegistry))
        {
            super.postProcessBeanDefinitionRegistry(beanDefinitionRegistry);
        }
    }
}
