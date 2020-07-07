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
package de.acosix.alfresco.utility.core.repo.quartz2;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Instances of this job class merely serve the purpose of relaying a job execution to an alternative class instance whose API has been
 * decoupled from the Quartz API to avoid binary class incompatibilities between Alfresco versions.
 *
 * @author Axel Faust
 */
public class InvocationRelayJob implements Job
{

    public static final String RELAY_CLASS = InvocationRelayJob.class.getName() + "-relayClass";

    private static final Logger LOGGER = LoggerFactory.getLogger(InvocationRelayJob.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException
    {
        final Object relayClassCandidate = context.getMergedJobDataMap().get(RELAY_CLASS);
        Object relay = null;
        Method execute = null;

        if (relayClassCandidate instanceof Class<?>)
        {
            try
            {
                relay = ((Class<?>) relayClassCandidate).newInstance();
            }
            catch (InstantiationException | IllegalAccessException ignore)
            {
                LOGGER.debug("Class {} cannot be instantiated using Class.newInstance()", relayClassCandidate);
            }

            try
            {
                execute = ((Class<?>) relayClassCandidate).getMethod("execute", Object.class);
            }
            catch (final NoSuchMethodException ignore)
            {
                LOGGER.debug("Class {} does not provide an execute(Object) method", relayClassCandidate);
            }
        }
        else
        {
            LOGGER.debug("Value {} for {} is not a class", relayClassCandidate, RELAY_CLASS);
        }

        if (relay == null || execute == null || Modifier.isStatic(execute.getModifiers()))
        {
            throw new IllegalStateException("Invalid relay class value: " + relayClassCandidate);
        }

        try
        {
            execute.invoke(relay, context);
        }
        catch (InvocationTargetException | IllegalAccessException ex)
        {
            throw new RuntimeException("Error invoking relay job class instance", ex);
        }
    }

}
