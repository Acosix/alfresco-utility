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
package de.acosix.alfresco.utility.share.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.alfresco.util.ParameterCheck;
import org.springframework.extensions.config.ConfigElement;
import org.springframework.extensions.config.element.ConfigElementAdapter;

/**
 * This class provides an alternative to {@link ConfigElementAdapter} as a base class for custom Java-backed config element implementations,
 * which neither need nor want to support the XML-oriented default APIs concerning {@link #getChildren() child elements} or
 * {@link #getAttributes() attributes}. The XML-oriented default operations forced upon config element implementations by the base interface
 * are implemented as pure no-op by this class, either returning {@code null}, {@code false}, {@code 0} or empty lists/maps, depending on
 * the respective return types.
 *
 * @author Axel Faust
 */
public abstract class BaseCustomConfigElement implements ConfigElement
{

    private static final long serialVersionUID = 8082319631062216246L;

    protected final String name;

    /**
     * Creates a new instance of this class.
     *
     * @param name
     *            the name of this config element
     */
    protected BaseCustomConfigElement(final String name)
    {
        ParameterCheck.mandatoryString("name", name);
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName()
    {
        return this.name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAttribute(final String name)
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getAttributes()
    {
        return Collections.emptyMap();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasAttribute(final String name)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getAttributeCount()
    {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getValue()
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigElement getChild(final String name)
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getChildValue(final String name)
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ConfigElement> getChildren(final String name)
    {
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ConfigElement> getChildren()
    {
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasChildren()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getChildCount()
    {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (!(obj instanceof BaseCustomConfigElement))
        {
            return false;
        }
        final BaseCustomConfigElement other = (BaseCustomConfigElement) obj;
        if (this.name == null)
        {
            if (other.name != null)
            {
                return false;
            }
        }
        else if (!this.name.equals(other.name))
        {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        final StringBuilder builder = new StringBuilder();
        builder.append("BaseCustomConfigElement [");
        if (this.name != null)
        {
            builder.append("name=");
            builder.append(this.name);
        }
        builder.append("]");
        return builder.toString();
    }

}
