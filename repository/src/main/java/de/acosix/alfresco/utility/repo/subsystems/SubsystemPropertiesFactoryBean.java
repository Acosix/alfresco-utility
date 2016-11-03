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

import java.util.Properties;

import org.alfresco.util.PropertyCheck;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderSupport;

/**
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class SubsystemPropertiesFactoryBean extends PropertiesLoaderSupport
        implements FactoryBean<Properties>, ApplicationContextAware, InitializingBean
{

    protected ApplicationContext applicationContext;

    protected SubsystemChildApplicationContextManager subsystemChildApplicationContextManager;

    protected SubsystemChildApplicationContextFactory subsystemChildApplicationContextFactory;

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

            final String instanceId = this.subsystemChildApplicationContextManager.determineInstanceId(this.applicationContext);
            if (instanceId == null)
            {
                throw new IllegalStateException("applicationContext is active but subsystem instance could not be determiend");
            }

            if (this.extensionProperties)
            {
                locations = this.subsystemChildApplicationContextManager.getSubsystemExtensionPropertiesResources(instanceId);
            }
            else
            {
                locations = this.subsystemChildApplicationContextManager.getSubsystemDefaultPropertiesResources(instanceId);
            }
        }
        else
        {
            if (this.extensionProperties)
            {
                locations = this.subsystemChildApplicationContextFactory.getSubsystemExtensionPropertiesResources();
            }
            else
            {
                locations = this.subsystemChildApplicationContextFactory.getSubsystemDefaultPropertiesResources();
            }
        }
        this.setLocations(locations);
    }

    /**
     * @param subsystemChildApplicationContextManager
     *            the subsystemChildApplicationContextManager to set
     */
    public void setSubsystemChildApplicationContextManager(
            final SubsystemChildApplicationContextManager subsystemChildApplicationContextManager)
    {
        this.subsystemChildApplicationContextManager = subsystemChildApplicationContextManager;
    }

    /**
     * @param subsystemChildApplicationContextFactory
     *            the subsystemChildApplicationContextFactory to set
     */
    public void setSubsystemChildApplicationContextFactory(
            final SubsystemChildApplicationContextFactory subsystemChildApplicationContextFactory)
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
