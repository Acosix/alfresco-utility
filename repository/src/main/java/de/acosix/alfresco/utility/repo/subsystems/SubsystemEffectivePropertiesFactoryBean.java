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

import java.util.Properties;

import org.alfresco.repo.management.subsystems.ChildApplicationContextFactory;
import org.alfresco.repo.management.subsystems.ChildApplicationContextManager;
import org.alfresco.util.PropertyCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Instances of this class allow constructing property beans for the current subsystem based on effective configuration properties exposed
 * via {@link SingleInstanceSubsystemHandler#getSubsystemEffectiveProperties() subsystem child application context factory} or
 * {@link MultiInstanceSubsystemHandler#getSubsystemEffectiveProperties(String) subsystem child application context manager}.
 *
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class SubsystemEffectivePropertiesFactoryBean
        implements FactoryBean<Properties>, ApplicationContextAware, InitializingBean, BeanNameAware
{

    private static final Logger LOGGER = LoggerFactory.getLogger(SubsystemEffectivePropertiesFactoryBean.class);

    protected ApplicationContext applicationContext;

    protected String beanName;

    protected ChildApplicationContextManager subsystemChildApplicationContextManager;

    protected ChildApplicationContextFactory subsystemChildApplicationContextFactory;

    /**
     * {@inheritDoc}
     */
    @Override
    public void setApplicationContext(final ApplicationContext applicationContext)
    {
        this.applicationContext = applicationContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBeanName(final String name)
    {
        this.beanName = name;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        if (this.subsystemChildApplicationContextFactory == null)
        {
            PropertyCheck.mandatory(this, "childApplicationContextManager", this.subsystemChildApplicationContextManager);
            if (!(this.subsystemChildApplicationContextManager instanceof MultiInstanceSubsystemHandler))
            {
                LOGGER.warn(
                        "subsystemChildApplicationContextManager for {} does not conform to the interface MultiInstanceSubsystemHandler - either subsystem has not been configured properly or original Alfresco bean may not have been enhanced",
                        this.beanName);
            }
        }
        else if (!(this.subsystemChildApplicationContextFactory instanceof SingleInstanceSubsystemHandler))
        {
            LOGGER.warn(
                    "subsystemChildApplicationContextFactory for {} does not conform to the interface SingleInstanceSubsystemHandler - either subsystem has not been configured properly or original Alfresco bean may not have been enhanced",
                    this.beanName);
        }
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
     *
     * {@inheritDoc}
     */
    @Override
    public Properties getObject() throws Exception
    {
        final Properties effectiveProperties;
        if (this.subsystemChildApplicationContextFactory instanceof SingleInstanceSubsystemHandler)
        {
            effectiveProperties = ((SingleInstanceSubsystemHandler) this.subsystemChildApplicationContextFactory)
                    .getSubsystemEffectiveProperties();
        }
        else if (this.subsystemChildApplicationContextManager instanceof MultiInstanceSubsystemHandler)
        {
            final String subsystemInstanceId = ((MultiInstanceSubsystemHandler) this.subsystemChildApplicationContextManager)
                    .determineInstanceId(this.applicationContext);
            effectiveProperties = ((MultiInstanceSubsystemHandler) this.subsystemChildApplicationContextManager)
                    .getSubsystemEffectiveProperties(subsystemInstanceId);
        }
        else
        {
            effectiveProperties = new Properties();
        }

        return effectiveProperties;
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
