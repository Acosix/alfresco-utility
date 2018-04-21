/*
 * Copyright 2016 - 2018 Acosix GmbH
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
package de.acosix.alfresco.utility.repo.batch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.alfresco.repo.batch.BatchProcessWorkProvider;
import org.alfresco.util.ParameterCheck;

/**
 * Implementation of a simple batch process work provider used to handle pre-collection collections of data entries
 *
 * @author Axel Faust
 */
public class CollectionWrappingWorkProvider<T> implements BatchProcessWorkProvider<T>
{

    protected final List<T> sourceList;

    protected final Iterator<T> sourceIterator;

    protected final int batchSize;

    /**
     * Creates a new instance of this work provider implementation
     *
     * @param sourceCollection
     *            the source collection to wrap
     * @param batchSize
     *            the expected batch size to properly size {@link BatchProcessWorkProvider#getNextWork() the next work}
     */
    public CollectionWrappingWorkProvider(final Collection<T> sourceCollection, final int batchSize)
    {
        ParameterCheck.mandatoryCollection("sourceCollection", sourceCollection);
        if (batchSize <= 0)
        {
            throw new IllegalArgumentException("Batch size must be a positive integer");
        }
        this.sourceList = new ArrayList<>(sourceCollection);
        this.batchSize = batchSize;
        this.sourceIterator = this.sourceList.iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTotalEstimatedWorkSize()
    {
        return this.sourceList.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<T> getNextWork()
    {
        final Collection<T> nextWork = new ArrayList<>(this.batchSize);
        while (this.sourceIterator.hasNext() && nextWork.size() < this.batchSize)
        {
            nextWork.add(this.sourceIterator.next());
        }
        return nextWork;
    }

}
