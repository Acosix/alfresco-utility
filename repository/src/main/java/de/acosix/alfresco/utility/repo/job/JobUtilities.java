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
package de.acosix.alfresco.utility.repo.job;

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
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public final class JobUtilities
{

    private static final Logger LOGGER = LoggerFactory.getLogger(JobUtilities.class);

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
     *            the context of the job
     * @param key
     *            the key to the expected value entry
     * @param valueClass
     *            the type of the value
     * @return the value for the specified key
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
     *            the context of the job
     * @param key
     *            the key to the expected value entry
     * @param valueClass
     *            the type of the value
     * @param nonNull
     *            {@code true} if the value must be set, or {@code false} if {@code null} is allowed
     * @return the value for the specified key
     *
     * @throws IllegalStateException
     *             if no value has been set for the specified key and {@code nonNull} has been specified as {@code true} ,or if the value is
     *             incompatible with the expected type
     */
    public static <V> V getJobDataValue(final JobExecutionContext context, final String key, final Class<V> valueClass,
            final boolean nonNull)
    {
        final JobDataMap jobDataMap = context.getMergedJobDataMap();
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
     * Executes an operation within the context of a job lock, preventing concurrent execution by other processes on this node or any other
     * node within a Repository cluster. The operation is itself responsible to refresh the acquired lock if necessary to ensure it does not
     * expire before the completion of the operation.
     *
     * @param context
     *            the context of the job
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
     *            the context of the job
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
     *            the context of the job
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
        final AtomicBoolean refreshFailed = new AtomicBoolean();
        final JobLockService jobLockService = getJobDataValue(context, "jobLockService", JobLockService.class);
        try
        {
            LOGGER.debug("Trying to obtain lock {} for job {} with TTL {}, retryWait {} and retryCount {}", lockQName,
                    context.getJobDetail().getFullName(), lockTTL, retryWait, retryCount);
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
                        LOGGER.warn("Lock refresh failed for lock {} and job {}", lockQName, context.getJobDetail().getFullName());
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
                            lockQName, context.getJobDetail().getFullName());
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
                LOGGER.info("Lock acquisition failed for lock {} and job {}", lockQName, context.getJobDetail().getFullName());
            }
            throw laex;
        }
    }

    /**
     * Executes an operation within the context of a job lock, preventing concurrent execution by other processes on this node or any other
     * node within a Repository cluster. The acquired lock will be refreshed automatically while the operation is running.
     *
     * @param context
     *            the context of the job
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
        final AtomicBoolean refreshFailed = new AtomicBoolean();
        final JobLockService jobLockService = getJobDataValue(context, "jobLockService", JobLockService.class);
        try
        {
            final AtomicBoolean active = new AtomicBoolean(true);
            final AtomicBoolean released = new AtomicBoolean();

            LOGGER.debug("Trying to obtain lock {} for job {} with TTL {}, retryWait {} and retryCount {}", lockQName,
                    context.getJobDetail().getFullName(), lockTTL, retryWait, retryCount);
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
                            lockToken, lockQName, context.getJobDetail().getFullName());
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
                LOGGER.info("Lock acquisition failed for lock {} and job {}", lockQName, context.getJobDetail().getFullName());
            }
            throw laex;
        }
    }

    /**
     * This functional interface represents job operations that may manually trigger a refresh of the acquired lock when necessary.
     *
     * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
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
     * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
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
     * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
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
     * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
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
}
