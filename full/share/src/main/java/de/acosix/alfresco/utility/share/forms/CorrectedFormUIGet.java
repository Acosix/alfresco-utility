/*
 * Copyright 2016 - 2020 Acosix GmbH
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

import java.util.List;

import org.alfresco.web.config.forms.FormSet;
import org.alfresco.web.scripts.forms.FormUIGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This specialisation of the default web script corrects minor bugs.
 *
 * @author Axel Faust
 */
public class CorrectedFormUIGet extends FormUIGet
{

    private static final Logger LOGGER = LoggerFactory.getLogger(FormUIGet.class);

    /**
     *
     * {@inheritDoc}
     */
    // almost completely copied from base class except for childSet != null check
    @Override
    protected Set generateSetModelUsingVisibleFields(final ModelContext context, final FormSet setConfig)
    {
        Set set = null;

        final List<String> fieldsInSet = this.getVisibleFieldsInSet(context, setConfig);

        // if there is something to show in the set create the set object
        if ((fieldsInSet != null && fieldsInSet.size() > 0) || setConfig.getChildrenAsList().size() > 0)
        {
            set = this.generateSetModel(context, setConfig, fieldsInSet);

            // recursively setup child sets
            for (final FormSet childSetConfig : setConfig.getChildrenAsList())
            {
                final Set childSet = this.generateSetModelUsingVisibleFields(context, childSetConfig);
                if (childSet != null)
                {
                    set.addChild(childSet);
                }
            }
        }
        else
        {
            LOGGER.debug("Ignoring set \"{}\" as it does not have any fields or child sets", setConfig.getSetId());
        }

        return set;
    }
}
