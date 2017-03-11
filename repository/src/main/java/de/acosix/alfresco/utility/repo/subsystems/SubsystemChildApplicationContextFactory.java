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
import java.util.List;
import java.util.Properties;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.management.subsystems.ChildApplicationContextFactory;
import org.alfresco.repo.management.subsystems.PropertyBackedBeanRegistry;
import org.alfresco.repo.management.subsystems.PropertyBackedBeanState;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

/**
 * This class provides a slightly enhanced variant of the Alfresco default {@link ChildApplicationContextFactory} which allows interested
 * client code to obtain the effective {@code *.properties} files used to define the configuration of a subsystem. It can be used as a
 * drop-in replacement to the original class in all out-of-the-box use cases.
 *
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class SubsystemChildApplicationContextFactory extends ChildApplicationContextFactory implements SingleInstanceSubsystemHandler
{

    public SubsystemChildApplicationContextFactory()
    {
        super();
    }

    public SubsystemChildApplicationContextFactory(final ApplicationContext parent, final PropertyBackedBeanRegistry registry,
            final Properties propertyDefaults, final String category, final String typeName, final List<String> instancePath)
            throws IOException
    {
        super(parent, registry, propertyDefaults, category, typeName, instancePath);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Resource[] getSubsystemDefaultPropertiesResources()
    {
        try
        {
            final String category = this.getCategory();
            final String type = this.getTypeName();

            final String defaultPropertiesPattern = SubsystemWithClassLoaderState.CLASSPATH_ALFRESCO_SUBSYSTEMS + category
                    + SubsystemWithClassLoaderState.CLASSPATH_DELIMITER + type + +SubsystemWithClassLoaderState.CLASSPATH_DELIMITER
                    + SubsystemWithClassLoaderState.PROPERTIES_FILE_PATTERN;
            final Resource[] resources = this.getParent().getResources(defaultPropertiesPattern);
            return resources;
        }
        catch (final IOException ioex)
        {
            throw new AlfrescoRuntimeException("Error loading resources", ioex);
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Resource[] getSubsystemExtensionPropertiesResources()
    {
        try
        {
            final List<String> idList = this.getId();
            final String category = this.getCategory();
            final String type = this.getTypeName();
            final String id = idList.get(idList.size() - 1);

            final String extensionPropertiesPattern = SubsystemWithClassLoaderState.CLASSPATH_ALFRESCO_EXTENSION_SUBSYSTEMS + category
                    + SubsystemWithClassLoaderState.CLASSPATH_DELIMITER + type + SubsystemWithClassLoaderState.CLASSPATH_DELIMITER + id
                    + SubsystemWithClassLoaderState.CLASSPATH_DELIMITER + SubsystemWithClassLoaderState.PROPERTIES_FILE_PATTERN;
            final Resource[] resources = this.getParent().getResources(extensionPropertiesPattern);
            return resources;
        }
        catch (final IOException ioex)
        {
            throw new AlfrescoRuntimeException("Error loading resources", ioex);
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Properties getSubsystemEffectiveProperties()
    {
        this.lock.readLock().lock();
        try
        {
            final Properties effectiveProperties = new Properties();

            for (final String propertyName : this.getPropertyNames())
            {
                final String propertyValue = this.getProperty(propertyName);
                effectiveProperties.put(propertyName, propertyValue);
            }

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
        return new SubsystemApplicationContextState(true);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void destroy(final boolean permanent)
    {
        super.destroy(permanent);
    }

    protected void lockWrite()
    {
        this.lock.writeLock().lock();
    }

    protected void unlockWrite()
    {
        this.lock.writeLock().unlock();
    }

    /**
     * This class only serves to allow the enclosing class to instantiate an application context state with a different value for
     * {@code allowInitAccess}.
     *
     * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
     */
    protected class SubsystemApplicationContextState extends ApplicationContextState
    {

        public SubsystemApplicationContextState(final boolean allowInitAccess) throws IOException
        {
            super(allowInitAccess);
        }

    }
}
