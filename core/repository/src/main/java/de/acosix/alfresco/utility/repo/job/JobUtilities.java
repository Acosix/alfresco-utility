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
package de.acosix.alfresco.utility.repo.job;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import org.alfresco.repo.lock.JobLockService;
import org.alfresco.repo.lock.JobLockService.JobLockRefreshCallback;
import org.alfresco.repo.lock.LockAcquisitionException;
import org.alfresco.service.namespace.QName;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides utility operations for handling Repository-tier jobs in a consistent way.
 *
 * @author Axel Faust
 */
public final class JobUtilities
{

    private static final Logger LOGGER = LoggerFactory.getLogger(JobUtilities.class);

    // API change (class => interface) between Quartz 1.x and 2.x means we have to use reflection for various accesses
    private static final Class<?> JOB_EXECUTION_CONTEXT_CLASS;

    private static final Method JOB_EXECUTION_CONTEXT_GET_JOB_DETAIL;

    private static final Method JOB_EXECUTION_CONTEXT_GET_MERGED_JOB_DATA_MAP;

    private static final Method JOB_DETAIL_GET_FULL_NAME;

    private static final Method JOB_DETAIL_GET_KEY;

    static
    {
        Class<?> jobDetailClass;
        try
        {
            JOB_EXECUTION_CONTEXT_CLASS = Class.forName("org.quartz.JobExecutionContext");
            JOB_EXECUTION_CONTEXT_GET_JOB_DETAIL = JOB_EXECUTION_CONTEXT_CLASS.getMethod("getJobDetail");
            JOB_EXECUTION_CONTEXT_GET_MERGED_JOB_DATA_MAP = JOB_EXECUTION_CONTEXT_CLASS.getMethod("getMergedJobDataMap");
            jobDetailClass = Class.forName("org.quartz.JobDetail");
        }
        catch (final Throwable e)
        {
            throw new RuntimeException("Error looking up known Quartz 1.x/2.x API reflectively to avoid incompatibilities");
        }

        Method getFullName = null;
        try
        {
            getFullName = jobDetailClass.getMethod("getFullName");
        }
        catch (final NoSuchMethodException ignore)
        {
            // NO-OP
        }
        JOB_DETAIL_GET_FULL_NAME = getFullName;

        Method getKey = null;
        try
        {
            getKey = jobDetailClass.getMethod("getKey");
        }
        catch (final NoSuchMethodException ignore)
        {
            // NO-OP
        }
        JOB_DETAIL_GET_KEY = getKey;
    }

    private static final int DEFAULT_LOCK_RETRIES = 15;

    private static final long DEFAULT_SINGLE_LOCK_WAIT = 1000;

    private static final int LOCK_WAIT_FACTOR = 2;

    private static final long MAX_TOTAL_LOCK_WAIT = LOCK_WAIT_FACTOR * DEFAULT_SINGLE_LOCK_WAIT * DEFAULT_LOCK_RETRIES;

    private static final long REFRESHING_LOCK_TTL = 5000;

    private JobUtilities()
    {
        // NO-OP
    }

    /**
     * Retrieves a non-null, keyed value of a specific type from the data associated with a specific job.
     *
     * @param context
     *            the {@link JobExecutionContext context of the job}
     * @param key
     *            the key to the expected value entry
     * @param valueClass
     *            the type of the value
     * @return the value for the specified key
     *
     * @param <V>
     *            the expected type of the value
     *
     * @throws IllegalStateException
     *             if no value has been set for the specified key or if the value is incompatible with the expected type
     */
    public static <V> V getJobDataValue(final Object context, final String key, final Class<V> valueClass)
    {
        return getJobDataValue(context, key, valueClass, true);
    }

    /**
     * Retrieves a non-null, keyed value of a specific type from the data associated with a specific job.
     *
     * @param context
     *            the {@link JobExecutionContext context of the job}
     * @param key
     *            the key to the expected value entry
     * @param valueClass
     *            the type of the value
     * @return the value for the specified key
     *
     * @param <V>
     *            the expected type of the value
     *
     * @throws IllegalStateException
     *             if no value has been set for the specified key or if the value is incompatible with the expected type
     */
    public static <V> V getJobDataValue(final JobExecutionContext context, final String key, final Class<V> valueClass)
    {
        return getJobDataValue(context, key, valueClass, true);
    }

