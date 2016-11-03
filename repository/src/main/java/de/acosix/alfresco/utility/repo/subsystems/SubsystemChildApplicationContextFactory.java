/*
 * Copyright 2016 Acosix GmbH
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
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class SubsystemChildApplicationContextFactory extends ChildApplicationContextFactory
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
     * Looks up the default subsystem configuration properties files for the this subsystem instance.
     *
     * @return the list of default configuration properties files
     */
    public Resource[] getSubsystemDefaultPropertiesResources()
    {
        try
        {
            final StringBuilder propertiesLocationBuilder = new StringBuilder();
            propertiesLocationBuilder.append("classpath*:alfresco");
            propertiesLocationBuilder.append("/subsystems/");
            propertiesLocationBuilder.append(this.getCategory());
            propertiesLocationBuilder.append('/');
            propertiesLocationBuilder.append(this.getTypeName());
            propertiesLocationBuilder.append("/*.properties");

            final String defaultPropertiesPattern = propertiesLocationBuilder.toString();
            final Resource[] resources = this.getParent().getResources(defaultPropertiesPattern);
            return resources;
        }
        catch (final IOException ioex)
        {
            throw new AlfrescoRuntimeException("Error loading resources", ioex);
        }
    }

    /**
     * Looks up the extension subsystem configuration properties files for the this subsystem instance.
     *
     * @return the list of extension configuration properties files
     */
    public Resource[] getSubsystemExtensionPropertiesResources()
    {
        try
        {
            final List<String> idList = this.getId();

            final StringBuilder propertiesLocationBuilder = new StringBuilder();
            propertiesLocationBuilder.append("classpath*:alfresco");
            propertiesLocationBuilder.append("/extension");
            propertiesLocationBuilder.append("/subsystems/");
            propertiesLocationBuilder.append(this.getCategory());
            propertiesLocationBuilder.append('/');
            propertiesLocationBuilder.append(this.getTypeName());
            propertiesLocationBuilder.append("/");
            propertiesLocationBuilder.append(idList.get(idList.size() - 1));
            propertiesLocationBuilder.append("/*.properties");

            final String extensionPropertiesPattern = propertiesLocationBuilder.toString();
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
