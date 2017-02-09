/*
 * Copyright 2016, 2017 Acosix GmbH
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
package de.acosix.alfresco.utility.share.surf;

import java.util.regex.Pattern;

import org.alfresco.util.PropertyCheck;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.extensions.surf.DojoCssDependencyRule;
import org.springframework.extensions.surf.DojoDependencies;

/**
 * This special rule implementation will attempt to parse and process the declarative widget model of a page as JSON before falling back to
 * the less efficient and more error prone default RegEx evaluation.
 *
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class JSONAwareCssDependencyRule extends DojoCssDependencyRule implements InitializingBean
{

    private static final String CSS_FILE = "cssFile";

    private static final String MEDIA_TYPE = "mediaType";

    private static final Logger LOGGER = LoggerFactory.getLogger(JSONAwareCssDependencyRule.class);

    protected String cssDependencyKeyRegex = "^(cssRequirements)$";

    protected Pattern cssDependencyKeyPattern;

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "cssDependencyKeyRegex", this.cssDependencyKeyRegex);
        this.cssDependencyKeyPattern = Pattern.compile(this.cssDependencyKeyRegex);
    }

    /**
     * @param cssDependencyKeyRegex
     *            the cssDependencyKeyRegex to set
     */
    public void setCssDependencyKeyRegex(final String cssDependencyKeyRegex)
    {
        this.cssDependencyKeyRegex = cssDependencyKeyRegex;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void processRegexRules(final String filePath, final String fileContents, final DojoDependencies dependencies)
    {
        if (filePath == null)
        {
            Object jsonModel = null;
            try
            {
                jsonModel = this.tryParseAsJSON(fileContents);
            }
            catch (final JSONException e)
            {
                LOGGER.debug("Failed to parse potential JSON model - falling back to regex evaluation", e);
            }

            if (!(jsonModel instanceof JSONObject || jsonModel instanceof JSONArray))
            {
                super.processRegexRules(null, fileContents, dependencies);
            }
            else
            {
                if (jsonModel instanceof JSONObject)
                {
                    this.processModelImpl((JSONObject) jsonModel, dependencies, fileContents);
                }
                else
                {
                    this.processModelsImpl((JSONArray) jsonModel, dependencies, fileContents);
                }
            }
        }
        else
        {
            super.processRegexRules(filePath, fileContents, dependencies);
        }
    }

    protected Object tryParseAsJSON(final String fileContents) throws JSONException
    {
        final Object json;

        if (fileContents.startsWith("{"))
        {
            json = new JSONObject(new JSONTokener(fileContents));
        }
        else if (fileContents.startsWith("["))
        {
            json = new JSONArray(new JSONTokener(fileContents));
        }
        else
        {
            LOGGER.debug("Unable to determine if JSON should be parsed as array or object");
            json = null;
        }

        return json;
    }

    protected void processModelImpl(final JSONObject jsonModel, final DojoDependencies dependencies, final String fileContents)
    {
        try
        {
            final String[] names = JSONObject.getNames(jsonModel);
            // null as return value for empty object is evil - should've been empty array
            if (names != null)
            {
                for (final String key : names)
                {
                    final Object value = jsonModel.get(key);

                    if (this.cssDependencyKeyPattern.matcher(key).matches())
                    {
                        if (value instanceof JSONObject)
                        {
                            this.processSingleDependencyImpl((JSONObject) value, dependencies, fileContents);
                        }
                        else if (value instanceof JSONArray)
                        {
                            this.processMultiDependenciesImpl((JSONArray) value, dependencies, fileContents);
                        }
                    }
                    else
                    {
                        if (value instanceof JSONObject)
                        {
                            this.processModelImpl((JSONObject) value, dependencies, fileContents);
                        }
                        else if (value instanceof JSONArray)
                        {
                            this.processModelsImpl((JSONArray) value, dependencies, fileContents);
                        }
                    }
                }
            }
        }
        catch (final JSONException e)
        {
            LOGGER.warn("Error processing JSON model - falling back to regex rules", e);
            super.processRegexRules(null, fileContents, dependencies);
        }
    }

    protected void processModelsImpl(final JSONArray jsonModels, final DojoDependencies dependencies, final String fileContents)
    {
        try
        {
            for (int idx = 0; idx < jsonModels.length(); idx++)
            {
                final Object element = jsonModels.get(idx);
                if (element instanceof JSONObject)
                {
                    this.processModelImpl((JSONObject) element, dependencies, fileContents);
                }
                else if (element instanceof JSONArray)
                {
                    this.processModelsImpl((JSONArray) element, dependencies, fileContents);
                }
            }
        }
        catch (final JSONException e)
        {
            LOGGER.warn("Error processing JSON models - falling back to regex rules", e);
            super.processRegexRules(null, fileContents, dependencies);
        }
    }

    protected void processMultiDependenciesImpl(final JSONArray dependenciesModel, final DojoDependencies dependencies,
            final String fileContents)
    {
        for (int idx = 0; idx < dependenciesModel.length(); idx++)
        {
            final JSONObject dependencyModel = dependenciesModel.optJSONObject(idx);
            if (dependencyModel != null)
            {
                this.processSingleDependencyImpl(dependencyModel, dependencies, fileContents);
            }
        }
    }

    protected void processSingleDependencyImpl(final JSONObject dependencyModel, final DojoDependencies dependencies,
            final String fileContents)
    {
        // TODO Do we want to make cssFile / mediaType key names configurable? Unlikely to be different than these defaults
        final String cssFile = dependencyModel.optString(CSS_FILE);
        String mediaType = dependencyModel.optString(MEDIA_TYPE);
        if (mediaType == null || mediaType.trim().isEmpty())
        {
            mediaType = "screen";
        }

        final String cssPath = this.getDojoDependencyHandler().getPath(null, cssFile);
        dependencies.addCssDep(cssPath, mediaType);
    }
}