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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.alfresco.repo.management.subsystems.AbstractPropertyBackedBean;
import org.alfresco.repo.management.subsystems.ChildApplicationContextFactory;
import org.alfresco.repo.management.subsystems.PropertyBackedBeanState;
import org.alfresco.util.ParameterCheck;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.util.DefaultPropertiesPersister;
import org.springframework.util.PropertiesPersister;

import de.acosix.alfresco.utility.repo.subsystems.SubsystemWithClassLoaderManagerState.SubsystemWithClassLoaderFactoryInitialiser;

/**
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class SubsystemWithClassLoaderManager extends AbstractPropertyBackedBean implements MultiInstanceSubsystemHandler
{

    protected String defaultTypeName;

    protected String defaultChain;

    protected PropertiesPersister persister = new DefaultPropertiesPersister();

    // field in super is inaccessible and getter is only introduced in 5.2
    protected Properties encryptedPropertyDefaults;

    {
        this.setInstancePath(Collections.singletonList("manager"));
    }

    /**
     * Sets the default type name. This is used when a type name is not included after an instance ID in a chain string.
     *
     * @param defaultTypeName
     *            the new default type name
     */
    public void setDefaultTypeName(final String defaultTypeName)
    {
        this.defaultTypeName = defaultTypeName;
    }

    /**
     * Configures the default chain of {@link ChildApplicationContextFactory} instances. May be set on initialization by
     * the Spring container.
     *
     * @param defaultChain
     *            a comma separated list in the following format:
     *            <ul>
     *            <li>&lt;id1&gt;:&lt;typeName1&gt;,&lt;id2&gt;:&lt;typeName2&gt;,...,&lt;id<i>n</i>&gt;:&lt;typeName<i>n</i>&gt;
     *            </ul>
     */
    public void setDefaultChain(final String defaultChain)
    {
        this.defaultChain = defaultChain;
    }

    /**
     * @param persister
     *            the persister to set
     */
    public void setPersister(final PropertiesPersister persister)
    {
        ParameterCheck.mandatory("persister", persister);
        this.persister = persister;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void setEncryptedPropertyDefaults(final Properties propertyDefaults)
    {
        super.setEncryptedPropertyDefaults(propertyDefaults);
        this.encryptedPropertyDefaults = propertyDefaults;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getDescription(final String name)
    {
        ParameterCheck.mandatoryString("name", name);

        String result;
        switch (name)
        {
            case SubsystemWithClassLoaderManagerState.CHAIN_PROPERTY:
                result = "Comma separated list of name:type pairs";
                break;
            default:
                result = super.getDescription(name);
        }
        return result;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void destroy(final boolean permanent)
    {
        if (this.getState(false) != null)
        {
            final boolean hadWriteLock = this.lock.isWriteLockedByCurrentThread();
            if (!hadWriteLock)
            {
                this.lock.readLock().unlock();
                this.lock.writeLock().lock();
            }
            try
            {
                final SubsystemWithClassLoaderManagerState state = (SubsystemWithClassLoaderManagerState) this.getState(false);

                if (state != null)
                {
                    // Cascade the destroy / shutdown
                    for (final String id : state.getInstanceIds())
                    {
                        final SubsystemWithClassLoaderFactory subsystemContextFactory = state.getApplicationContextFactory(id);
                        subsystemContextFactory.destroy(permanent);
                    }
                }

                super.destroy(permanent);
            }
            finally
            {
                if (!hadWriteLock)
                {
                    this.lock.readLock().lock();
                    this.lock.writeLock().unlock();
                }
            }
        }
        else
        {
            super.destroy(permanent);
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected PropertyBackedBeanState createInitialState() throws IOException
    {
        final SubsystemWithClassLoaderFactoryInitialiser initialiser = (subsystemContextFactory) -> {
            subsystemContextFactory.setApplicationContext(this.getParent());
            subsystemContextFactory.setCategory(this.getCategory());
            subsystemContextFactory.setPropertyDefaults(this.getPropertyDefaults());
            subsystemContextFactory.setEncryptedPropertyDefaults(this.encryptedPropertyDefaults);
            subsystemContextFactory.setPersister(this.persister);
            subsystemContextFactory.setRegistry(this.getRegistry());
        };
        final SubsystemWithClassLoaderManagerState state = new SubsystemWithClassLoaderManagerState(this.defaultChain, this.defaultTypeName,
                initialiser);
        return state;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getInstanceIds()
    {
        this.lock.readLock().lock();
        try
        {
            return ((SubsystemWithClassLoaderManagerState) this.getState(true)).getInstanceIds();
        }
        finally
        {
            this.lock.readLock().unlock();
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public ApplicationContext getApplicationContext(final String id)
    {
        this.lock.readLock().lock();
        try
        {
            return ((SubsystemWithClassLoaderManagerState) this.getState(true)).getApplicationContext(id);
        }
        finally
        {
            this.lock.readLock().unlock();
        }
    }

    public SubsystemWithClassLoaderFactory getChildApplicationContextFactory(final String id)
    {
        this.lock.readLock().lock();
        try
        {
            final SubsystemWithClassLoaderManagerState state = (SubsystemWithClassLoaderManagerState) this.getState(true);
            return state.getApplicationContextFactory(id);
        }
        finally
        {
            this.lock.readLock().unlock();
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String determineInstanceId(final ApplicationContext childApplicationContext)
    {
        this.lock.readLock().lock();
        try
        {
            final SubsystemWithClassLoaderManagerState state = (SubsystemWithClassLoaderManagerState) this.getState(false);

            final Collection<String> instanceIds = state.getInstanceIds();
            final AtomicReference<String> matchingInstanceId = new AtomicReference<>(null);

            for (final String id : instanceIds)
            {
                if (matchingInstanceId.get() == null)
                {
                    final SubsystemWithClassLoaderFactory subsystemContextFactory = state.getApplicationContextFactory(id);
                    final ApplicationContext readOnlyApplicationContext = subsystemContextFactory.getReadOnlyApplicationContext();

                    if (readOnlyApplicationContext == childApplicationContext)
                    {
                        matchingInstanceId.set(id);
                    }
                }
            }

            return matchingInstanceId.get();
        }
        finally

        {
            this.lock.readLock().unlock();
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Resource[] getSubsystemDefaultPropertiesResources(final String instanceId)
    {
        this.lock.readLock().lock();
        try
        {
            final SubsystemWithClassLoaderManagerState state = (SubsystemWithClassLoaderManagerState) this.getState(false);
            final SubsystemWithClassLoaderFactory subsystemContextFactory = state.getApplicationContextFactory(instanceId);
            final Resource[] subsystemDefaultPropertiesResources = subsystemContextFactory.getSubsystemDefaultPropertiesResources();
            return subsystemDefaultPropertiesResources;
        }
        finally

        {
            this.lock.readLock().unlock();
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Resource[] getSubsystemExtensionPropertiesResources(final String instanceId)
    {
        this.lock.readLock().lock();
        try
        {
            final SubsystemWithClassLoaderManagerState state = (SubsystemWithClassLoaderManagerState) this.getState(false);
            final SubsystemWithClassLoaderFactory subsystemContextFactory = state.getApplicationContextFactory(instanceId);
            final Resource[] subsystemExtensionPropertiesResources = subsystemContextFactory.getSubsystemExtensionPropertiesResources();
            return subsystemExtensionPropertiesResources;
        }
        finally

        {
            this.lock.readLock().unlock();
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Properties getSubsystemEffectiveProperties(final String instanceId)
    {
        this.lock.readLock().lock();
        try
        {
            final SubsystemWithClassLoaderManagerState state = (SubsystemWithClassLoaderManagerState) this.getState(false);
            final SubsystemWithClassLoaderFactory subsystemContextFactory = state.getApplicationContextFactory(instanceId);

            final Properties effectiveProperties = subsystemContextFactory != null
                    ? subsystemContextFactory.getSubsystemEffectiveProperties() : new Properties();
            return effectiveProperties;
        }
        finally

        {
            this.lock.readLock().unlock();
        }
    }
}
