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
package de.acosix.alfresco.utility.share.config;

import java.io.Serializable;

import org.alfresco.util.ParameterCheck;

/**
 * This class provides a simple value holder implementation for config elements to manage the three- or more fold state of configuration
 * settings (undefined, set, explicitly unset).
 *
 * This class is not thread-safe.
 *
 * @author Axel Faust
 */
public final class ConfigValueHolder<T> implements Serializable
{

    private static final long serialVersionUID = 8715468305579932370L;

    private T value;

    private boolean unset = false;

    /**
     * Creates a new instance of this class without a default value / state.
     */
    public ConfigValueHolder()
    {
        // NO-OP
    }

    /**
     * Creates a new instance of this class with a default value / state.
     *
     * @param value
     *            the default value - must not be {@code null}
     */
    public ConfigValueHolder(final T value)
    {
        ParameterCheck.mandatory("value", value);
        this.value = value;
    }

    /**
     * Retrieves the internal flag for the state of "explicitly unset".
     *
     * @return {@code true} if the value was unset, {@code false} otherwise
     */
    public boolean isUnset()
    {
        final boolean unset = this.value == null && this.unset;
        return unset;
    }

    /**
     * Retrieves the current value of this instance.
     *
     * @return the value or {@code null} if no value was configured or the state is "explicitly unset"
     */
    public T getValue()
    {
        return this.value;
    }

    /**
     * Sets the current value of this instance. If the provided value is {@code null} then this operation will set the flag for the
     * "explicitly unset" state. If the provided value is not {@code null}, the flag for the "explicitly unset" state is transparently
     * removed if already set.
     *
     * @param value
     *            the value to set
     */
    public void setValue(final T value)
    {
        this.unset = value == null;
        this.value = value;
    }

    /**
     * Unsets the current value of this instance. This operation always sets the flag for the "explicitly unset" state.
     */
    public void unset()
    {
        this.unset = true;
        this.value = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.unset ? 1231 : 1237);
        result = prime * result + ((this.value == null) ? 0 : this.value.hashCode());
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
        if (!(obj instanceof ConfigValueHolder))
        {
            return false;
        }
        final ConfigValueHolder<?> other = (ConfigValueHolder<?>) obj;
        if (this.unset != other.unset)
        {
            return false;
        }
        if (this.value == null)
        {
            if (other.value != null)
            {
                return false;
            }
        }
        else if (!this.value.equals(other.value))
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
        builder.append("ConfigValueHolder [");
        if (this.value != null)
        {
            builder.append("value=");
            builder.append(this.value);
            builder.append(", ");
        }
        builder.append("unset=");
        builder.append(this.unset);
        builder.append("]");
        return builder.toString();
    }

}
