/*
 * Copyright 2016 - 2019 Acosix GmbH
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
package de.acosix.alfresco.utility.common.web.scripts;

import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.webscripts.Description;
import org.springframework.extensions.webscripts.Match;
import org.springframework.extensions.webscripts.UriIndex;
import org.springframework.extensions.webscripts.UriTemplate;
import org.springframework.extensions.webscripts.WebScript;
import org.springframework.extensions.webscripts.WebScriptException;

/**
 * This class provides a slightly improved variant of the Alfresco default {@link org.springframework.extensions.webscripts.JaxRSUriIndex
 * JAXRSUriIndex}. The default class suffers from a lack of "best match" semantics when selecting web scripts based on matching URL
 * patterns, always
 * picking simply the first web scripts which has a matching URI pattern even if there may be other web scripts with far more specific
 * patterns. Since variable match tokens in URI patterns are allowed to match more than one path segment in the URI, a single token can be
 * expanded in such a way that the following URI patterns are considered equal when matched against
 * {@code /acme/api/typeA/1234/typeB/9876/comments/412}:
 * <ul>
 * <li>/acme/api/{primaryEntityType}/{primaryEntityId}/{collectionName}s/{collectionIndex}</li>
 * <li>/acme/api/{primaryEntityType}/{primaryEntityId}/{secondaryEntityType}/{secondaryEntityId}/{collectionName}s/{collectionIndex}</li>
 * </ul>
 * In this example, if the web script for the first URI pattern is first evaluated for a match, it will be picked since {collectionName} can
 * be expanded to match {@code typeB/9876/comment}, thus hiding / suppressing the web script for second URI pattern. Though the default
 * class does employ a {@link TreeMap} and custom {@link Comparator} for being compliant to a JSR-311 mandated lookup priority, this is not
 * working in practice, as the primary sort criterion is based on the assumption that a more specific URI pattern is always composed of more
 * characters than a more generic one, which may not hold true depending on the length of variable names in match tokens.
 *
 * This class is a mostly verbatim copy with minor alterations, necessary since the default class makes use of private- and
 * package-protected visibility modifiers making a simpler, sub-class based patch impossible.
 *
 * @author Axel Faust
 */
public class JaxRSUriIndex implements UriIndex
{

    // Logger
    private static final Logger LOGGER = LoggerFactory.getLogger(JaxRSUriIndex.class);

