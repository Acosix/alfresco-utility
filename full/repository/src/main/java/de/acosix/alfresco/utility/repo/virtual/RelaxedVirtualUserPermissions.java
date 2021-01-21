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
package de.acosix.alfresco.utility.repo.virtual;

import java.util.HashSet;
import java.util.Set;

import org.alfresco.repo.virtual.store.VirtualUserPermissions;

/**
 * This sub-class fixes a state exposure issue with the base class, allowing clients to mutate the internal state of the allow/deny
 * permission sets, and potentially run into issues because those internal sets may be immutable.
 *
 * @author Axel Faust
 */
public class RelaxedVirtualUserPermissions extends VirtualUserPermissions
{

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getAllowSmartNodes()
    {
        return new HashSet<>(super.getAllowSmartNodes());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getDenySmartNodes()
    {
        return new HashSet<>(super.getDenySmartNodes());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getDenyReadonlySmartNodes()
    {
        return new HashSet<>(super.getDenyReadonlySmartNodes());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getAllowQueryNodes()
    {
        return new HashSet<>(super.getAllowQueryNodes());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getDenyQueryNodes()
    {
        return new HashSet<>(super.getDenyQueryNodes());
    }

}