    /**
     * Retrieves a keyed value of a specific type from the data associated with a specific job.
     *
     * @param context
     *            the {@link JobExecutionContext context of the job}
     * @param key
     *            the key to the expected value entry
     * @param valueClass
     *            the type of the value
     * @param nonNull
     *            {@code true} if the value must be set, or {@code false} if {@code null} is allowed
     * @return the value for the specified key
     *
     * @param <V>
     *            the expected type of the value
     *
     * @throws IllegalStateException
     *             if no value has been set for the specified key and {@code nonNull} has been specified as {@code true} ,or if the value is
     *             incompatible with the expected type
     */
    public static <V> V getJobDataValue(final Object context, final String key, final Class<V> valueClass, final boolean nonNull)
    {
        verifyJobExecutionContext(context);

        final JobDataMap jobDataMap = getJobDataMap(context);
        final Object object = jobDataMap.get(key);

        if (object == null && nonNull)
        {
            throw new IllegalStateException("Entry " + key + " has not been set in job data map");
        }

        if (!valueClass.isInstance(object))
        {
            throw new IllegalStateException("Entry " + key + " in job data map is not compatible to expected value class " + valueClass);
        }

        final V value = valueClass.cast(object);
        return value;
    }

    /**
     * Retrieves a keyed value of a specific type from the data associated with a specific job.
     *
     * @param context
     *            the {@link JobExecutionContext context of the job}
     * @param key
     *            the key to the expected value entry
     * @param valueClass
     *            the type of the value
     * @param nonNull
     *            {@code true} if the value must be set, or {@code false} if {@code null} is allowed
     * @return the value for the specified key
     *
     * @param <V>
     *            the expected type of the value
     *
     * @throws IllegalStateException
     *             if no value has been set for the specified key and {@code nonNull} has been specified as {@code true} ,or if the value is
     *             incompatible with the expected type
     */
    public static <V> V getJobDataValue(final JobExecutionContext context, final String key, final Class<V> valueClass,
            final boolean nonNull)
    {
        return getJobDataValue((Object) context, key, valueClass, nonNull);
    }

    /**
     * Executes an operation within the context of a job lock, preventing concurrent execution by other processes on this node or any other
     * node within a Repository cluster. The operation is itself responsible to refresh the acquired lock if necessary to ensure it does not
     * expire before the completion of the operation.
     *
     * @param context
     *            the {@link JobExecutionContext context of the job}
     * @param lockQName
     *            the qualified name of the lock to acquire
     * @param lockTTL
     *            the time-to-live for the lock
     * @param operation
     *            the callback to the operation that must run within the context of a job lock
     *
     * @throws LockAcquisitionException
     *             if the lock could not be acquired or a refresh triggered by the operation failed
     */
    public static void runWithJobLock(final Object context, final QName lockQName, final long lockTTL,
            final ManualRefreshOperationWithJobLock operation)
    {
        runWithJobLock(context, lockQName, lockTTL, Math.min(lockTTL, MAX_TOTAL_LOCK_WAIT) / DEFAULT_LOCK_RETRIES, DEFAULT_LOCK_RETRIES,
                operation);
    }

    /**
     * Executes an operation within the context of a job lock, preventing concurrent execution by other processes on this node or any other
     * node within a Repository cluster. The operation is itself responsible to refresh the acquired lock if necessary to ensure it does not
     * expire before the completion of the operation.
     *
     * @param context
     *            the {@link JobExecutionContext context of the job}
     * @param lockQName
     *            the qualified name of the lock to acquire
     * @param lockTTL
     *            the time-to-live for the lock
     * @param operation
     *            the callback to the operation that must run within the context of a job lock
     *
     * @throws LockAcquisitionException
     *             if the lock could not be acquired or a refresh triggered by the operation failed
     */
    public static void runWithJobLock(final JobExecutionContext context, final QName lockQName, final long lockTTL,
            final ManualRefreshOperationWithJobLock operation)
    {
        runWithJobLock(context, lockQName, lockTTL, Math.min(lockTTL, MAX_TOTAL_LOCK_WAIT) / DEFAULT_LOCK_RETRIES, DEFAULT_LOCK_RETRIES,
                operation);
    }