    // map of web scripts by url
    protected final Map<IndexEntry, IndexEntry> index = new TreeMap<>();

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void clear()
    {
        this.index.clear();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public int getSize()
    {
        return this.index.size();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Match findWebScript(String method, final String uri)
    {
        IndexEntry pathMatch = null;
        Map<String, String> varsMatch = null;
        Match scriptMatch = null;
        final String match = uri;
        String matchNoExt = uri;
        final int extIdx = uri.indexOf('.');
        if (extIdx != -1)
        {
            // format extension is only valid as the last URL element
            if (uri.lastIndexOf('/') < extIdx)
            {
                matchNoExt = uri.substring(0, extIdx);
            }
        }
        method = method.toUpperCase(Locale.ENGLISH);

        // locate full match - on URI and METHOD
        for (final IndexEntry entry : this.index.keySet())
        {
            final String test = entry.getIncludeExtension() ? match : matchNoExt;
            final Map<String, String> vars = entry.getTemplate().match(test);
            if (vars != null)
            {
                pathMatch = entry;
                varsMatch = vars;
                if (entry.getMethod().equals(method))
                {
                    scriptMatch = new Match(entry.getTemplate().getTemplate(), vars, entry.getStaticTemplate(), entry.getScript());
                    break;
                }
            }
        }

        // locate URI match
        if (scriptMatch == null && pathMatch != null)
        {
            scriptMatch = new Match(pathMatch.getTemplate().getTemplate(), varsMatch, pathMatch.getStaticTemplate());
        }

        return scriptMatch;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void registerUri(final WebScript script, String uri)
    {
        final Description desc = script.getDescription();
        boolean extension = true;

        // trim uri parameters
        final int queryArgIdx = uri.indexOf('?');
        if (queryArgIdx != -1)
        {
            uri = uri.substring(0, queryArgIdx);
        }

        // trim extension, only if script distinguishes response format via the extension
        if (desc.getFormatStyle() != Description.FormatStyle.argument)
        {
            final int extIdx = uri.lastIndexOf(".");
            if (extIdx != -1)
            {
                uri = uri.substring(0, extIdx);
            }
            extension = false;
        }

        // index service ensuring no other service has already claimed the url
        final IndexEntry entry = new IndexEntry(desc.getMethod(), new UriTemplate(uri), extension, script);
        if (this.index.containsKey(entry))
        {
            final IndexEntry existingEntry = this.index.get(entry);
            final WebScript existingService = existingEntry.getScript();
            if (!existingService.getDescription().getId().equals(desc.getId()))
            {
                final String msg = "Web Script document " + desc.getDescPath() + " is attempting to define the url '" + entry
                        + "' already defined by " + existingService.getDescription().getDescPath();
                throw new WebScriptException(msg);
            }
        }
        else
        {
            this.index.put(entry, entry);
            LOGGER.trace("Indexed URI '{}' as '{}'", uri, entry.getTemplate());
        }
    }

    /**
     * URI Index Entry
     *
     * @author davidc
     */
    protected static class IndexEntry implements Comparable<IndexEntry>
    {

        private final String method;

        private final UriTemplate template;

        private final WebScript script;

        private final boolean includeExtension;

        private final String staticTemplate;

        private final String key;

        private final int pathElementCount;

        /**
         * Construct
         *
         * @param method
         *            http method
         * @param template
         *            uri template
         * @param includeExtension
         *            include uri extension in index
         * @param script
         *            associated web script
         */
        protected IndexEntry(final String method, final UriTemplate template, final boolean includeExtension, final WebScript script)
        {
            this.method = method.toUpperCase(Locale.ENGLISH);
            this.template = template;
            this.includeExtension = includeExtension;
            this.script = script;
            this.key = template.getRegex() + ":" + this.method;
            final int firstTokenIdx = template.getTemplate().indexOf('{');
            this.staticTemplate = (firstTokenIdx == -1) ? template.getTemplate() : template.getTemplate().substring(0, firstTokenIdx);

            final String rawTemplate = template.getTemplate();
            this.pathElementCount = rawTemplate.substring(rawTemplate.startsWith("/") ? 1 : 0, rawTemplate.length()).split("/").length;
        }

        /**
         * @return http method
         */
        public String getMethod()
        {
            return this.method;
        }

        /**
         * @return uri template
         */
        public UriTemplate getTemplate()
        {
            return this.template;
        }

        /**
         * @return static prefix of uri template
         */
        public String getStaticTemplate()
        {
            return this.staticTemplate;
        }

        /**
         * @return includes uri extension in index
         */
        public boolean getIncludeExtension()
        {
            return this.includeExtension;
        }

        /**
         * @return associated web script
         */
        public WebScript getScript()
        {
            return this.script;
        }

        /**
         * @return the count of URI path elements in the raw template
         */
        public int getPathElementCount()
        {
            return this.pathElementCount;
        }

        /**
         *
         * {@inheritDoc}
         */
        @Override
        public final String toString()
        {
            return this.key;
        }

        /**
         *
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj)
        {
            if (!(obj instanceof IndexEntry))
            {
                return false;
            }
            return this.key.equals(((IndexEntry) obj).key);
        }

        /**
         *
         * {@inheritDoc}
         */
        @Override
        public int hashCode()
        {
            return this.key.hashCode();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int compareTo(final IndexEntry other)
        {
            int result;

            if (other == null)
            {
                result = -1;
            }
            // primary key: order by number of explicit path elements in URI template
            else if (this.pathElementCount != other.getPathElementCount())
            {
                result = other.getPathElementCount() - this.pathElementCount;
            }
            // primary key in default, now tertiary key: order by number of static characters in URI template
            else if (this.template.getStaticCharCount() != other.getTemplate().getStaticCharCount())
            {
                result = other.getTemplate().getStaticCharCount() - this.template.getStaticCharCount();
            }
            else
            {
                // order by uri template regular expression
                result = other.getTemplate().getRegex().pattern().compareTo(this.template.getRegex().pattern());

                if (result == 0)
                {
                    // implementation specific: order by http method
                    result = other.getMethod().compareTo(this.method);
                }
            }

            return result;
        }
    }
}
