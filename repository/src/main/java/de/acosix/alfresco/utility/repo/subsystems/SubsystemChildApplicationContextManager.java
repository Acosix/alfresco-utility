/*
 * Copyright 2016, 2017 Acosix GmbH
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

import org.alfresco.repo.management.subsystems.AbstractPropertyBackedBean;
import org.alfresco.repo.management.subsystems.DefaultChildApplicationContextManager;
import org.alfresco.repo.management.subsystems.PropertyBackedBeanState;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

/**
 * This class provides a slightly enhanced variant of the Alfresco default {@link DefaultChildApplicationContextManager} which allows
 * interested client code to identify a specific subsystem instance based on its application context and to obtain the effective
 * {@code *.properties} files used to define the configuration of that specific subsystem instance. It can be used as a drop-in replacement
 * to the original class in all out-of-the-box use cases.
 *
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class SubsystemChildApplicationContextManager extends DefaultChildApplicationContextManager implements MultiInstanceSubsystemHandler
{

    /** The default type name. */
    protected String defaultTypeName;

    /** The default chain. */
    protected String defaultChain;

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDefaultTypeName(final String defaultTypeName)
    {
        super.setDefaultTypeName(defaultTypeName);
        this.defaultTypeName = defaultTypeName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDefaultChain(final String defaultChain)
    {
        super.setDefaultChain(defaultChain);
        this.defaultChain = defaultChain;
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
            final SubsystemApplicationContextManagerState state = (SubsystemApplicationContextManagerState) this.getState(false);

            final Collection<String> instanceIds = state.getInstanceIds();
            final AtomicReference<String> matchingInstanceId = new AtomicReference<>(null);

            for (final String id : instanceIds)
            {
                if (matchingInstanceId.get() == null)
                {
                    final SubsystemChildApplicationContextFactory applicationContextFactory = state.getApplicationContextFactory(id);
                    final ApplicationContext readOnlyApplicationContext = applicationContextFactory.getReadOnlyApplicationContext();

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
            final SubsystemApplicationContextManagerState state = (SubsystemApplicationContextManagerState) this.getState(false);
            final SubsystemChildApplicationContextFactory applicationContextFactory = state.getApplicationContextFactory(instanceId);
            final Resource[] subsystemDefaultPropertiesResources = applicationContextFactory.getSubsystemDefaultPropertiesResources();
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
            final SubsystemApplicationContextManagerState state = (SubsystemApplicationContextManagerState) this.getState(false);
            final SubsystemChildApplicationContextFactory applicationContextFactory = state.getApplicationContextFactory(instanceId);
            final Resource[] subsystemExtensionPropertiesResources = applicationContextFactory.getSubsystemExtensionPropertiesResources();
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
            final SubsystemApplicationContextManagerState state = (SubsystemApplicationContextManagerState) this.getState(false);
            final SubsystemChildApplicationContextFactory applicationContextFactory = state.getApplicationContextFactory(instanceId);

            final Properties effectiveProperties = applicationContextFactory != null
                    ? applicationContextFactory.getSubsystemEffectiveProperties() : new Properties();
            return effectiveProperties;
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
    protected PropertyBackedBeanState createInitialState() throws IOException
    {
        return new SubsystemApplicationContextManagerState(this.defaultChain, this.defaultTypeName);
    }

    /**
     *
     * This class exists primarily because the default {@link ApplicationContextManagerState} simply cannot be extended in any way due to
     * excessive use of private visibility. Most of its implementation had to be copied verbatim from the base class but due to instance-of
     * requirements it was impossible to drop the class inheritance hierarchy.
     *
     * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
     */
    protected class SubsystemApplicationContextManagerState extends ApplicationContextManagerState
    {

        private static final String PROPERTY_CHAIN = "chain";

        /** The instance ids. */
        protected final List<String> instanceIds = new ArrayList<>(10);

        /** The child application contexts. */
        protected final Map<String, SubsystemChildApplicationContextFactory> childApplicationContexts = new TreeMap<>();

        /** The default type name. */
        protected final String defaultTypeName;

        protected SubsystemApplicationContextManagerState(final String defaultChain, final String defaultTypeName)
        {
            super(defaultChain, defaultTypeName);

            // Work out what the default type name should be; either specified explicitly or implied by the first member
            // of the default chain
            if (SubsystemChildApplicationContextManager.this.defaultChain != null
                    && SubsystemChildApplicationContextManager.this.defaultChain.length() > 0)
            {
                // Use the first type as the default, unless one is specified explicitly
                if (defaultTypeName == null)
                {
                    this.updateChain(defaultChain, AbstractPropertyBackedBean.DEFAULT_INSTANCE_NAME);
                    this.defaultTypeName = this.childApplicationContexts.get(this.instanceIds.get(0)).getTypeName();
                }
                else
                {
                    this.defaultTypeName = defaultTypeName;
                    this.updateChain(defaultChain, defaultTypeName);
                }
            }
            else if (defaultTypeName == null)
            {
                this.defaultTypeName = AbstractPropertyBackedBean.DEFAULT_INSTANCE_NAME;
            }
            else
            {
                this.defaultTypeName = defaultTypeName;
            }
        }

        @Override
        public String getProperty(final String name)
        {
            if (!name.equals(PROPERTY_CHAIN))
            {
                return null;
            }
            return this.getChainString();
        }

        @Override
        public void setProperty(final String name, final String value)
        {
            super.setProperty(name, value);

            if (name.equals(PROPERTY_CHAIN))
            {
                this.updateChain(value, this.defaultTypeName);
            }
        }

        @Override
        public ApplicationContext getApplicationContext(final String id)
        {
            final SubsystemChildApplicationContextFactory child = this.childApplicationContexts.get(id);
            return child == null ? null : child.getApplicationContext();
        }

        /**
         *
         * {@inheritDoc}
         */
        @Override
        protected SubsystemChildApplicationContextFactory getApplicationContextFactory(final String id)
        {
            return this.childApplicationContexts.get(id);
        }

        protected String getChainString()
        {
            final StringBuilder orderString = new StringBuilder(100);
            for (final String id : new ArrayList<>(this.instanceIds))
            {
                if (orderString.length() > 0)
                {
                    orderString.append(",");
                }
                orderString.append(id).append(':').append(this.childApplicationContexts.get(id).getTypeName());
            }
            return orderString.toString();
        }

        protected void updateChain(final String orderString, final String defaultTypeName)
        {
            try
            {
                final StringTokenizer tkn = new StringTokenizer(orderString, ", \t\n\r\f");
                final List<String> newInstanceIds = new ArrayList<>(tkn.countTokens());
                while (tkn.hasMoreTokens())
                {
                    final String instance = tkn.nextToken();
                    final int sepIndex = instance.indexOf(':');
                    final String id = sepIndex == -1 ? instance : instance.substring(0, sepIndex);
                    final String typeName = sepIndex == -1 || sepIndex + 1 >= instance.length() ? defaultTypeName
                            : instance.substring(sepIndex + 1);
                    newInstanceIds.add(id);

                    // Look out for new or updated children
                    SubsystemChildApplicationContextFactory factory = this.childApplicationContexts.get(id);

                    // If we have the same instance ID but a different type, treat that as a destroy and remove
                    if (factory != null && !factory.getTypeName().equals(typeName))
                    {
                        factory.lockWrite();
                        ;
                        try
                        {
                            factory.destroy(true);
                        }
                        finally
                        {
                            factory.unlockWrite();
                        }
                        factory = null;
                    }
                    if (factory == null)
                    {
                        // Generate a unique ID within the category
                        final List<String> childId = new ArrayList<>(2);
                        childId.add("managed");
                        childId.add(id);
                        this.childApplicationContexts.put(id,
                                new SubsystemChildApplicationContextFactory(SubsystemChildApplicationContextManager.this.getParent(),
                                        SubsystemChildApplicationContextManager.this.getRegistry(),
                                        SubsystemChildApplicationContextManager.this.getPropertyDefaults(),
                                        SubsystemChildApplicationContextManager.this.getCategory(), typeName, childId));
                    }
                }

                // Destroy any children that have been removed
                final Set<String> idsToRemove = new TreeSet<>(this.childApplicationContexts.keySet());
                idsToRemove.removeAll(newInstanceIds);
                for (final String id : idsToRemove)
                {
                    final SubsystemChildApplicationContextFactory factory = this.childApplicationContexts.remove(id);
                    factory.lockWrite();
                    try
                    {
                        factory.destroy(true);
                    }
                    finally
                    {
                        factory.unlockWrite();
                    }
                }
                this.instanceIds.clear();
                this.instanceIds.addAll(newInstanceIds);
            }
            catch (final RuntimeException e)
            {
                throw e;
            }
            catch (final Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }
}