    /**
     * Executes an operation within the context of a job lock, preventing concurrent execution by other processes on this node or any other
     * node within a Repository cluster. The acquired lock will be refreshed automatically while the operation is running.
     *
     * @param context
     *            the {@link JobExecutionContext context of the job}
     * @param lockQName
     *            the qualified name of the lock to acquire
     * @param operation
     *            the callback to the operation that must run within the context of a job lock
     *
     * @throws LockAcquisitionException
     *             if the lock could not be acquired
     */
    public static void runWithJobLock(final Object context, final QName lockQName, final RefreshAwareOperationWithJobLock operation)
    {
        runWithJobLock(context, lockQName, REFRESHING_LOCK_TTL, REFRESHING_LOCK_TTL / DEFAULT_LOCK_RETRIES, DEFAULT_LOCK_RETRIES,
                operation);
    }

    /**
     * Executes an operation within the context of a job lock, preventing concurrent execution by other processes on this node or any other
     * node within a Repository cluster. The acquired lock will be refreshed automatically while the operation is running.
     *
     * @param context
     *            the {@link JobExecutionContext context of the job}
     * @param lockQName
     *            the qualified name of the lock to acquire
     * @param operation
     *            the callback to the operation that must run within the context of a job lock
     *
     * @throws LockAcquisitionException
     *             if the lock could not be acquired
     */
    public static void runWithJobLock(final JobExecutionContext context, final QName lockQName,
            final RefreshAwareOperationWithJobLock operation)
    {
        runWithJobLock(context, lockQName, REFRESHING_LOCK_TTL, REFRESHING_LOCK_TTL / DEFAULT_LOCK_RETRIES, DEFAULT_LOCK_RETRIES,
                operation);
    }

    /**
     * Executes an operation within the context of a job lock, preventing concurrent execution by other processes on this node or any other
     * node within a Repository cluster. The operation is itself responsible to refresh the acquired lock if necessary to ensure it does not
     * expire before the completion of the operation.
     *
     * @param context
     *            the {@link JobExecutionContext context of the job}
     * @param lockQName
     *            the qualified name of the lock to acquire
     * @param lockTTL
     *            the time-to-live for the lock
     * @param retryWait
     *            the amount of time in milliseconds to wait to retry lock acquisition if the lock is currently being held by another
     *            process
     * @param retryCount
     *            the number of times to retry lock acquisition if the lock is currently being held by another
     * @param operation
     *            the callback to the operation that must run within the context of a job lock
     *
     * @throws LockAcquisitionException
     *             if the lock could not be acquired or a refresh triggered by the operation failed
     */
    public static void runWithJobLock(final Object context, final QName lockQName, final long lockTTL, final long retryWait,
            final int retryCount, final ManualRefreshOperationWithJobLock operation)
    {
        verifyJobExecutionContext(context);

        final AtomicBoolean refreshFailed = new AtomicBoolean();
        final JobLockService jobLockService = getJobDataValue(context, "jobLockService", JobLockService.class);
        try
        {
            LOGGER.debug("Trying to obtain lock {} for job {} with TTL {}, retryWait {} and retryCount {}", lockQName,
                    new JobExecutionContextToStringWrapper(context), lockTTL, retryWait, retryCount);
            final String lockToken = jobLockService.getLock(lockQName, lockTTL, retryWait, retryCount);
            try
            {
                operation.withJobLock(() -> {
                    try
                    {
                        jobLockService.refreshLock(lockToken, lockQName, lockTTL);
                    }
                    catch (final LockAcquisitionException laex)
                    {
                        LOGGER.warn("Lock refresh failed for lock {} and job {}", lockQName,
                                new JobExecutionContextToStringWrapper(context));
                        refreshFailed.set(true);
                        throw laex;
                    }
                });
            }
            finally
            {
                final boolean releasedProperly = jobLockService.releaseLockVerify(lockToken, lockQName);
                if (!releasedProperly)
                {
                    LOGGER.warn("Token {} for lock has expired and was claimed by another process while job {} was running", lockToken,
                            lockQName, new JobExecutionContextToStringWrapper(context));
                }
                else
                {
                    LOGGER.debug("Released lock {} (token {})", lockQName, lockToken);
                }
            }
        }
        catch (final LockAcquisitionException laex)
        {
            if (!refreshFailed.get())
            {
                LOGGER.info("Lock acquisition failed for lock {} and job {}", lockQName, new JobExecutionContextToStringWrapper(context));
            }
            throw laex;
        }
    }

