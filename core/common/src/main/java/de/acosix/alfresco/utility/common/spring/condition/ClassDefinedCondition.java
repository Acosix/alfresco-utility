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
 *
 * @author Axel Faust
 */
public class ClassDefinedCondition extends BaseBeanDefinitionPostProcessorCondition
{

    private String className;

    /**
     * @param className
     *     the name of the class to check if it is defined
     */
    public void setClassName(final String className)
    {
        this.className = className;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean applies(final BeanFactory factory)
    {
        return this.applies();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean applies(final BeanDefinitionRegistry registry)
    {
        return this.applies();
    }

    protected boolean applies()
    {
        boolean applies = false;
        try
        {
            Class.forName(this.className);
            applies = !this.negate;
        }
        catch (final ClassNotFoundException e)
        {
            applies = this.negate;
        }
        return applies;
    }
}
