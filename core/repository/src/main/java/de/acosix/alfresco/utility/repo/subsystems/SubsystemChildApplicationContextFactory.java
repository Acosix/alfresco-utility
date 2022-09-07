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
package de.acosix.alfresco.utility.repo.subsystems;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.management.subsystems.ChildApplicationContextFactory;
import org.alfresco.repo.management.subsystems.PropertyBackedBeanRegistry;
import org.alfresco.repo.management.subsystems.PropertyBackedBeanState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

/**
 * This class provides a slightly enhanced variant of the Alfresco default {@link ChildApplicationContextFactory} which allows interested
 * client code to obtain the effective {@code *.properties} files used to define the configuration of a subsystem. It can be used as a
 * drop-in replacement to the original class in all out-of-the-box use cases.
 *
 * @author Axel Faust
 */
public class SubsystemChildApplicationContextFactory extends ChildApplicationContextFactory
        implements SingleInstanceSubsystemHandler, SubsystemConstants
{

    private static final Logger LOGGER = LoggerFactory.getLogger(SubsystemChildApplicationContextFactory.class);

    public SubsystemChildApplicationContextFactory()
    {
        super();
    }

    public SubsystemChildApplicationContextFactory(final ApplicationContext parent, final PropertyBackedBeanRegistry registry,
            final Properties propertyDefaults, final Properties encryptedPropertyDefaults, final String category, final String typeName,
            final List<String> instancePath) throws IOException
    {
        // due to API incompatibilities between 5.x and 6.x we can't call super constructor
        super();

        this.setApplicationContext(parent);
        this.setRegistry(registry);
        this.setPropertyDefaults(propertyDefaults);
        this.setEncryptedPropertyDefaults(encryptedPropertyDefaults);
        this.setCategory(category);
        this.setTypeName(typeName);
        this.setInstancePath(instancePath);

        try
        {
            this.afterPropertiesSet();
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

            final String defaultPropertiesPattern = CLASSPATH_ALFRESCO_SUBSYSTEMS + category + CLASSPATH_DELIMITER + type
                    + CLASSPATH_DELIMITER + PROPERTIES_FILE_PATTERN;
            final Resource[] resources = this.getParent().getResources(defaultPropertiesPattern);
            LOGGER.debug("Resolved default properties files for {}: {}", this, Arrays.asList(resources));
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

            final String extensionPropertiesPattern = CLASSPATH_ALFRESCO_EXTENSION_SUBSYSTEMS + category + CLASSPATH_DELIMITER + type
                    + CLASSPATH_DELIMITER + id + CLASSPATH_DELIMITER + PROPERTIES_FILE_PATTERN;
            final Resource[] resources = this.getParent().getResources(extensionPropertiesPattern);
            LOGGER.debug("Resolved extension properties files for {}: {}", this, Arrays.asList(resources));
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

            LOGGER.debug("Constructed effective properties for {}: {}", this, effectiveProperties);

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
    public ApplicationContext getReadOnlyApplicationContext()
    {
        this.lock.readLock().lock();
        try
        {
            return ((ApplicationContextState) this.getState(false)).getReadOnlyApplicationContext();
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
    public String toString()
    {
        // base class does not offer a proper toString
        final StringBuilder builder = new StringBuilder();
        builder.append(this.getCategory()).append(" subsystem (ID: ").append(this.getId()).append(')');
        final String result = builder.toString();
        return result;
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
    protected void applyDefaultOverrides(final PropertyBackedBeanState state) throws IOException
    {
        final String propertyNamePatterns = state.getProperty(SUBSYSTEM_PROPERTY_NAME_PATTERNS);
        if (propertyNamePatterns != null && !propertyNamePatterns.trim().isEmpty())
        {
            state.removeProperty(SUBSYSTEM_PROPERTY_NAME_PATTERNS);

            // map in any system + global properties matching defined patterns
            // this deals with dynamically defined properties without any predefines in the subsystem default
            final Set<String> systemAndDefaultPropertyNames = new HashSet<>(this.getPropertyDefaults().stringPropertyNames());
            systemAndDefaultPropertyNames.addAll(this.getEncryptedPropertyDefaults().stringPropertyNames());
            systemAndDefaultPropertyNames.addAll(System.getProperties().stringPropertyNames());

            final String[] patterns = propertyNamePatterns.trim().split(",");
            for (final String pattern : patterns)
            {
                final Pattern rpattern = Pattern.compile(pattern);
                systemAndDefaultPropertyNames.stream().filter(pn -> rpattern.matcher(pn).matches()).forEach(pn -> {
                    if (state.getProperty(pn) == null)
                    {
                        state.setProperty(pn, "");
                    }
                });
            }
        }

        super.applyDefaultOverrides(state);
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
     * @author Axel Faust
     */
    protected class SubsystemApplicationContextState extends ApplicationContextState
    {

        public SubsystemApplicationContextState(final boolean allowInitAccess) throws IOException
        {
            super(allowInitAccess);
        }
    }
}
