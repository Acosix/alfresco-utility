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
package de.acosix.alfresco.utility.core.repo.quartz1;

import java.util.List;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

/**
 * This utility class provides the means to dispose of Quartz 1.x job triggers.
 *
 * @author Axel Faust
 */
public final class DisposableJobUtilities
{

    /**
     * Disposes of jobs by removing their triggers from the scheduler.
     *
     * @param scheduler
     *     the scheduler to access
     * @param triggers
     *     the list of triggers to remove
     */
    public static void disposeJobTriggers(final Scheduler scheduler, final List<Object> triggers)
    {
        for (final Object trigger : triggers)
        {
            if (trigger instanceof Trigger)
            {
                final String jobGroup = ((Trigger) trigger).getJobGroup();
                final String jobName = ((Trigger) trigger).getJobName();

                try
                {
                    scheduler.deleteJob(jobName, jobGroup);
                }
                catch (final SchedulerException e)
                {
                    throw new RuntimeException("Failed to delete job", e);
                }
            }
        }
    }

    private DisposableJobUtilities()
    {
        // NO-OP
    }
}
