/*
 * Copyright 2017 Acosix GmbH
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
package de.acosix.alfresco.utility.repo.subsystems;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.management.subsystems.PropertyBackedBean;
import org.alfresco.repo.management.subsystems.PropertyBackedBeanState;
import org.alfresco.util.EqualsHelper;
import org.alfresco.util.ParameterCheck;
import org.springframework.context.ApplicationContext;

/**
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class SubsystemWithClassLoaderManagerState implements PropertyBackedBeanState
{

    /**
     * Instances of this interface are capable of initialising the {@link PropertyBackedBean} references of a subystem context factory.
     *
     * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
     */
    @FunctionalInterface
    public interface SubsystemWithClassLoaderFactoryInitialiser
    {

        /**
         * Initialises the state of a provided subsystem context factory, specifically its {@link PropertyBackedBean} references.
         *
         * @param subsystemContextFactory
         *            the subsystem context factory to initialise
         */
        void initialise(SubsystemWithClassLoaderFactory subsystemContextFactory);
    }

    public static final String CHAIN_PROPERTY = "$chain";

    private static final String DEFAULT_TYPE = "default";

    protected final List<String> instanceIds = new ArrayList<>(10);

    protected final Map<String, SubsystemWithClassLoaderFactory> subsystems = new TreeMap<>();

    protected final String defaultTypeName;

    protected final SubsystemWithClassLoaderFactoryInitialiser initialiser;

    protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    public SubsystemWithClassLoaderManagerState(final String defaultChain, final String defaultTypeName,
            final SubsystemWithClassLoaderFactoryInitialiser initialiser)
    {
        ParameterCheck.mandatory("initialiser", initialiser);
        this.initialiser = initialiser;

        if (defaultChain != null && defaultChain.length() > 0)
        {
            if (defaultTypeName == null)
            {
                this.defaultTypeName = DEFAULT_TYPE;
            }
            else
            {
                this.defaultTypeName = defaultTypeName;
            }
            this.setChain(defaultChain, this.defaultTypeName);
        }
        else if (defaultTypeName == null)
        {
            this.defaultTypeName = DEFAULT_TYPE;
        }
        else
        {
            this.defaultTypeName = defaultTypeName;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getPropertyNames()
    {
        return Collections.singleton(CHAIN_PROPERTY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProperty(final String name)
    {
        ParameterCheck.mandatoryString("name", name);
        String result;
        switch (name)
        {
            case CHAIN_PROPERTY:
                this.lock.readLock().lock();
                try
                {
                    final StringBuilder chainBuilder = new StringBuilder();
                    this.instanceIds.forEach(id -> {
                        if (chainBuilder.length() > 0)
                        {
                            chainBuilder.append(',');
                        }
                        chainBuilder.append(id).append(',').append(this.subsystems.get(id).getTypeName());
                    });
                    result = chainBuilder.toString();
                }
                finally
                {
                    this.lock.readLock().unlock();
                }
                break;
            default:
                result = null;
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProperty(final String name, final String value)
    {
        ParameterCheck.mandatoryString("name", name);

        switch (name)
        {
            case CHAIN_PROPERTY:
                this.lock.writeLock().lock();
                try
                {
                    this.setChain(value, this.defaultTypeName);
                }
                finally
                {
                    this.lock.writeLock().unlock();
                }
                break;
            default:
                throw new IllegalStateException("Illegal attempt to write to property \"" + name + "\"");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeProperty(final String name)
    {
        ParameterCheck.mandatoryString("name", name);
        throw new UnsupportedOperationException("All properties are non-removable");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start()
    {
        this.lock.writeLock().lock();
        try
        {
            boolean oneSuccess = false;
            RuntimeException lastError = null;
            for (final String instance : this.getInstanceIds())
            {
                try
                {
                    final SubsystemWithClassLoaderFactory subsystemContextFactory = this.subsystems.get(instance);
                    subsystemContextFactory.start();
                    oneSuccess = true;
                }
                catch (final RuntimeException e)
                {
                    lastError = e;
                }
            }

            if (lastError != null && !oneSuccess)
            {
                throw lastError;
            }
        }
        finally
        {
            this.lock.writeLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop()
    {
        this.lock.writeLock().lock();
        try
        {
            boolean oneSuccess = false;
            RuntimeException lastError = null;

            for (final String instance : this.getInstanceIds())
            {
                try
                {
                    final SubsystemWithClassLoaderFactory subsystemContextFactory = this.subsystems.get(instance);
                    subsystemContextFactory.stop();
                    oneSuccess = true;
                }
                catch (final RuntimeException e)
                {
                    lastError = e;
                }
            }

            if (lastError != null && !oneSuccess)
            {
                throw lastError;
            }
        }
        finally
        {
            this.lock.writeLock().unlock();
        }
    }

    /**
     * Gets the instance ids.
     *
     * @return the instance ids
     */
    public Collection<String> getInstanceIds()
    {
        this.lock.readLock().lock();
        try
        {
            return new ArrayList<>(this.instanceIds);
        }
        finally
        {
            this.lock.readLock().unlock();
        }
    }

    /**
     * Gets the application context.
     *
     * @param id
     *            the id
     * @return the application context
     */
    public ApplicationContext getApplicationContext(final String id)
    {
        this.lock.readLock().lock();
        try
        {
            final SubsystemWithClassLoaderFactory subsystemContextFactory = this.subsystems.get(id);
            return subsystemContextFactory == null ? null : subsystemContextFactory.getApplicationContext();
        }
        finally
        {
            this.lock.readLock().unlock();
        }
    }

    /**
     * Gets the application context factory.
     *
     * @param id
     *            the id
     * @return the application context factory
     */
    public SubsystemWithClassLoaderFactory getApplicationContextFactory(final String id)
    {
        this.lock.readLock().lock();
        try
        {
            final SubsystemWithClassLoaderFactory subsystemContextFactory = this.subsystems.get(id);
            return subsystemContextFactory;
        }
        finally
        {
            this.lock.readLock().unlock();
        }
    }

    protected void setChain(final String chain, final String defaultTypeName)
    {
        final String[] instanceTokens = chain.split("[,\\s]+");

        final List<String> instanceIds = new ArrayList<>();
        final Map<String, SubsystemWithClassLoaderFactory> subsystems = new HashMap<>();

        for (final String instance : instanceTokens)
        {
            final int colonIdx = instance.indexOf(':');
            final String id = colonIdx == -1 ? instance : instance.substring(0, colonIdx);
            instanceIds.add(id);
            final String typeName = colonIdx == -1 || (colonIdx + 1) >= instance.length() ? defaultTypeName
                    : instance.substring(colonIdx + 1);

            SubsystemWithClassLoaderFactory subsystemContextFactory = this.subsystems.get(id);
            if (subsystemContextFactory != null && !EqualsHelper.nullSafeEquals(subsystemContextFactory.getTypeName(), typeName))
            {
                subsystemContextFactory.destroy();
                subsystemContextFactory = null;
            }

            if (subsystemContextFactory == null)
            {
                final List<String> instancePath = new ArrayList<>();
                instancePath.add("managed");
                instancePath.add(id);
                subsystemContextFactory = new SubsystemWithClassLoaderFactory();
                this.initialiser.initialise(subsystemContextFactory);
                subsystemContextFactory.setTypeName(typeName);
                subsystemContextFactory.setInstancePath(instancePath);
                try
                {
                    subsystemContextFactory.afterPropertiesSet();
                }
                catch (final Exception e)
                {
                    if (e instanceof RuntimeException)
                    {
                        throw (RuntimeException) e;
                    }
                    throw new AlfrescoRuntimeException("Error instantiating subsystem context factory", e);
                }
            }
            subsystems.put(id, subsystemContextFactory);
        }

        this.subsystems.clear();
        this.subsystems.putAll(subsystems);

        final Set<String> idsToRemove = new TreeSet<>(this.subsystems.keySet());
        idsToRemove.removeAll(instanceIds);
        for (final String id : idsToRemove)
        {
            final SubsystemWithClassLoaderFactory subsystemContextFactory = this.subsystems.remove(id);
            subsystemContextFactory.destroy();
        }

        this.instanceIds.clear();
        this.instanceIds.addAll(instanceIds);
    }
}
