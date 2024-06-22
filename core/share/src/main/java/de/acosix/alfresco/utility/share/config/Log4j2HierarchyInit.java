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
/*
 * Linked to Alfresco
 * Original file alfresco-repository/source/java/org/alfresco/repo/admin/Log4JHierarchyInit.java
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 */
package de.acosix.alfresco.utility.share.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.alfresco.util.ParameterCheck;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.properties.PropertiesConfiguration;
import org.apache.logging.log4j.core.config.properties.PropertiesConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * Initialises Log4j's logger repository. The actual implementation uses introspection to avoid any hard-coded references to Log4J classes.
 * If Log4J is not present, this class will do nothing.
 * <p>
 * Alfresco modules can provide their own log4j.properties file, which augments/overrides the global log4j.properties within the Alfresco
 * webapp. Within the module's source tree, suppose you create:
 *
 * <pre>
 *      config/alfresco/module/{module.id}/log4j2.properties
 * </pre>
 *
 * At deployment time, this log4j.properties file will be placed in:
 *
 * <pre>
 *      WEB-INF/classes/alfresco/module/{module.id}/log4j2.properties
 * </pre>
 *
 * Where {module.id} is whatever value is set within the AMP's module.properties file. For details, see: <a
 * href='http://wiki.alfresco.com/wiki/Developing_an_Alfresco_Module'>Developing an Alfresco Module</a>
 * <p>
 * For example, if {module.id} is "org.alfresco.module.someModule", then within your source code you'll have:
 *
 * <pre>
 * config/alfresco/module/org.alfresco.module.someModule/log4j2.properties
 * </pre>
 *
 * This would be deployed to:
 *
 * <pre>
 * WEB-INF/classes/alfresco/module/org.alfresco.module.someModule/log4j2.properties
 * </pre>
 *
 *
 * This class is a near-verbatim copy from the
 * <a href=
 * "https://github.com/Alfresco/alfresco-community-repo/blob/master/repository/src/main/java/org/alfresco/repo/admin/Log4JHierarchyInit.java">Repository-tier
 * class</a>.
 *
 * @author Axel Faust
 */
public class Log4j2HierarchyInit implements InitializingBean, ApplicationContextAware, BeanFactoryPostProcessor
{

    private static final Logger LOGGER = LoggerFactory.getLogger(Log4j2HierarchyInit.class);

    protected final List<String> extraLog4jUrls = new ArrayList<>();

    protected ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    /**
     * {@inheritDoc}
     */
    @Override
    public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) throws BeansException
    {
        // NO-OP - we only implement the interface to be instantiated as early as possible in Spring lifecycle
    }

    /**
     * Loads a set of augmenting/overriding log4j.properties files from locations specified via an array of Srping URLS.
     * <p>
     * This function supports Spring's syntax for retrieving multiple class path resources with the same name, via the "classpath&#042;:"
     * prefix. For details, see: {@link PathMatchingResourcePatternResolver}.
     *
     * @param urls
     *     the URLs to Log4J configuration files
     */
    public void setExtraLog4jUrls(final List<String> urls)
    {
        ParameterCheck.mandatory("urls", urls);
        this.extraLog4jUrls.addAll(urls);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException
    {
        this.resolver = applicationContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        this.importLogSettings();
    }

    protected void importLogSettings()
    {
        try
        {
            final Properties mainProperties = new Properties();

            this.importMainLogSettings(mainProperties);
            for (final String url : this.extraLog4jUrls)
            {
                this.importLogSettings(url, mainProperties);
            }

            final PropertiesConfiguration propertiesConfiguration = new PropertiesConfigurationBuilder().setConfigurationSource(null)
                    .setRootProperties(mainProperties).setLoggerContext((LoggerContext) LogManager.getContext(false)).build();

            propertiesConfiguration.initialize();
            ((LoggerContext) LogManager.getContext(false)).reconfigure(propertiesConfiguration);
        }
        catch (final Throwable t)
        {
            LOGGER.debug("Failed to add extra Logger configuration", t);
        }
    }

    private void importMainLogSettings(final Properties mainProperties) throws IOException
    {
        final File file = ((LoggerContext) LogManager.getContext()).getConfiguration().getConfigurationSource().getFile();
        if (file != null)
        {
            try (FileInputStream fis = new FileInputStream(file))
            {
                mainProperties.load(fis);
            }
            catch (final FileNotFoundException e)
            {
                LOGGER.debug("Failed to find initial configuration", e);
            }
        }
    }

    private void importLogSettings(final String springUrl, final Properties mainProperties)
    {
        Resource[] resources = null;

        try
        {
            resources = this.resolver.getResources(springUrl);
        }
        catch (final Exception e)
        {
            LOGGER.warn("Failed to find additional Logger configuration {}", springUrl);
            return;
        }

        // Read each resource
        for (final Resource resource : resources)
        {
            try
            {
                final InputStream inputStream = resource.getInputStream();
                final Properties properties = new Properties();
                properties.load(inputStream);
                mainProperties.putAll(properties);
            }
            catch (final Throwable e)
            {
                LOGGER.debug("Failed to add extra Logger configuration", e);
            }
        }
    }
}
