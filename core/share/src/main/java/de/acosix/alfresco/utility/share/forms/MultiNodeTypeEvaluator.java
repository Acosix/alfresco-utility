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
package de.acosix.alfresco.utility.share.forms;

import java.util.Arrays;
import java.util.List;

import org.alfresco.web.config.forms.NodeMetadataBasedEvaluator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * This evaluator implementation supports the evaluation of a node's type against a list of node types (separated by semi-colons) in the
 * condition of a config section.
 *
 * @author Axel Faust
 */
public class MultiNodeTypeEvaluator extends NodeMetadataBasedEvaluator
{

    // ugh, Commons Logging
    private static final Log LEGACY_LOGGER = LogFactory.getLog(MultiNodeTypeEvaluator.class);

    protected static final String JSON_TYPE = "type";

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected Log getLogger()
    {
        return LEGACY_LOGGER;
    }

    /**
     * This method checks if the specified condition is matched by the node type
     * within the specified jsonResponse String.
     *
     * @return true if the node type matches the condition, else false.
     */
    @Override
    protected boolean checkJsonAgainstCondition(final String condition, final String jsonResponseString)
    {
        boolean result = false;
        try
        {
            final JSONObject json = new JSONObject(new JSONTokener(jsonResponseString));
            Object typeObj = null;
            if (json.has(JSON_TYPE))
            {
                typeObj = json.get(JSON_TYPE);
            }

            if (typeObj instanceof String)
            {
                final String typeString = (String) typeObj;
                final List<String> conditionTypes = Arrays.asList(condition.split(";"));
                result = conditionTypes.contains(typeString);
            }
        }
        catch (final JSONException e)
        {
            if (this.getLogger().isWarnEnabled())
            {
                this.getLogger().warn("Failed to find node type in JSON response from metadata service: " + e.getMessage(), e);
            }
        }
        return result;
    }
}
