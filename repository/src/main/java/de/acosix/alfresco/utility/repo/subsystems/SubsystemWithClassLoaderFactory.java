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
import java.util.List;
import java.util.Properties;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.management.subsystems.AbstractPropertyBackedBean;
import org.alfresco.repo.management.subsystems.PropertyBackedBeanState;
import org.alfresco.repo.management.subsystems.PropertyBackedBeanWithMonitor;
import org.alfresco.util.ParameterCheck;
import org.alfresco.util.PropertyCheck;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.util.DefaultPropertiesPersister;
import org.springframework.util.PropertiesPersister;

/**
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class SubsystemWithClassLoaderFactory extends AbstractPropertyBackedBean
        implements SingleInstanceSubsystemHandler, PropertyBackedBeanWithMonitor
{

    protected String typeName;

    protected PropertiesPersister persister = new DefaultPropertiesPersister();

    // field in super is inaccessible and getter is only introduced in 5.2
    protected Properties encryptedPropertyDefaults;

    /**
     * Sets the type name.
     *
     * @param typeName
     *            the typeName to set
     */
    public void setTypeName(final String typeName)
    {
        this.typeName = typeName;
    }

    /**
     * @return the typeName
     */
    public String getTypeName()
    {
        return this.typeName;
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
    public void afterPropertiesSet() throws Exception
    {
        PropertyCheck.mandatory(this, "encryptedPropertyDefaults", this.encryptedPropertyDefaults);

        final List<String> idList = this.getInstancePath();
        if (idList.isEmpty())
        {
            throw new IllegalStateException("Invalid instance path");
        }
        if (this.typeName == null)
        {
            this.setTypeName(idList.get(0));
        }

        super.afterPropertiesSet();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isUpdateable(final String name)
    {
        final boolean isUpdateable = !SubsystemWithClassLoaderState.NOT_UPDATEABLE_PROPERTIES.contains(name);
        return isUpdateable;
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
            case SubsystemWithClassLoaderState.PROPERTY_CATEGORY:
                result = "Read-only subsystem category";
                break;
            case SubsystemWithClassLoaderState.PROPERTY_TYPE:
                result = "Read-only subsystem type name";
                break;
            case SubsystemWithClassLoaderState.PROPERTY_ID:
                result = "Read-only subsystem instance id";
                break;
            case SubsystemWithClassLoaderState.PROPERTY_INSTANCE_PATH:
                result = "Read-only instance path";
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
    public ApplicationContext getApplicationContext()
    {
        this.lock.readLock().lock();
        try
        {
            return ((SubsystemWithClassLoaderState) this.getState(true)).getApplicationContext();
        }
        finally
        {
            this.lock.readLock().unlock();
        }
    }

    /**
     * Gets the application context. Will not start a subsystem.
     *
     * @return the application context or null
     */
    public ApplicationContext getReadOnlyApplicationContext()
    {
        this.lock.readLock().lock();
        try
        {
            return ((SubsystemWithClassLoaderState) this.getState(false)).getApplicationContext();
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
    public Object getMonitorObject()
    {
        this.lock.readLock().lock();
        try
        {
            return ((SubsystemWithClassLoaderState) this.getState(false)).getMonitor();
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
     *
     * {@inheritDoc}
     */
    @Override
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
    public void destroy(final boolean permanent)
    {
        this.lock.writeLock().lock();
        try
        {
            super.destroy(permanent);
        }
        finally
        {
            this.lock.writeLock().unlock();
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected PropertyBackedBeanState createInitialState() throws IOException
    {
        final List<String> idList = this.getId();
        final String id = idList.get(idList.size() - 1);
        final String instancePath = this.getInstancePath().toString();

        final Properties globalProperties = new Properties();
        globalProperties.putAll(this.getPropertyDefaults());
        globalProperties.putAll(this.encryptedPropertyDefaults);

        final SubsystemWithClassLoaderState state = new SubsystemWithClassLoaderState(this.getParent(), globalProperties, this.persister,
                this.getCategory(), this.getTypeName(), id, instancePath);
        return state;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void applyDefaultOverrides(final PropertyBackedBeanState state) throws IOException
    {
        // NO-OP
        // no need to apply default overrides - SubsystemWithClassLoaderState is already able to do just that
    }

}
