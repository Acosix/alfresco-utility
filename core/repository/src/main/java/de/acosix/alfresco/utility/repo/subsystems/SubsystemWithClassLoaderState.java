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
package de.acosix.alfresco.utility.repo.subsystems;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.alfresco.config.JndiPropertiesFactoryBean;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.management.subsystems.PropertyBackedBeanState;
import org.alfresco.util.ParameterCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.Resource;
import org.springframework.util.PropertiesPersister;
import org.springframework.util.PropertyPlaceholderHelper;

/**
 * @author Axel Faust
 */
@SuppressWarnings({ "deprecation", "removal" })
public class SubsystemWithClassLoaderState implements PropertyBackedBeanState, SubsystemConstants
{

    private static final Logger LOGGER = LoggerFactory.getLogger(SubsystemWithClassLoaderState.class);

    protected final ApplicationContext parentContext;

    protected final Properties globalProperties;

    protected final PropertiesPersister propertiesPersister;

    protected final Properties runtimeProperties = new Properties();

    protected final String category;

    protected final String type;

    protected final String id;

    protected final String instancePath;

    protected transient Properties fixedConfigProperties;

    protected transient ClassPathXmlApplicationContext applicationContext;

    protected transient RuntimeException lastStartupError;

    protected transient Object monitor;

