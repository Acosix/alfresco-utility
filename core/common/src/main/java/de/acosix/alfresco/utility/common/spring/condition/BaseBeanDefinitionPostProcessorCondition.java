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
package de.acosix.alfresco.utility.common.spring.condition;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * @author Axel Faust
 */
public class BaseBeanDefinitionPostProcessorCondition implements BeanDefinitionPostProcessorCondition
{

    protected boolean negate;

    /**
     * @return {@code true} if the result of this condition's check should be negated / inversed, {@code false} if not
     */
    public boolean isNegate()
    {
        return this.negate;
    }

    /**
     * @param negate
     *            {@code true} if the result of this condition's check should be negated / inversed, {@code false} if not
     */
    public void setNegate(final boolean negate)
    {
        this.negate = negate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean applies(final BeanFactory factory)
    {
        // default: does not apply unless negated
        return this.negate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean applies(final BeanDefinitionRegistry registry)
    {
        // default: does not apply unless negated
        return this.negate;
    }
}
