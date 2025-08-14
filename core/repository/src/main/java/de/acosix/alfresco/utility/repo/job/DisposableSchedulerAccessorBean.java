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
package de.acosix.alfresco.utility.repo.job;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.BiConsumer;

import org.alfresco.error.AlfrescoRuntimeException;
import org.quartz.Scheduler;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.quartz.SchedulerAccessorBean;

/**
 * Instances of this class are disposable, properly removing its jobs when an application context is disposed.
 *
 * @author Axel Faust
 */
public class DisposableSchedulerAccessorBean extends SchedulerAccessorBean implements DisposableBean
{

    private static final Class<?> TRIGGER_CLASS;

    private static final BiConsumer<Scheduler, List<Object>> DISPOSER;

    private static final Method SET_TRIGGERS_METHOD;
    static
    {
        try
        {
            TRIGGER_CLASS = Class.forName("org.quartz.Trigger");
            final Object arr = Array.newInstance(TRIGGER_CLASS, 0);
            final Class<? extends Object> arrClass = arr.getClass();
            SET_TRIGGERS_METHOD = SchedulerAccessorBean.class.getMethod("setTriggers", arrClass);

            if (TRIGGER_CLASS.isInterface())
            {
                DISPOSER = de.acosix.alfresco.utility.core.repo.quartz2.DisposableJobUtilities::disposeJobTriggers;
            }
            else
            {
                DISPOSER = de.acosix.alfresco.utility.core.repo.quartz1.DisposableJobUtilities::disposeJobTriggers;
            }
        }
        catch (final ClassNotFoundException | NoSuchMethodException | SecurityException e)
        {
            throw new RuntimeException("Error looking up known Quartz 1.x/2.x API reflectively to avoid incompatibilities");
        }
    }

    private List<Object> triggers;

    /**
     * Sets the triggers to register in the scheduler.
     *
     * @param triggers
     *     the triggers
     */
    public void setTriggersGen(final List<Object> triggers)
    {
        this.triggers = triggers;

        final int size = triggers.size();
        final Object arr = Array.newInstance(TRIGGER_CLASS, size);
        for (int idx = 0; idx < size; idx++)
        {
            Array.set(arr, idx, triggers.get(idx));
        }
        try
        {
            SET_TRIGGERS_METHOD.invoke(this, arr);
        }
        catch (IllegalAccessException | InvocationTargetException e)
        {
            throw new AlfrescoRuntimeException("Failed to invoke setTriggers");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() throws Exception
    {
        DISPOSER.accept(this.getScheduler(), this.triggers);
    }

}
