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
package de.acosix.alfresco.utility.repo.subetha3.email.imap;

import java.util.Set;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.search.SearchTerm;

/**
 * Instances of this class perform a regex-like comparison of a from address agaist a configured set of allowed patterns.
 *
 * @author Axel Faust
 */
public class FromAddressesTerm extends SearchTerm
{

    private final Set<String> patterns;

    /**
     * Creates an instance of this class.
     *
     * @param patterns
     *     the allowed patterns
     */
    public FromAddressesTerm(final Set<String> patterns)
    {
        this.patterns = patterns;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean match(final Message msg)
    {
        Address[] from;

        try
        {
            from = msg.getFrom();
        }
        catch (final Exception e)
        {
            return false;
        }

        if (from == null)
        {
            return false;
        }

        boolean match = false;
        for (final Address address : from)
        {
            final String addressStr = address instanceof InternetAddress ? ((InternetAddress) address).getAddress() : address.toString();
            match = match || this.patterns.stream().anyMatch(addressStr::matches);
        }

        return match;
    }

}
