/*
 * Copyright 2019 Acosix GmbH
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
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * @author Axel Faust
 */
public class BeanTypeMatchCondition extends BaseBeanDefinitionPostProcessorCondition
{

    private static final Logger LOGGER = LoggerFactory.getLogger(BeanTypeMatchCondition.class);

    /**
     *
     * @author Axel Faust
     */
    public static enum TypeMatchMode
    {
        INSTANCE_OF,
        CLASS_MATCH;
    }

    protected TypeMatchMode typeMatchMode = TypeMatchMode.CLASS_MATCH;

    protected String beanName;

    protected String expectedTypeName;

    /**
     * Constructs a new instance of this condition for subsequent configuration.
     */
    public BeanTypeMatchCondition()
    {
        // NO-OP
    }

    /**
     * Constructs a new instance of this condition with an initial configuration.
     *
     * @param beanName
     *            the name of the bean to check for having a specific type
     * @param expectedTypeName
     *            the expected type that the bean should comply to
     * @param typeMatchMode
     *            the type of check to apply to the bean type
     * @param negate
     *            {@code true} if the result of this condition's check should be negated / inversed, {@code false} if not
     */
    public BeanTypeMatchCondition(final String beanName, final String expectedTypeName, final TypeMatchMode typeMatchMode,
            final boolean negate)
    {
        super();
        this.beanName = beanName;
        this.expectedTypeName = expectedTypeName;
        this.typeMatchMode = typeMatchMode;
        this.negate = negate;
    }

    /**
     * @return the type of check to apply to the bean type
     */
    public TypeMatchMode getTypeMatchMode()
    {
        return this.typeMatchMode;
    }

    /**
     * @param typeMatchMode
     *            the type of check to apply to the bean type
     */
    public void setTypeMatchMode(final TypeMatchMode typeMatchMode)
    {
        this.typeMatchMode = typeMatchMode;
    }

    /**
     * @return the name of the bean to check for having a specific type
     */
    public String getBeanName()
    {
        return this.beanName;
    }

    /**
     * @param beanName
     *            the name of the bean to check for having a specific type
     */
    public void setBeanName(final String beanName)
    {
        this.beanName = beanName;
    }

    /**
     * @return the expected type that the bean should comply to
     */
    public String getExpectedTypeName()
    {
        return this.expectedTypeName;
    }

    /**
     * @param expectedTypeName
     *            the expected type that the bean should comply to
     */
    public void setExpectedTypeName(final String expectedTypeName)
    {
        this.expectedTypeName = expectedTypeName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean applies(final BeanFactory factory)
    {
        boolean baseApplies = false;

        if (factory.containsBean(this.beanName))
        {
            final Class<?> beanType = factory.getType(this.beanName);

            if (beanType != null)
            {
                if (this.typeMatchMode == TypeMatchMode.CLASS_MATCH)
                {
                    baseApplies = beanType.getName().equals(this.expectedTypeName);

                    LOGGER.debug("Result of matching bean {} type {} against {} using match mode {}: {} (negation: {})", this.beanName,
                            beanType, this.expectedTypeName, this.typeMatchMode, baseApplies, this.negate);
                }
                else
                {
                    try
                    {
                        baseApplies = Class.forName(this.expectedTypeName).isAssignableFrom(beanType);

                        LOGGER.debug("Result of matching bean {} type {} against {} using match mode {}: {} (negation: {})", this.beanName,
                                beanType, this.expectedTypeName, this.typeMatchMode, baseApplies, this.negate);
                    }
                    catch (final ClassNotFoundException cnfEx)
                    {
                        LOGGER.debug(
                                "Expected type {} does not exist - failed to test bean {} pf type {} using match mode {} (negation: {})",
                                this.expectedTypeName, this.beanName, beanType, this.typeMatchMode, this.negate);
                    }
                }
            }
            else
            {
                LOGGER.debug("Bean {} does not exist - failed to test {} using match mode {} (negation: {})", this.beanName,
                        this.expectedTypeName, this.typeMatchMode, this.negate);
            }
        }

        final boolean result = baseApplies != this.negate;
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean applies(final BeanDefinitionRegistry registry)
    {
        boolean baseApplies = false;

        if (registry.containsBeanDefinition(this.beanName))
        {
            final BeanDefinition beanDefinition = registry.getBeanDefinition(this.beanName);
            final String beanClassName = beanDefinition.getBeanClassName();

            if (beanClassName != null)
            {
                if (this.typeMatchMode == TypeMatchMode.CLASS_MATCH)
                {
                    baseApplies = beanClassName.equals(this.expectedTypeName);

                    LOGGER.debug("Result of matching bean {} type {} against {} using match mode {}: {} (negation: {})", this.beanName,
                            beanClassName, this.expectedTypeName, this.typeMatchMode, baseApplies, this.negate);
                }
                else
                {
                    try
                    {
                        final Class<?> expectedType = Class.forName(this.expectedTypeName);
                        try
                        {
                            final Class<?> beanType = Class.forName(beanClassName);
                            baseApplies = expectedType.isAssignableFrom(beanType);

                            LOGGER.debug("Result of matching bean {} type {} against {} using match mode {}: {} (negation: {})",
                                    this.beanName, beanType, this.expectedTypeName, this.typeMatchMode, baseApplies, this.negate);
                        }
                        catch (final ClassNotFoundException cnfEx)
                        {
                            LOGGER.debug("Bean class {} does not exist - failed to test {} against {} using match mode {} (negation: {})",
                                    beanClassName, this.beanName, this.expectedTypeName, this.typeMatchMode, this.negate);
                        }
                    }
                    catch (final ClassNotFoundException cnfEx)
                    {
                        LOGGER.debug(
                                "Expected type {} does not exist - failed to test bean {} of type {} using match mode {} (negation: {})",
                                this.expectedTypeName, this.beanName, beanClassName, this.typeMatchMode, this.negate);
                    }
                }
            }
            else
            {
                LOGGER.debug("Bean {} does not exist - failed to test {} using match mode {} (negation: {})", this.beanName,
                        this.expectedTypeName, this.typeMatchMode, this.negate);
            }
        }

        final boolean result = baseApplies != this.negate;
        return result;
    }

}