    /**
     * Executes an operation within the context of a job lock, preventing concurrent execution by other processes on this node or any other
     * node within a Repository cluster. The operation is itself responsible to refresh the acquired lock if necessary to ensure it does not
     * expire before the completion of the operation.
     *
     * @param context
     *            the {@link JobExecutionContext context of the job}
     * @param lockQName
     *            the qualified name of the lock to acquire
     * @param lockTTL
     *            the time-to-live for the lock
     * @param retryWait
     *            the amount of time in milliseconds to wait to retry lock acquisition if the lock is currently being held by another
     *            process
     * @param retryCount
     *            the number of times to retry lock acquisition if the lock is currently being held by another
     * @param operation
     *            the callback to the operation that must run within the context of a job lock
     *
     * @throws LockAcquisitionException
     *             if the lock could not be acquired or a refresh triggered by the operation failed
     */
    public static void runWithJobLock(final JobExecutionContext context, final QName lockQName, final long lockTTL, final long retryWait,
            final int retryCount, final ManualRefreshOperationWithJobLock operation)
    {
        runWithJobLock((Object) context, lockQName, lockTTL, retryWait, retryCount, operation);
    }

    /**
     * Executes an operation within the context of a job lock, preventing concurrent execution by other processes on this node or any other
     * node within a Repository cluster. The acquired lock will be refreshed automatically while the operation is running.
     *
     * @param context
     *            the {@link JobExecutionContext context of the job}
     * @param lockQName
     *            the qualified name of the lock to acquire
     * @param lockTTL
     *            the time-to-live for the lock
     * @param retryWait
     *            the amount of time in milliseconds to wait to retry lock acquisition if the lock is currently being held by another
     *            process
     * @param retryCount
     *            the number of times to retry lock acquisition if the lock is currently being held by another
     * @param operation
     *            the callback to the operation that must run within the context of a job lock
     *
     * @throws LockAcquisitionException
     *             if the lock could not be acquired
     */
    public static void runWithJobLock(final Object context, final QName lockQName, final long lockTTL, final long retryWait,
            final int retryCount, final RefreshAwareOperationWithJobLock operation)
    {
        verifyJobExecutionContext(context);

        final AtomicBoolean refreshFailed = new AtomicBoolean();
        final JobLockService jobLockService = getJobDataValue(context, "jobLockService", JobLockService.class);
        try
        {
            final AtomicBoolean active = new AtomicBoolean(true);
            final AtomicBoolean released = new AtomicBoolean();

            LOGGER.debug("Trying to obtain lock {} for job {} with TTL {}, retryWait {} and retryCount {}", lockQName,
                    new JobExecutionContextToStringWrapper(context), lockTTL, retryWait, retryCount);
            final String lockToken = jobLockService.getLock(lockQName, lockTTL, retryWait, retryCount);
            try
            {
                jobLockService.refreshLock(lockToken, lockQName, lockTTL, new JobLockRefreshCallback()
                {

                    /**
                     *
                     */
                    @Override
                    public void lockReleased()
                    {
                        released.set(true);
                    }

                    @Override
                    public boolean isActive()
                    {
                        return active.get();
                    }
                });

                operation.withJobLock(() -> {
                    return released.get();
                });
            }
            finally
            {
                final boolean releasedProperly = jobLockService.releaseLockVerify(lockToken, lockQName);
                if (!releasedProperly)
                {
                    LOGGER.error(
                            "Token {} for lock has expired and was claimed by another process while job {} was running - this should not have happened due to automatic refresh handling",
                            lockToken, lockQName, new JobExecutionContextToStringWrapper(context));
                }
                else
                {
                    LOGGER.debug("Released lock {} (token {})", lockQName, lockToken);
                }

                active.set(false);
            }
        }
        catch (final LockAcquisitionException laex)
        {
            if (!refreshFailed.get())
            {
                LOGGER.info("Lock acquisition failed for lock {} and job {}", lockQName, new JobExecutionContextToStringWrapper(context));
            }
            throw laex;
        }
    }

    /**
     * Executes an operation within the context of a job lock, preventing concurrent execution by other processes on this node or any other
     * node within a Repository cluster. The acquired lock will be refreshed automatically while the operation is running.
     *
     * @param context
     *            the {@link JobExecutionContext context of the job}
     * @param lockQName
     *            the qualified name of the lock to acquire
     * @param lockTTL
     *            the time-to-live for the lock
     * @param retryWait
     *            the amount of time in milliseconds to wait to retry lock acquisition if the lock is currently being held by another
     *            process
     * @param retryCount
     *            the number of times to retry lock acquisition if the lock is currently being held by another
     * @param operation
     *            the callback to the operation that must run within the context of a job lock
     *
     * @throws LockAcquisitionException
     *             if the lock could not be acquired
     */
    public static void runWithJobLock(final JobExecutionContext context, final QName lockQName, final long lockTTL, final long retryWait,
            final int retryCount, final RefreshAwareOperationWithJobLock operation)
    {
        runWithJobLock((Object) context, lockQName, lockTTL, retryWait, retryCount, operation);
    }