    public SubsystemWithClassLoaderState(final ApplicationContext parentContext, final Properties globalProperties,
            final PropertiesPersister propertiesPersister, final String category, final String type, final String id,
            final String instancePath)
    {
        ParameterCheck.mandatory("parentContext", parentContext);
        ParameterCheck.mandatory("globalProperties", globalProperties);
        ParameterCheck.mandatory("propertiesPersister", propertiesPersister);

        ParameterCheck.mandatoryString("category", category);
        ParameterCheck.mandatoryString("type", type);
        ParameterCheck.mandatoryString("id", id);
        ParameterCheck.mandatoryString("instancePath", instancePath);

        this.parentContext = parentContext;
        this.globalProperties = globalProperties;
        this.propertiesPersister = propertiesPersister;

        this.category = category;
        this.type = type;
        this.id = id;
        this.instancePath = instancePath;

        this.loadFixedConfigProperties();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void start()
    {
        if (this.applicationContext == null)
        {
            if (this.lastStartupError != null)
            {
                throw this.lastStartupError;
            }

            try
            {
                // support re-load of classpath configurations (this specifically aims for extension configuration)
                this.loadFixedConfigProperties();

                // TODO Support composites same as Alfresco subsystems do
                this.applicationContext = new ClassPathXmlApplicationContext(new String[] { BASE_SUBSYSTEM_CONTEXT }, false,
                        this.parentContext)
                {

                    /**
                     *
                     * {@inheritDoc}
                     */
                    @Override
                    public void publishEvent(final ApplicationEvent event)
                    {
                        ParameterCheck.mandatory("event", event);
                        if (this.logger.isTraceEnabled())
                        {
                            this.logger.trace("Publishing event in " + this.getDisplayName() + ": " + event);
                        }

                        ((ApplicationEventMulticaster) this.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME)).multicastEvent(event);

                        final ApplicationContext parent = this.getParent();
                        if (parent != null && !(event instanceof ContextRefreshedEvent || event instanceof ContextClosedEvent))
                        {
                            parent.publishEvent(event);
                        }
                    }

                    /**
                     *
                     * {@inheritDoc}
                     */
                    @Override
                    protected void prepareBeanFactory(final ConfigurableListableBeanFactory beanFactory)
                    {
                        super.prepareBeanFactory(beanFactory);

                        final Properties subsystemProperties = SubsystemWithClassLoaderState.this.populateSubsystemBeanFactory(beanFactory);

                        final PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
                        if (subsystemProperties != null)
                        {
                            configurer.setPropertiesArray(SubsystemWithClassLoaderState.this.globalProperties, subsystemProperties);
                        }
                        else
                        {
                            configurer.setProperties(SubsystemWithClassLoaderState.this.globalProperties);
                        }
                        configurer.setIgnoreUnresolvablePlaceholders(true);
                        configurer.setSearchSystemEnvironment(false);
                        this.addBeanFactoryPostProcessor(configurer);
                    }
                };

                final Properties instanceIdProps = new Properties();
                instanceIdProps.put(PROPERTY_CATEGORY, this.category);
                instanceIdProps.put(PROPERTY_TYPE, this.type);
                instanceIdProps.put(PROPERTY_ID, this.id);
                instanceIdProps.put(PROPERTY_INSTANCE_PATH, this.instancePath);
                this.applicationContext.getEnvironment().getPropertySources()
                        .addLast(new PropertiesPropertySource("instanceIdProps", instanceIdProps));

                // build custom loader
                final List<URL> classLoaderURLs = this.buildClassLoaderURLs();
                final ClassLoader classLoader = AccessController.<ClassLoader> doPrivileged(
                        (PrivilegedAction<ClassLoader>) () -> new SubsystemClassLoader(this.parentContext.getClassLoader(),
                                classLoaderURLs));
                this.applicationContext.setClassLoader(classLoader);

                // for initialisation of the context (incl singletons) we should set the proper class loader as the context
                // some API (i.e. java.util.ServiceLoader) use that instead of applicationContext class loader
                final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(classLoader);
                try
                {
                    this.applicationContext.refresh();
                }
                finally
                {
                    Thread.currentThread().setContextClassLoader(contextClassLoader);
                }

                LOGGER.info("Startup of '{}' subsystem, ID: {} complete", this.category, this.id);

                if (this.applicationContext.containsBean(BEAN_NAME_MONITOR))
                {
                    final Object m = this.applicationContext.getBean(BEAN_NAME_MONITOR);
                    LOGGER.debug("Got a monitor object {} for '{}' subsystem, ID: {}", m, this.category, this.id);
                    this.monitor = m;
                }
            }
            catch (final RuntimeException e)
            {
                LOGGER.warn("Startup of '{}' subsystem, ID: {} failed", this.category, this.id, e);
                this.applicationContext = null;
                this.lastStartupError = e;
                throw e;
            }
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void stop()
    {
        if (this.applicationContext != null)
        {
            LOGGER.info("Stopping '{}' subsystem, ID: {}", this.category, this.id);
            try
            {
                this.applicationContext.close();
            }
            catch (final Exception e)
            {
                LOGGER.warn("Error stopping subsystem application context", e);
                // Continue anyway. Perhaps it didn't start properly
            }

            this.applicationContext = null;
            this.monitor = null;

            LOGGER.info("Stopped '{}' subsystem, ID: {}", this.category, this.id);
        }
    }

    /**
     * Gets the application context. Will not start a subsystem.
     *
     * @return the application context or null
     */
    public ApplicationContext getReadOnlyApplicationContext()
    {
        return this.applicationContext;
    }

    /**
     * Gets the application context.
     *
     * @return the application context
     */
    public ApplicationContext getApplicationContext()
    {
        return this.getApplicationContext(true);
    }

    /**
     * Gets the application context.
     *
     * @param start
     *     indicates whether state should be started
     *
     * @return the application context or <code>null</code> if state was not already started and start == false
     */
    public ApplicationContext getApplicationContext(final boolean start)
    {
        if (start)
        {
            this.start();
        }
        return this.applicationContext;
    }

    /**
     * @return the monitor
     */
    public Object getMonitor()
    {
        return this.monitor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Set<String> getPropertyNames()
    {
        final Set<String> propertyNames = new TreeSet<>();

        propertyNames.addAll(((Map) this.fixedConfigProperties).keySet());
        propertyNames.addAll(NON_UPDATEABLE_PROPERTIES);
        propertyNames.addAll(((Map) this.runtimeProperties).keySet());

        return propertyNames;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProperty(final String name)
    {
        ParameterCheck.mandatoryString("name", name);

        String value;
        switch (name)
        {
            case PROPERTY_CATEGORY:
                value = this.category;
                break;
            case PROPERTY_TYPE:
                value = this.type;
                break;
            case PROPERTY_ID:
                value = this.id;
                break;
            case PROPERTY_INSTANCE_PATH:
                value = this.instancePath;
                break;
            default:
                if (this.runtimeProperties.containsKey(name))
                {
                    value = this.runtimeProperties.getProperty(name);
                }
                else
                {
                    value = this.fixedConfigProperties.getProperty(name);
                }
        }
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProperty(final String name, final String value)
    {
        ParameterCheck.mandatoryString("name", name);
        if (NON_UPDATEABLE_PROPERTIES.contains(name))
        {
            throw new IllegalStateException("Illegal write to property \"" + name + "\"");
        }

        this.lastStartupError = null;
        this.runtimeProperties.setProperty(name, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeProperty(final String name)
    {
        ParameterCheck.mandatoryString("name", name);
        if (NON_UPDATEABLE_PROPERTIES.contains(name))
        {
            throw new IllegalStateException("Illegal write to property \"" + name + "\"");
        }

        this.lastStartupError = null;
        this.runtimeProperties.remove(name);
    }

    protected void loadFixedConfigProperties()
    {
        try
        {
            // #1: load static base properties
            PropertiesFactoryBean factory = new PropertiesFactoryBean();
            factory.setPropertiesPersister(this.propertiesPersister);
            final Resource[] baseClasspathResources = this.parentContext.getResources(CLASSPATH_ALFRESCO_SUBSYSTEMS + this.category
                    + CLASSPATH_DELIMITER + this.type + CLASSPATH_DELIMITER + PROPERTIES_FILE_PATTERN);
            factory.setLocations(baseClasspathResources);
            factory.afterPropertiesSet();
            final Properties properties = factory.getObject();

            if (properties != null)
            {
                // #2: map in any system + global property names matching defined patterns
                // this deals with dynamically defined properties without any predefines in the subsystem default
                final String propertyNamePatterns = properties.getProperty(SUBSYSTEM_PROPERTY_NAME_PATTERNS);
                if (propertyNamePatterns != null && !propertyNamePatterns.trim().isEmpty())
                {
                    properties.remove(SUBSYSTEM_PROPERTY_NAME_PATTERNS);

                    final Set<String> systemAndDefaultPropertyNames = new HashSet<>(this.globalProperties.stringPropertyNames());
                    systemAndDefaultPropertyNames.addAll(System.getProperties().stringPropertyNames());

                    final String[] patterns = propertyNamePatterns.trim().split(",");
                    for (final String pattern : patterns)
                    {
                        final Pattern rpattern = Pattern.compile(pattern);
                        systemAndDefaultPropertyNames.stream().filter(np -> !properties.containsKey(np))
                                .filter(pn -> rpattern.matcher(pn).matches()).forEach(pn -> properties.setProperty(pn, ""));
                    }
                }

                // #3: check and resolve defaults from global properties
                final PropertyPlaceholderHelper resolver = new PropertyPlaceholderHelper("${", "}", ":", true);
                for (final String name : properties.stringPropertyNames())
                {
                    String value = this.globalProperties.getProperty(name);
                    if (value != null)
                    {
                        value = resolver.replacePlaceholders(value, this.globalProperties);
                        properties.setProperty(name, value);
                    }
                }
            }

            // #4: aggregate with static overrides from extensions + system properties
            factory = new JndiPropertiesFactoryBean();
            ((JndiPropertiesFactoryBean) factory).setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_OVERRIDE);
            if (properties != null)
            {
                factory.setProperties(properties);
            }
            factory.setPropertiesPersister(this.propertiesPersister);
            final Resource[] extensionClasspathResources = this.parentContext
                    .getResources(CLASSPATH_ALFRESCO_EXTENSION_SUBSYSTEMS + this.category + CLASSPATH_DELIMITER + this.type
                            + CLASSPATH_DELIMITER + this.id + CLASSPATH_DELIMITER + PROPERTIES_FILE_PATTERN);
            factory.setLocations(extensionClasspathResources);
            factory.afterPropertiesSet();
            this.fixedConfigProperties = factory.getObject();
        }
        catch (final IOException e)
        {
            LOGGER.error("Failed to load configuration properties from classpath resources for '{}' subsystem, ID: {}", this.category,
                    this.id, e);
            throw new AlfrescoRuntimeException("Failed to load subsystem configuration", e);
        }
    }

    protected Properties populateSubsystemBeanFactory(final ConfigurableListableBeanFactory beanFactory)
    {
        try
        {
            // build effective properties
            final PropertiesFactoryBean factory = new PropertiesFactoryBean();
            factory.setPropertiesArray(this.fixedConfigProperties, this.runtimeProperties);
            factory.afterPropertiesSet();
            final Properties subsystemProperties = factory.getObject();
            if (subsystemProperties != null)
            {
                beanFactory.registerSingleton(BEAN_NAME_SUBSYSTEM_PROPERTIES, subsystemProperties);
            }
            return subsystemProperties;
        }
        catch (final IOException e)
        {
            throw new AlfrescoRuntimeException("Error instantiating effective subsystem properties", e);
        }
    }

    protected List<URL> buildClassLoaderURLs()
    {
        /*
         * We need to identify all JAR / class files found within the subsystem context (on the current classpath) and derive the base URLs
         * for these so URLClassLoader can work with that. We also want there to be a sane priority order between the base URLs so that
         * class files may override JAR-bundled classes, and extension path may override default path.
         */

        final List<URL> allUrls = new ArrayList<>();

        // class folders in extension path have utmost priority
        allUrls.addAll(this.resolveClassesDirectoryURLs(
                CLASSPATH_ALFRESCO_EXTENSION_SUBSYSTEMS + this.category + CLASSPATH_DELIMITER + this.type + CLASSPATH_DELIMITER + this.id));

        // extension JARs have priority over anything in regular paths
        allUrls.addAll(this.resolveJarURLs(
                CLASSPATH_ALFRESCO_EXTENSION_SUBSYSTEMS + this.category + CLASSPATH_DELIMITER + this.type + CLASSPATH_DELIMITER + this.id));

        // class folders in regular path have priority over JARs
        allUrls.addAll(this.resolveClassesDirectoryURLs(CLASSPATH_ALFRESCO_SUBSYSTEMS + this.category + CLASSPATH_DELIMITER + this.type));

        // JARs in regular path have lowest priority
        allUrls.addAll(this.resolveJarURLs(CLASSPATH_ALFRESCO_SUBSYSTEMS + this.category + CLASSPATH_DELIMITER + this.type));

        LOGGER.debug("Resolved class loader URLs {} for '{}' subsystem, ID: {}", allUrls, this.category, this.id);

        return allUrls;
    }

    protected List<URL> resolveClassesDirectoryURLs(final String classpathBase)
    {
        final Set<URL> urls = new HashSet<>();
        try
        {
            final Resource[] simpleLookupResources = this.parentContext
                    .getResources(classpathBase + CLASSPATH_DELIMITER + CLASSES_FOLDER_NAME);
            for (final Resource resource : simpleLookupResources)
            {
                try
                {
                    final File file = resource.getFile();
                    urls.add(file.toURI().toURL());
                }
                catch (final IOException fileEx)
                {
                    LOGGER.debug("Failed to resolve resource {} to file for '{}' subsystem, ID: {}", resource.getDescription(),
                            this.category, this.id);
                }
            }
        }
        catch (final IOException ex)
        {
            throw new AlfrescoRuntimeException("Failed to resolve subsystem classes directory URL(s)");
        }

        return new ArrayList<>(urls);
    }

    protected List<URL> resolveJarURLs(final String classpathBase)
    {
        final Set<URL> urls = new HashSet<>();
        try
        {
            final Resource[] simpleLookupResources = this.parentContext
                    .getResources(classpathBase + CLASSPATH_DELIMITER + JAR_FILE_LOOKUP_PATTERN);
            if (simpleLookupResources != null)
            {
                for (final Resource resource : simpleLookupResources)
                {
                    try
                    {
                        final File file = resource.getFile();
                        urls.add(file.toURI().toURL());
                    }
                    catch (final IOException fileEx)
                    {
                        LOGGER.debug("Failed to resolve resource {} to JAR file for '{}' subsystem, ID: {}", resource.getDescription(),
                                this.category, this.id);
                    }
                }
            }
        }
        catch (final IOException ex)
        {
            throw new AlfrescoRuntimeException("Failed to resolve subsystem JAR file URL(s)");
        }

        return new ArrayList<>(urls);
    }
}
