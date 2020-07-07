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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

/**
 * Instances of this interface represent conditions for the execution of either {@link BeanFactoryPostProcessor} or
 * {@link BeanDefinitionRegistryPostProcessor} logic.
 *
 * @author Axel Faust
 */
public interface BeanDefinitionPostProcessorCondition
{

    /**
     * Determines if this condition applies in the context of the provided bean factory and the specific conditional checks represented by
     * this instance.
     *
     * @param factory
     *            the bean factory to consider as the context, e.g. for testing any related beans / bean definitions
     * @return {@code true} if the condition represented by this instance applies, {@code false} otherwise
     */
    boolean applies(BeanFactory factory);

    /**
     * Determines if this condition applies in the context of the provided bean definition registry and the specific conditional checks
     * represented by this instance.
     *
     * @param registry
     *            the bean definition registry to consider as the context, e.g. for testing any related beans / bean definitions
     * @return {@code true} if the condition represented by this instance applies, {@code false} otherwise
     */
    boolean applies(BeanDefinitionRegistry registry);
}
