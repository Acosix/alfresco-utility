/*
 * Copyright 2016 - 2020 Acosix GmbH
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

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import org.alfresco.repo.management.subsystems.ActivateableBean;
import org.alfresco.repo.management.subsystems.ChainingSubsystemProxyFactory;
import org.alfresco.repo.management.subsystems.ChildApplicationContextManager;
import org.alfresco.repo.security.authentication.external.RemoteUserMapper;
import org.alfresco.util.PropertyCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;

/**
 * This class provides an alternative to the default {@link ChainingSubsystemProxyFactory} used to expose the first active
 * {@link RemoteUserMapper} in the Alfresco authentication chain. By replacing the default proxy with this implementation, an Alfresco
 * Repository can support running multiple user mappers, from which the first one to successfully identify and validate a remote user will
 * "win".
 *
 * @author Axel Faust
 */
public class SubsystemChainingRemoteUserMapper implements RemoteUserMapper, ActivateableBean, InitializingBean
{

    private static final Logger LOGGER = LoggerFactory.getLogger(SubsystemChainingRemoteUserMapper.class);

    protected ChildApplicationContextManager applicationContextManager;

    protected String sourceBeanName;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "applicationContextManager", this.applicationContextManager);
        PropertyCheck.mandatory(this, "sourceBeanName", this.sourceBeanName);
    }

    /**
     * Sets the application context manager.
     *
     * @param applicationContextManager
     *            the applicationContextManager to set
     */
    public void setApplicationContextManager(final ChildApplicationContextManager applicationContextManager)
    {
        this.applicationContextManager = applicationContextManager;
    }

    /**
     * Sets the name of the bean to look up in the child application contexts.
     *
     * @param sourceBeanName
     *            the bean name
     */
    public void setSourceBeanName(final String sourceBeanName)
    {
        this.sourceBeanName = sourceBeanName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isActive()
    {
        final Collection<String> instanceIds = this.applicationContextManager.getInstanceIds();

        boolean active = false;
        for (final String instanceId : instanceIds)
        {
            final RemoteUserMapper remoteUserMapper = this.lookupMapperInSubsystem(instanceId);

            if (remoteUserMapper != null)
            {
                active = !(remoteUserMapper instanceof ActivateableBean) || ((ActivateableBean) remoteUserMapper).isActive();
            }

            if (active)
            {
                break;
            }
        }

        return active;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRemoteUser(final HttpServletRequest request)
    {
        final Collection<String> instanceIds = this.applicationContextManager.getInstanceIds();

        String remoteUser = null;
        for (final String instanceId : instanceIds)
        {
            final RemoteUserMapper remoteUserMapper = this.lookupMapperInSubsystem(instanceId);

            if (remoteUserMapper != null)
            {
                if (!(remoteUserMapper instanceof ActivateableBean) || ((ActivateableBean) remoteUserMapper).isActive())
                {
                    remoteUser = remoteUserMapper.getRemoteUser(request);
                }
            }

            if (remoteUser != null)
            {
                break;
            }
        }

        return remoteUser;
    }

    protected RemoteUserMapper lookupMapperInSubsystem(final String instanceId)
    {
        RemoteUserMapper remoteUserMapper = null;

        ApplicationContext subsystemContext;
        try
        {
            subsystemContext = this.applicationContextManager.getApplicationContext(instanceId);
        }
        catch (final RuntimeException e)
        {
            LOGGER.debug("Subsystem {} does not start properly - skipping", instanceId);
            subsystemContext = null;
        }

        if (subsystemContext != null)
        {
            if (subsystemContext.containsBean(this.sourceBeanName))
            {
                try
                {
                    remoteUserMapper = subsystemContext.getBean(this.sourceBeanName, RemoteUserMapper.class);
                }
                catch (final BeansException e)
                {
                    LOGGER.debug("Subsystem {} does not define a bean {} compatible with required interface", instanceId,
                            this.sourceBeanName, e);
                }
            }
            else
            {
                LOGGER.debug("Subsystem {} does not define a bean {}", instanceId, this.sourceBeanName);
            }
        }

        return remoteUserMapper;
    }
}
