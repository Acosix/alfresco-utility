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

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Instances of this job file visitor process the file tree of a deleted content store, removing all files last modified before a particular
 * cutoff date and cleaning up empty directories.
 *
 * @author Axel Faust
 */
public class ContentStoreDeletedCleaningVisitor implements FileVisitor<Path>
{

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentStoreDeletedCleaningVisitor.class);

    private final long cutoffFileModified;

    public ContentStoreDeletedCleaningVisitor(final long cutoffFileModified)
    {
        this.cutoffFileModified = cutoffFileModified;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException
    {
        LOGGER.debug("Visiting directory {}", dir);
        return FileVisitResult.CONTINUE;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException
    {
        final FileTime lastModifiedTime = Files.getLastModifiedTime(file, LinkOption.NOFOLLOW_LINKS);
        if (lastModifiedTime.toMillis() < this.cutoffFileModified)
        {
            LOGGER.debug("Deleting {}", file);
            Files.delete(file);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException
    {
        LOGGER.debug("Failed to visit file {}", file, exc);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException
    {
        final long entries = Files.list(dir).count();
        if (entries == 0)
        {
            LOGGER.debug("Deleting empty directory {}", dir);
            Files.delete(dir);
        }
        else
        {
            LOGGER.debug("Leaving directory {}", dir);
        }
        return FileVisitResult.CONTINUE;
    }
}