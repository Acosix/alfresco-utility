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

import java.lang.reflect.Constructor;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.util.PropertyPlaceholderHelper;

/**
 * Instances of this class provide
 *
 * @author Axel Faust
 */
public class PropertyPlaceholderHelperFactory implements FactoryBean<PropertyPlaceholderHelper>
{

    protected String placeholderPrefix = PlaceholderConfigurerSupport.DEFAULT_PLACEHOLDER_PREFIX;

    protected String placeholderSuffix = PlaceholderConfigurerSupport.DEFAULT_PLACEHOLDER_SUFFIX;

    protected String valueSeparator = PlaceholderConfigurerSupport.DEFAULT_VALUE_SEPARATOR;

    protected Character escapeCharacter = '\\';

    protected boolean ignoreUnresolvablePlaceholders = true;

    /**
     * Sets the placeholder prefix to use.
     *
     * @param placeholderPrefix
     *     the placeholderPrefix to set
     */
    public void setPlaceholderPrefix(final String placeholderPrefix)
    {
        this.placeholderPrefix = placeholderPrefix;
    }

    /**
     * Sets the placeholder suffix to use.
     *
     * @param placeholderSuffix
     *     the placeholderSuffix to set
     */
    public void setPlaceholderSuffix(final String placeholderSuffix)
    {
        this.placeholderSuffix = placeholderSuffix;
    }

    /**
     * Sets the value separator to use.
     *
     * @param valueSeparator
     *     the valueSeparator to set
     */
    public void setValueSeparator(final String valueSeparator)
    {
        this.valueSeparator = valueSeparator;
    }

    /**
     * Sets the escape character to use.
     *
     * @param escapeCharacter
     *     the escapeCharacter to set
     */
    public void setEscapeCharacter(final Character escapeCharacter)
    {
        this.escapeCharacter = escapeCharacter;
    }

    /**
     * Sets whether to ignore unresolvable placeholders.
     *
     * @param ignoreUnresolvablePlaceholders
     *     the ignoreUnresolvablePlaceholders to set
     */
    public void setIgnoreUnresolvablePlaceholders(final boolean ignoreUnresolvablePlaceholders)
    {
        this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PropertyPlaceholderHelper getObject() throws Exception
    {
        Object[] params;
        // Spring 7.x
        Constructor<PropertyPlaceholderHelper> ctor = this.lookup(String.class, String.class, String.class, Character.class, boolean.class);
        if (ctor != null)
        {
            params = new Object[] { this.placeholderPrefix, this.placeholderSuffix, this.valueSeparator, this.escapeCharacter,
                    this.ignoreUnresolvablePlaceholders };
        }
        else
        {
            ctor = this.lookup(String.class, String.class, String.class, boolean.class);
            params = new Object[] { this.placeholderPrefix, this.placeholderSuffix, this.valueSeparator,
                    this.ignoreUnresolvablePlaceholders };
        }
        return ctor.newInstance(params);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSingleton()
    {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getObjectType()
    {
        return PropertyPlaceholderHelper.class;
    }

    private Constructor<PropertyPlaceholderHelper> lookup(final Class<?>... types)
    {
        Constructor<PropertyPlaceholderHelper> ctor = null;
        try
        {
            ctor = PropertyPlaceholderHelper.class.getConstructor(types);
        }
        catch (final NoSuchMethodException ignore)
        {
            // ignored
        }

        return ctor;
    }
}
