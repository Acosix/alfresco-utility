/*
 * Copyright 2016 - 2019 Acosix GmbH
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
package de.acosix.alfresco.utility.repo.job;

import org.quartz.Job;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;

/**
 * @author Axel Faust
 */
public class GenericJobDetailsFactoryBean extends JobDetailFactoryBean
{

    private static final Class<? extends Job> EFFECTIVE_JOB_CLASS;

    private static final String RELAY_JOB_CLASS_DATA_KEY;

    static
    {
        try
        {
            final Class<?> jobExecutionContextCls = Class.forName("org.quartz.JobExecutionContext");
            if (jobExecutionContextCls.isInterface())
            {
                EFFECTIVE_JOB_CLASS = de.acosix.alfresco.utility.core.repo.quartz2.InvocationRelayJob.class;
                RELAY_JOB_CLASS_DATA_KEY = de.acosix.alfresco.utility.core.repo.quartz2.InvocationRelayJob.RELAY_CLASS;
            }
            else
            {
                EFFECTIVE_JOB_CLASS = de.acosix.alfresco.utility.core.repo.quartz1.InvocationRelayJob.class;
                RELAY_JOB_CLASS_DATA_KEY = de.acosix.alfresco.utility.core.repo.quartz1.InvocationRelayJob.RELAY_CLASS;
            }
        }
        catch (final ClassNotFoundException cnfe)
        {
            throw new IllegalStateException("Quartz not available", cnfe);
        }
    }

    // due to erasure of generics, this might actually not extend Job if set via Spring using auto-conversion from String to class
    protected Class<? extends Job> jobClass;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        if (GenericJob.class.isAssignableFrom(this.jobClass))
        {
            this.getJobDataMap().put(RELAY_JOB_CLASS_DATA_KEY, this.jobClass);
            this.setJobClass(EFFECTIVE_JOB_CLASS);
        }

        super.afterPropertiesSet();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void setJobClass(final Class<? extends Job> jobClass)
    {
        this.jobClass = jobClass;
        super.setJobClass(jobClass);
    }
}
