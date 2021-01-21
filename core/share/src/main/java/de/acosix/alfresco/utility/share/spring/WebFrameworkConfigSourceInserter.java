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
package de.acosix.alfresco.utility.share.spring;

import java.util.List;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import org.alfresco.util.PropertyCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.extensions.config.source.UrlConfigSource;

/**
 * Instances of this class insert Surf Web Framework config source URLs into the {@link UrlConfigSource#UrlConfigSource(java.util.List) URL
 * config source constructor}. This may be necessary for the correct functioning of Alfresco Share extensions, as the default config source
 * URL order is <a href="https://issues.alfresco.com/jira/browse/ALF-21820">broken</a> with no intention by Alfresco to be fixed. This issue
 * would otherwise prevent an extension from providing a default configuration that can be overriden by administrators using a
 * {@code share-config-custom.xml} file.
 *
 * This inserter can insert new config source URLs before/after some specific reference config source URLs to ensure proper ordering. If an
 * 'after' reference URL is configured, the new config source URL will be inserted as early as possible (immediately after the reference
 * URL), otherwise it will be inserted as late as possible (immediately before the 'before' reference URL). This class will throw an error
 * if the order of the reference URLs in the current Spring bean configuration is reversed or one of the reference URLs cannot be found.
 *
 *
 * @author Axel Faust
 */
public class WebFrameworkConfigSourceInserter implements BeanDefinitionRegistryPostProcessor
{

    private static final Logger LOGGER = LoggerFactory.getLogger(WebFrameworkConfigSourceInserter.class);

    protected List<String> beforeConfigSources;

    protected List<String> afterConfigSources;

    protected List<String> configSources;

    /**
     * @param beforeConfigSources
     *            the beforeConfigSources to set
     */
    public void setBeforeConfigSources(final List<String> beforeConfigSources)
    {
        this.beforeConfigSources = beforeConfigSources;
    }

    /**
     * @param afterConfigSources
     *            the afterConfigSources to set
     */
    public void setAfterConfigSources(final List<String> afterConfigSources)
    {
        this.afterConfigSources = afterConfigSources;
    }

    /**
     * @param configSources
     *            the configSources to set
     */
    public void setConfigSources(final List<String> configSources)
    {
        this.configSources = configSources;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) throws BeansException
    {
        // NO-OP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postProcessBeanDefinitionRegistry(final BeanDefinitionRegistry registry) throws BeansException
    {
        PropertyCheck.mandatory(this, "configSources", this.configSources);

        final BeanDefinition configSourceBean = registry.getBeanDefinition("webframework.configsource");
        final ConstructorArgumentValues ctorArguments = configSourceBean.getConstructorArgumentValues();
        final ValueHolder ctorArgumentValue = ctorArguments.getGenericArgumentValue(null);
        if (ctorArgumentValue == null)
        {
            throw new IllegalStateException("Failed to lookup constructor argument of bean webframework.configsource");
        }

        final Object value = ctorArgumentValue.getValue();
        if (!(value instanceof List<?>))
        {
            throw new IllegalStateException("Constructor argument of bean webframework.configsource is not a list");
        }

        @SuppressWarnings("unchecked")
        final List<TypedStringValue> sources = (List<TypedStringValue>) value;

        int startIdx = 0;
        if (this.afterConfigSources != null)
        {
            final Integer latestIdx = this.afterConfigSources.stream().map(TypedStringValue::new).map(sources::indexOf)
                    .reduce(BinaryOperator.<Integer> maxBy((o1, o2) -> o1.compareTo(o2))).get();
            startIdx = latestIdx.intValue();
            if (startIdx != -1)
            {
                startIdx++;
            }
        }

        int endIdx = sources.size();
        if (this.beforeConfigSources != null)
        {
            final Integer earliestIdx = this.beforeConfigSources.stream().map(TypedStringValue::new).map(sources::indexOf)
                    .reduce(BinaryOperator.<Integer> minBy((o1, o2) -> o1.compareTo(o2))).get();
            endIdx = earliestIdx.intValue();
        }

        if (startIdx == -1)
        {
            throw new IllegalStateException("None of the 'after' config source URLs " + this.afterConfigSources + " could be found");
        }

        if (endIdx == -1)
        {
            throw new IllegalStateException("None of the 'before' source URLs " + this.beforeConfigSources + " could be found");
        }

        if (endIdx < startIdx)
        {
            throw new IllegalStateException(
                    "Order of 'after' and 'before' source URLs does not allow for any source URLs to be inserted between them");
        }

        if (startIdx == 0)
        {
            // insert as late as possible (no afterConfigSource)
            sources.addAll(endIdx, this.configSources.stream().map(TypedStringValue::new).collect(Collectors.toList()));
            LOGGER.info("Inserted config source(s) {} in webframework.configsource sources list at index {}", this.configSources, endIdx);
        }
        else
        {
            // insert as early as possible
            sources.addAll(startIdx, this.configSources.stream().map(TypedStringValue::new).collect(Collectors.toList()));
            LOGGER.info("Inserted config source(s) {} in webframework.configsource sources list at index {}", this.configSources, startIdx);
        }
    }

}
