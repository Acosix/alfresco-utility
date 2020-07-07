/*
 * Copyright 2016 - 2020 Acosix GmbH
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
package de.acosix.alfresco.utility.common.spring.condition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * @author Axel Faust
 */
public class BeanDefinedCondition extends BaseBeanDefinitionPostProcessorCondition
{

    private static final Logger LOGGER = LoggerFactory.getLogger(BeanDefinedCondition.class);

    protected String beanName;

    /**
     * Constructs a new instance of this condition for subsequent configuration.
     */
    public BeanDefinedCondition()
    {
        // NO-OP
    }

    /**
     * Constructs a new instance of this condition with an initial configuration.
     *
     * @param beanName
     *            the name of the bean to check for having been defined
     * @param negate
     *            {@code true} if the result of this condition's check should be negated / inversed, {@code false} if not
     */
    public BeanDefinedCondition(final String beanName, final boolean negate)
    {
        super();
        this.beanName = beanName;
        this.negate = negate;
    }

    /**
     * @return the name of the bean to check for having been defined
     */
    public String getBeanName()
    {
        return this.beanName;
    }

    /**
     * @param beanName
     *            the name of the bean to check for having been defined
     */
    public void setBeanName(final String beanName)
    {
        this.beanName = beanName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean applies(final BeanFactory factory)
    {
        final boolean baseApplies = factory.containsBean(this.beanName);

        LOGGER.debug("Result of checking existence of bean definition for {}: {} (negation: {})", this.beanName, baseApplies, this.negate);

        final boolean result = baseApplies != this.negate;
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean applies(final BeanDefinitionRegistry registry)
    {
        final boolean baseApplies = registry.containsBeanDefinition(this.beanName);

        LOGGER.debug("Result of checking existence of bean definition for {}: {} (negation: {})", this.beanName, baseApplies, this.negate);

        final boolean result = baseApplies != this.negate;
        return result;
    }

}
