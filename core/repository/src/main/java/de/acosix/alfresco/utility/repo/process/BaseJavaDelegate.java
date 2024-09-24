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
package de.acosix.alfresco.utility.repo.process;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.util.PropertyCheck;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;

/**
 * Sub-class instances of this class are capable of self-registering with the Activiti bean registry in order to be resolveable via delegate
 * expressions in Activiti processes. This is necessary in order to allow Spring-managed beans to be used in processes instead of ad-hoc
 * instantiated ones, which by way of instantiation cannot make use of shared services / utilities.
 *
 * @author Axel Faust
 */
public abstract class BaseJavaDelegate implements InitializingBean, BeanNameAware, Serializable
{

    private static final long serialVersionUID = 1L;

    protected transient Map<Object, Object> beanRegistry;

    protected String beanName;

    /**
     *
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "beanRegistry", this.beanRegistry);
        PropertyCheck.mandatory(this, "beanName", this.beanName);

        // if name includes dots we have to build a bean model that can actually be resolved by Activiti expressions
        Map<Object, Object> currentBeanMap = this.beanRegistry;
        final String[] dotSeparateFragments = this.beanName.split("\\.");
        for (int idx = 0; idx < dotSeparateFragments.length - 1; idx++)
        {
            final String fragment = dotSeparateFragments[idx];

            Map<Object, Object> nextMap;
            if (currentBeanMap.containsKey(fragment))
            {
                final Object nextMapCandidate = currentBeanMap.get(fragment);
                if (!(nextMapCandidate instanceof Map<?, ?>))
                {
                    throw new IllegalStateException("Dot-notation does not contain maps for all intermediary steps");
                }
                // we know this to be true / safe, but compiler won't understand (this is reason for @SupressWarnings)
                nextMap = (Map<Object, Object>) nextMapCandidate;
            }
            else
            {
                nextMap = new HashMap<>();
                currentBeanMap.put(fragment, nextMap);
            }

            currentBeanMap = nextMap;
        }
        currentBeanMap.put(dotSeparateFragments[dotSeparateFragments.length - 1], this);
    }

    /**
     * @param beanRegistry
     *     the beanRegistry to set
     */
    public void setBeanRegistry(final Map<Object, Object> beanRegistry)
    {
        this.beanRegistry = beanRegistry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBeanName(final String beanName)
    {
        this.beanName = beanName;
    }
}
