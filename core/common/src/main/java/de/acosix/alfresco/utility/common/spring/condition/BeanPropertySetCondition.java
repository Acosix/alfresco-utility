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
package de.acosix.alfresco.utility.common.spring.condition;

import org.alfresco.error.AlfrescoRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * Instances of this condition check if a particular property has been set on a particular bean definition. The check will succeed only if
 * the bean in question exists and the value of the property has been set to anything other than {@code null}.
 *
 * @author Axel Faust
 */
public class BeanPropertySetCondition extends BaseBeanDefinitionPostProcessorCondition
{

    private static final Logger LOGGER = LoggerFactory.getLogger(BeanPropertySetCondition.class);

    protected String beanName;

    protected String propertyName;

    /**
     * Constructs a new instance of this condition for subsequent configuration.
     */
    public BeanPropertySetCondition()
    {
        // NO-OP
    }

    /**
     * Constructs a new instance of this condition with an initial configuration.
     *
     * @param beanName
     *            the name of the bean to check
     * @param propertyName
     *            the name of the property to check for having been set
     * @param negate
     *            {@code true} if the result of this condition's check should be negated / inversed, {@code false} if not
     */
    public BeanPropertySetCondition(final String beanName, final String propertyName, final boolean negate)
    {
        super();
        this.beanName = beanName;
        this.propertyName = propertyName;
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
     * @return the propertyName
     */
    public String getPropertyName()
    {
        return this.propertyName;
    }

    /**
     * @param propertyName
     *            the propertyName to set
     */
    public void setPropertyName(final String propertyName)
    {
        this.propertyName = propertyName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean applies(final BeanFactory factory)
    {
        final boolean containsBean = factory.containsBean(this.beanName);

        if (!(factory instanceof ConfigurableListableBeanFactory))
        {
            throw new AlfrescoRuntimeException(
                    "Cannot perform check on bean factory unless it implements the ConfigurableListableBeanFactory interface");
        }

        boolean baseApplies = containsBean;
        if (baseApplies)
        {
            final BeanDefinition beanDefinition = ((ConfigurableListableBeanFactory) factory).getBeanDefinition(this.beanName);
            final PropertyValue propertyValue = beanDefinition.getPropertyValues().getPropertyValue(this.propertyName);

            baseApplies = propertyValue != null && propertyValue.getValue() != null;
        }

        LOGGER.debug("Result of checking if property {} on bean {} has been set: {} (negation: {})", this.propertyName, this.beanName,
                baseApplies, this.negate);

        final boolean result = baseApplies != this.negate;
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean applies(final BeanDefinitionRegistry registry)
    {
        final boolean containsBean = registry.containsBeanDefinition(this.beanName);

        boolean baseApplies = containsBean;
        if (baseApplies)
        {
            final BeanDefinition beanDefinition = registry.getBeanDefinition(this.beanName);
            final PropertyValue propertyValue = beanDefinition.getPropertyValues().getPropertyValue(this.propertyName);

            baseApplies = propertyValue != null && propertyValue.getValue() != null;
        }

        LOGGER.debug("Result of checking if property {} on bean {} has been set: {} (negation: {})", this.propertyName, this.beanName,
                baseApplies, this.negate);

        final boolean result = baseApplies != this.negate;
        return result;
    }

}
