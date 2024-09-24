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
package de.acosix.alfresco.utility.repo.email.imap;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Instances of this class denote filter settings to apply to message operations.
 *
 * @author Axel Faust
 */
public class MessageFilter
{

    private int flagSetBits = 0;

    private int flagUnsetBits = 0;

    private final Set<String> flagSetNames = new HashSet<>();

    private final Set<String> flagUnsetNames = new HashSet<>();

    private final Set<String> allowedFromAddressPatterns = new HashSet<>();

    private final Set<String> blockedFromAddressPatterns = new HashSet<>();

    /**
     * Retrieves the flag bits that must be set on messages.
     *
     * @return the flagSetBits
     */
    public int getFlagSetBits()
    {
        return this.flagSetBits;
    }

    /**
     * Sets the flag bits that must be set on messages.
     *
     * @param flagSetBits
     *     the flagSetBits to set
     */
    public void setFlagSetBits(final int flagSetBits)
    {
        this.flagSetBits = flagSetBits;
    }

    /**
     * Retrieves the flag bits that must not be set on messages.
     *
     * @return the flagUnsetBits
     */
    public int getFlagUnsetBits()
    {
        return this.flagUnsetBits;
    }

    /**
     * Sets the flag bits that must not be set on messages.
     *
     * @param flagUnsetBits
     *     the flagUnsetBits to set
     */
    public void setFlagUnsetBits(final int flagUnsetBits)
    {
        this.flagUnsetBits = flagUnsetBits;
    }

    /**
     * Retrieves the custom flag names that must be set on messages.
     *
     * @return the flagSetNames
     */
    public Set<String> getFlagSetNames()
    {
        return Collections.unmodifiableSet(this.flagSetNames);
    }

    /**
     * Adds a custom flag name that must be set on messages.
     *
     * @param flagName
     *     the custom flag name
     */
    public void addFlagSetName(final String flagName)
    {
        this.flagSetNames.add(flagName);
    }

    /**
     * Retrieves the custom flag names that must not be set on messages.
     *
     * @return the flagUnsetNames
     */
    public Set<String> getFlagUnsetNames()
    {
        return Collections.unmodifiableSet(this.flagUnsetNames);
    }

    /**
     * Adds a custom flag name that must not be set on messages.
     *
     * @param flagName
     *     the custom flag name
     */
    public void addFlagUnsetName(final String flagName)
    {
        this.flagUnsetNames.add(flagName);
    }

    /**
     * Retrieves the from address patterns that are allowed on messages.
     *
     * @return the allowedFromAddressPatterns
     */
    public Set<String> getAllowedFromAddressPatterns()
    {
        return Collections.unmodifiableSet(this.allowedFromAddressPatterns);
    }

    /**
     * Adds a from address that is blocked on messages.
     *
     * @param fromAdddressPattern
     *     the from address pattern
     */
    public void addAllowedFromAddressPattern(final String fromAdddressPattern)
    {
        this.allowedFromAddressPatterns.add(fromAdddressPattern);
    }

    /**
     * Retrieves the from address patterns that are blocked on messages.
     *
     * @return the blockedFromAddressPatterns
     */
    public Set<String> getBlockedFromAddressPatterns()
    {
        return Collections.unmodifiableSet(this.blockedFromAddressPatterns);
    }

    /**
     * Adds a from address that is blocked on messages.
     *
     * @param fromAdddressPattern
     *     the from address pattern
     */
    public void addBlockedFromAddressPattern(final String fromAdddressPattern)
    {
        this.blockedFromAddressPatterns.add(fromAdddressPattern);
    }
}