    /**
     * This functional interface represents job operations that may manually trigger a refresh of the acquired lock when necessary.
     *
     * @author Axel Faust
     */
    @FunctionalInterface
    public static interface ManualRefreshOperationWithJobLock
    {

        /**
         * Continues execution of the job in the context of an acquired job lock.
         *
         * @param refresher
         *            the callback to use for refreshing the lock held by the job being currently executed
         */
        void withJobLock(LockRefresher refresher);
    }

    /**
     * This functional interface provides the means to manually trigger a lock refresh from an {@link ManualRefreshOperationWithJobLock
     * operation with a job lock}.
     *
     * @author Axel Faust
     */
    @FunctionalInterface
    public static interface LockRefresher
    {

        /**
         * Refreshes the lock held for the job being currently executed.
         *
         * @throws LockAcquisitionException
         *             if the lock could not be refreshed or acquired
         */
        void refreshLock();
    }

    /**
     * This functional interface represents job operations that use automatically refreshing locks.
     *
     * @author Axel Faust
     */
    @FunctionalInterface
    public static interface RefreshAwareOperationWithJobLock
    {

        /**
         * Continues execution of the job in the context of an acquired job lock.
         *
         * @param releaseCheck
         *            the callback to check if the lock has been {@link LockReleasedCheck#isLockReleased() released} while the operation was
         *            running - the operation should
         *            regularly check the result of this callback and immediately cease execution in case the lock was released
         */
        void withJobLock(LockReleasedCheck releaseCheck);
    }

    /**
     * This functional interface provides the means to check if a job lock has been released while the
     * {@link ManualRefreshOperationWithJobLock job operation} was running.
     *
     * @author Axel Faust
     */
    @FunctionalInterface
    public static interface LockReleasedCheck
    {

        /**
         * Checks if the acquired job lock has been released either as a result of a failed lock refresh or a VM shutdown having been
         * triggered.
         *
         * @return {@code true} if the lock has been released, {@code false} otherwise
         */
        boolean isLockReleased();
    }

    /**
     * Instances of this class are used only for logging purposes to handle the stringification of a job execution context to the full name
     * / key of the job detail it represents.
     *
     * @author Axel Faust
     */
    private static class JobExecutionContextToStringWrapper
    {

        private final Object jobExecutionContext;

        protected JobExecutionContextToStringWrapper(final Object jobExecutionContext)
        {
            this.jobExecutionContext = jobExecutionContext;
        }

        /**
         *
         * {@inheritDoc}
         */
        @Override
        public String toString()
        {
            final Object jobDetailId = getJobDetailId(this.jobExecutionContext);
            return String.valueOf(jobDetailId);
        }
    }

    private static void verifyJobExecutionContext(final Object jobExecutionContext)
    {
        if (!JOB_EXECUTION_CONTEXT_CLASS.isInstance(jobExecutionContext))
        {
            throw new IllegalArgumentException("'context' must be an instance of org.quartz.JobExecutionContext");
        }
    }

    private static JobDataMap getJobDataMap(final Object jobExecutionContext)
    {
        try
        {
            final Object jobDataMap = JOB_EXECUTION_CONTEXT_GET_MERGED_JOB_DATA_MAP.invoke(jobExecutionContext);
            return (JobDataMap) jobDataMap;
        }
        catch (final Throwable e)
        {
            throw new RuntimeException("Unexpected error calling known Quartz 1.x/2.x API reflectively to avoid API incompatibility");
        }
    }

    private static Object getJobDetailId(final Object jobExecutionContext)
    {
        try
        {
            final Object jobDetail = JOB_EXECUTION_CONTEXT_GET_JOB_DETAIL.invoke(jobExecutionContext);
            final Object id = JOB_DETAIL_GET_FULL_NAME != null ? JOB_DETAIL_GET_FULL_NAME.invoke(jobDetail)
                    : JOB_DETAIL_GET_KEY.invoke(jobDetail);
            return id;
        }
        catch (final Throwable e)
        {
            throw new RuntimeException("Unexpected error calling known Quartz 1.x/2.x API reflectively to avoid API incompatibility");
        }
    }
}
