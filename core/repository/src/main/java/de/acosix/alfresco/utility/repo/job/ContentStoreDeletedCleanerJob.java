/*
 * Copyright 2016 - 2021 Acosix GmbH
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import org.alfresco.repo.content.ContentStore;
import org.alfresco.repo.content.cleanup.ContentStoreCleanupJob;
import org.alfresco.service.cmr.repository.datatype.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Instances of this job delete files from the content store used to store content files after they have been orphaned long enough for the
 * {@link ContentStoreCleanupJob} to move them out of the primary content store(s).
 *
 * @author Axel Faust
 */
public class ContentStoreDeletedCleanerJob implements GenericJob
{

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentStoreDeletedCleanerJob.class);

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void execute(final Object jobExecutionContext)
    {
        final String enabled = JobUtilities.getJobDataValue(jobExecutionContext, "enabled", String.class);

        if (Boolean.parseBoolean(enabled))
        {
            LOGGER.info("Running cleanup of store for deleted content");

            final String cleanupDelayDuration = JobUtilities.getJobDataValue(jobExecutionContext, "cleanupDelayDuration", String.class);

            if (!cleanupDelayDuration.matches(
                    "^-?P(?:[1-9][0-9]*Y)?(?:[1-9][0-9]*M)?(?:[1-9][0-9]*D)?(?:T(?:[1-9][0-9]*H)?(?:[1-9][0-9]*M)?(?:[1-9][0-9]*S)?)?$"))
            {
                LOGGER.warn("Invalid / unsupported cleanup delay duration: {}", cleanupDelayDuration);
            }
            else
            {
                final ContentStore contentStoreDeleted = JobUtilities.getJobDataValue(jobExecutionContext, "contentStoreDeleted",
                        ContentStore.class);
                final String rootLocation = contentStoreDeleted.getRootLocation();
                Duration cleanupDelayDurationObj = new Duration(cleanupDelayDuration);
                if (!cleanupDelayDuration.startsWith("-"))
                {
                    cleanupDelayDurationObj = cleanupDelayDurationObj.unaryMinus();
                }
                final Date cutoff = Duration.add(new Date(), cleanupDelayDurationObj);
                final long cutoffFileModified = cutoff.getTime();

                try
                {
                    final Path path = Paths.get(rootLocation);
                    final File file = path.toFile();
                    if (file.exists() && file.isDirectory())
                    {
                        Files.walkFileTree(path, new ContentStoreDeletedCleaningVisitor(cutoffFileModified));
                    }
                    else
                    {
                        LOGGER.info(
                                "Unable to perform cleanup of store for deleted content - root location {} does not exist or is not a directory",
                                rootLocation);
                    }
                }
                catch (final InvalidPathException | UnsupportedOperationException ex)
                {
                    LOGGER.warn(
                            "Unable to perform cleanup of store for deleted content - root location {} is not a supported / resolveable file path",
                            rootLocation);
                }
                catch (final IOException ex)
                {
                    LOGGER.error("Error during clean store for deleted content", ex);
                }
            }

            LOGGER.info("Completed cleanup of store for deleted content");
        }
        else
        {
            LOGGER.debug("Job is disabled");
        }
    }

}
