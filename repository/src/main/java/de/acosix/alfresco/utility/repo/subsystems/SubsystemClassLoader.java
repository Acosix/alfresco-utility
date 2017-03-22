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
package de.acosix.alfresco.utility.repo.subsystems;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import org.alfresco.util.ParameterCheck;

/**
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class SubsystemClassLoader extends URLClassLoader
{

    protected final ClassLoader parentLoader;

    public SubsystemClassLoader(final ClassLoader parentLoader, final List<URL> urls)
    {
        // parentLoader is not passed to the super class so it does not delegate-first during class loading
        super(urls.toArray(new URL[0]), null);
        this.parentLoader = parentLoader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> loadClass(final String name) throws ClassNotFoundException
    {
        Class<?> c = null;
        try
        {
            c = super.loadClass(name);
        }
        catch (final ClassNotFoundException cnf)
        {
            c = this.parentLoader.loadClass(name);
        }
        return c;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URL getResource(final String name)
    {
        URL r = super.getResource(name);
        if (r == null)
        {
            r = this.parentLoader.getResource(name);
        }
        return r;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Enumeration<URL> getResources(final String name) throws IOException
    {
        final Enumeration<URL> resourcesActual = super.getResources(name);
        final Enumeration<URL> resourcesParent = this.parentLoader.getResources(name);
        return new CompoundURLEnumeration(Arrays.asList(resourcesParent, resourcesActual));
    }

    protected static class CompoundURLEnumeration implements Enumeration<URL>
    {

        private final List<Enumeration<URL>> urlEnumerations;

        private Enumeration<URL> currentEnumeration;

        protected CompoundURLEnumeration(final List<Enumeration<URL>> urlEnumerations)
        {
            ParameterCheck.mandatoryCollection("urlEnumerations", urlEnumerations);
            this.urlEnumerations = new ArrayList<>(urlEnumerations);
            this.currentEnumeration = this.urlEnumerations.remove(0);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasMoreElements()
        {
            this.checkActiveUrlEnumeration();

            final boolean hasMore = this.currentEnumeration != null && this.currentEnumeration.hasMoreElements();
            return hasMore;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public URL nextElement()
        {
            URL nextElement = null;

            this.checkActiveUrlEnumeration();
            if (this.currentEnumeration != null)
            {
                nextElement = this.currentEnumeration.nextElement();
            }

            return nextElement;
        }

        protected void checkActiveUrlEnumeration()
        {
            while (this.currentEnumeration != null && !this.currentEnumeration.hasMoreElements())
            {
                if (!this.urlEnumerations.isEmpty())
                {
                    this.currentEnumeration = this.urlEnumerations.remove(0);
                }
                else
                {
                    this.currentEnumeration = null;
                }
            }
        }

    }
}
