/*
 * Copyright 2016 - 2018 Acosix GmbH
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
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;

/**
 * {@link BeanFactoryPostProcessor Bean factory post processor} to alter the implementation class of a bean definition without requiring an
 * override that may conflict with custom Spring configuration.
 *
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class ImplementationClassReplacingBeanFactoryPostProcessor<D extends BeanFactoryPostProcessor>
        extends BaseBeanFactoryPostProcessor<D>
{

    private static final Logger LOGGER = LoggerFactory.getLogger(ImplementationClassReplacingBeanFactoryPostProcessor.class);

    protected String originalClassName;

    protected String replacementClassName;

    /**
     * @param originalClassName
     *            the originalClassName to set
     */
    public void setOriginalClassName(final String originalClassName)
    {
        this.originalClassName = originalClassName;
    }

    /**
     * @param replacementClassName
     *            the replacementClassName to set
     */
    public void setReplacementClassName(final String replacementClassName)
    {
        this.replacementClassName = replacementClassName;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void applyChange(final BeanDefinition affectedBeanDefinition, final Function<String, BeanDefinition> getBeanDefinition)
    {
        if (this.replacementClassName != null)
        {
            if (this.originalClassName == null || this.originalClassName.equals(affectedBeanDefinition.getBeanClassName()))
            {
                LOGGER.info("[{}] Patching implementation class of bean {} to {}", this.beanName, this.targetBeanName,
                        this.replacementClassName);
                affectedBeanDefinition.setBeanClassName(this.replacementClassName);
            }
            else
            {
                LOGGER.info("[{}] patch will not be applied - class of bean {} does not match expected implementation {}", this.beanName,
                        this.targetBeanName, this.originalClassName);
            }
        }
        else
        {
            LOGGER.warn("[{}] patch cannnot be applied as it does not define a replacement class", this.beanName);
        }
    }

}