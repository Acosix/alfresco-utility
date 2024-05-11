/*
 * Copyright 2016 - 2024 Acosix GmbH
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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;

/**
 * {@link BeanFactoryPostProcessor Bean factory post processor} to (bulk) change the parent on bean definitions matching a defined naming
 * pattern.
 *
 * @author Axel Faust
 */
public class BeanParentAlteringBeanFactoryPostProcessor<D extends BeanFactoryPostProcessor> extends BaseBeanFactoryPostProcessor<D>
{

    protected String parentBeanName;

    /**
     * @return the parentBeanName
     */
    public String getParentBeanName()
    {
        return this.parentBeanName;
    }

    /**
     * @param parentBeanName
     *            the parentBeanName to set
     */
    public void setParentBeanName(final String parentBeanName)
    {
        this.parentBeanName = parentBeanName;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void applyChange(final BeanDefinition affectedBeanDefinition, final Function<String, BeanDefinition> getBeanDefinition)
    {
        affectedBeanDefinition.setParentName(this.parentBeanName);
    }
}
