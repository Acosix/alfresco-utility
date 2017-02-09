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

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.springframework.extensions.surf.HybridCssThemeHandler;
import org.springframework.extensions.surf.RequestContext;
import org.springframework.extensions.surf.support.ThreadLocalRequestContext;
import org.springframework.extensions.surf.types.Theme;

/**
 * This class provides an improved CSS theme handler implementation that does not allow CSS / LESS tokens to be reused across different
 * request contexts with potentially different customizations applied to them. Such behaviour is a bug in the default implementation and can
 * cause spill-over of tokens from Surf extension modules / customizations into default themes.
 *
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class StateSafeHybridCssThemeHandler extends HybridCssThemeHandler
{

    protected final Map<RequestContext, Map<String, String>> tokensMap = new WeakHashMap<>();

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void determineThemeTokens()
    {
        // NO-OP - it is only called from base class
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getTokenMap()
    {
        Map<String, String> cssTokens;

        // tokensMap keyed by request context which may have different customizations applied to it than previous contexts
        final RequestContext rc = ThreadLocalRequestContext.getRequestContext();
        cssTokens = this.tokensMap.get(rc);
        if (cssTokens == null)
        {
            Theme currentTheme = rc.getTheme();
            if (currentTheme == null)
            {
                currentTheme = rc.getObjectService().getTheme("default");
            }
            // obtain theme tokens (previously handled by determineThemeTokens in base class)
            cssTokens = currentTheme.getCssTokens();
            this.tokensMap.put(rc, cssTokens);
        }

        // do not expose modifiable internal state
        return Collections.unmodifiableMap(cssTokens);
    }
}
