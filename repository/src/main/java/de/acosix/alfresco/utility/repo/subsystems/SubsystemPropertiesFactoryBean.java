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

import java.util.Properties;

import org.alfresco.repo.management.subsystems.ChildApplicationContextFactory;
import org.alfresco.repo.management.subsystems.ChildApplicationContextManager;
import org.alfresco.repo.management.subsystems.PropertyBackedBean;
import org.alfresco.util.PropertyCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderSupport;

/**
 * Instances of this class allow constructing property beans for the current subsystem based on exposed paths for {@code *.properties} files
 * for subsystems via one of the following methods:
 * <ul>
 * <li>{@link SingleInstanceSubsystemHandler#getSubsystemDefaultPropertiesResources() simple subsystem default properties}</li>
 * <li>{@link SingleInstanceSubsystemHandler#getSubsystemExtensionPropertiesResources() simple subsystem extension properties}</li>
 * <li>{@link MultiInstanceSubsystemHandler#getSubsystemDefaultPropertiesResources(String) multi-instance subsystem default
 * properties}</li>
 * <li>{@link MultiInstanceSubsystemHandler#getSubsystemExtensionPropertiesResources(String) multi-instance subsystem extension
 * properties}</li>
 * </ul>
 *
 * Instances of this class require the {@link ApplicationContextAware currently active application context} and either the
 * {@link #setSubsystemChildApplicationContextManager(ChildApplicationContextManager) subsystem child application context manager} and
 * {@link #setSubsystemChildApplicationContextFactory(ChildApplicationContextFactory) subsystem child application context factory}
 * responsible for managing the subsystem.
 *
 * This class does not incorporate any properties set at runtime via the {@link PropertyBackedBean} API.
 *
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class SubsystemPropertiesFactoryBean extends PropertiesLoaderSupport
        implements FactoryBean<Properties>, ApplicationContextAware, InitializingBean
{

    private static final Logger LOGGER = LoggerFactory.getLogger(SubsystemPropertiesFactoryBean.class);

    protected ApplicationContext applicationContext;

    protected ChildApplicationContextManager subsystemChildApplicationContextManager;

    protected ChildApplicationContextFactory subsystemChildApplicationContextFactory;

    protected boolean extensionProperties;

    /**
     * {@inheritDoc}
     */
    @Override
    public void setApplicationContext(final ApplicationContext applicationContext)
    {
        this.applicationContext = applicationContext;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        final Resource[] locations;
        if (this.subsystemChildApplicationContextFactory == null)
        {
            PropertyCheck.mandatory(this, "childApplicationContextManager", this.subsystemChildApplicationContextManager);

            if (this.subsystemChildApplicationContextManager instanceof MultiInstanceSubsystemHandler)
            {
                final String instanceId = ((MultiInstanceSubsystemHandler) this.subsystemChildApplicationContextManager)
                        .determineInstanceId(this.applicationContext);
                if (instanceId == null)
                {
                    throw new IllegalStateException("applicationContext is active but subsystem instance could not be determiend");
                }

                if (this.extensionProperties)
                {
                    locations = ((MultiInstanceSubsystemHandler) this.subsystemChildApplicationContextManager)
                            .getSubsystemExtensionPropertiesResources(instanceId);
                }
                else
                {
                    locations = ((MultiInstanceSubsystemHandler) this.subsystemChildApplicationContextManager)
                            .getSubsystemDefaultPropertiesResources(instanceId);
                }
            }
            else
            {
                locations = new Resource[0];
                LOGGER.warn(
                        "subsystemChildApplicationContextManager does not conform to the interface MultiInstanceSubsystemHandler - either subsystem has not been configured properly or original Alfresco bean may not have been enhanced");
            }
        }
        else
        {
            if (this.subsystemChildApplicationContextFactory instanceof SingleInstanceSubsystemHandler)
            {
                if (this.extensionProperties)
                {
                    locations = ((SingleInstanceSubsystemHandler) this.subsystemChildApplicationContextFactory)
                            .getSubsystemExtensionPropertiesResources();
                }
                else
                {
                    locations = ((SingleInstanceSubsystemHandler) this.subsystemChildApplicationContextFactory)
                            .getSubsystemDefaultPropertiesResources();
                }
            }
            else
            {
                locations = new Resource[0];
                LOGGER.warn(
                        "subsystemChildApplicationContextFactory does not conform to the interface SingleInstanceSubsystemHandler - either subsystem has not been configured properly or original Alfresco bean may not have been enhanced");
            }
        }
        this.setLocations(locations);
    }

    /**
     * @param subsystemChildApplicationContextManager
     *            the subsystemChildApplicationContextManager to set
     */
    public void setSubsystemChildApplicationContextManager(final ChildApplicationContextManager subsystemChildApplicationContextManager)
    {
        this.subsystemChildApplicationContextManager = subsystemChildApplicationContextManager;
    }

    /**
     * @param subsystemChildApplicationContextFactory
     *            the subsystemChildApplicationContextFactory to set
     */
    public void setSubsystemChildApplicationContextFactory(final ChildApplicationContextFactory subsystemChildApplicationContextFactory)
    {
        this.subsystemChildApplicationContextFactory = subsystemChildApplicationContextFactory;
    }

    /**
     * @param extensionProperties
     *            the extensionProperties to set
     */
    public void setExtensionProperties(final boolean extensionProperties)
    {
        this.extensionProperties = extensionProperties;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Properties getObject() throws Exception
    {
        return this.mergeProperties();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Class<?> getObjectType()
    {
        return Properties.class;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isSingleton()
    {
        return true;
    }

}
