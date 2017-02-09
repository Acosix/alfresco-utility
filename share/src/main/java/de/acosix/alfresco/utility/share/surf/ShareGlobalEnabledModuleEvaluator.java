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

import java.util.Map;

import org.springframework.extensions.surf.RequestContext;
import org.springframework.extensions.surf.extensibility.ExtensionModuleEvaluator;

/**
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class ShareGlobalEnabledModuleEvaluator implements ExtensionModuleEvaluator
{

    protected boolean enabled;

    /**
     * @param enabled
     *            the enabled to set
     */
    public void setEnabled(final boolean enabled)
    {
        this.enabled = enabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean applyModule(final RequestContext context, final Map<String, String> evaluationProperties)
    {
        return this.enabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getRequiredProperties()
    {
        return new String[0];
    }

}
