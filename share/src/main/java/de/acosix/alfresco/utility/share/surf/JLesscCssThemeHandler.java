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
package de.acosix.alfresco.utility.share.surf;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.config.WebFrameworkConfigElement;
import org.springframework.extensions.surf.CssThemeHandler;
import org.springframework.extensions.surf.RequestContext;
import org.springframework.extensions.surf.support.ThreadLocalRequestContext;
import org.springframework.extensions.surf.types.Theme;
import org.springframework.extensions.webscripts.ScriptConfigModel;

import com.inet.lib.less.Less;
import com.inet.lib.less.LessException;

/**
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class JLesscCssThemeHandler extends CssThemeHandler
{

    public static final String LESS_TOKEN = "less-variables";

    private static final Logger LOGGER = LoggerFactory.getLogger(JLesscCssThemeHandler.class);

    private static final String LESS_PROCESSED_MARKER = "/* LESS-processed */";

    protected final Map<RequestContext, Map<String, String>> tokensMap = new WeakHashMap<>();

    /**
     * The default LESS configuration. This will be populated with the contents of a file referenced by the
     * web-framework &gt; defaults &gt; dojo-pages &gt; default-less-configuration.
     */
    protected String defaultLessConfig = null;

    /**
     * Returns the current default LESS configuration. If it has not previously been retrieved then it will
     * attempt to load it.
     *
     * @return A String containing the default LESS configuration variables.
     */
    public String getDefaultLessConfig()
    {
        final RequestContext rc = ThreadLocalRequestContext.getRequestContext();
        if (this.defaultLessConfig == null)
        {
            String defaultLessConfigPath = null;
            final ScriptConfigModel config = rc.getExtendedScriptConfigModel(null);
            final Map<?, ?> configs = (Map<?, ?>) config.getScoped().get("WebFramework");
            if (configs != null)
            {
                final WebFrameworkConfigElement wfce = (WebFrameworkConfigElement) configs.get("web-framework");
                defaultLessConfigPath = wfce.getDojoDefaultLessConfig();
            }
            else
            {
                defaultLessConfigPath = this.getWebFrameworkConfigElement().getDojoDefaultLessConfig();
            }
            try
            {
                final InputStream in = this.getDependencyHandler().getResourceInputStream(defaultLessConfigPath);
                if (in != null)
                {
                    this.defaultLessConfig = this.getDependencyHandler().convertResourceToString(in);
                }
                else
                {
                    LOGGER.error("Could not find the default LESS configuration at: {}", defaultLessConfigPath);
                    // Set the configuration as the empty string as it's not in the configured location
                    this.defaultLessConfig = "";
                }
            }
            catch (final IOException e)
            {
                LOGGER.error("An exception occurred retrieving the default LESS configuration from: {}", defaultLessConfigPath, e);
            }
        }
        return this.defaultLessConfig;
    }

    /**
     * Looks for the LESS CSS token which should contain the LESS style variables that
     * can be applied to each CSS file. This will be prepended to each CSS file processed.
     *
     * @return The String of LESS variables.
     */
    public String getLessVariables()
    {
        String variables = this.getDefaultLessConfig();
        Theme currentTheme = ThreadLocalRequestContext.getRequestContext().getTheme();
        if (currentTheme == null)
        {
            currentTheme = ThreadLocalRequestContext.getRequestContext().getObjectService().getTheme("default");
        }
        final String themeVariables = currentTheme.getCssTokens().get(JLesscCssThemeHandler.LESS_TOKEN);
        if (themeVariables != null)
        {
            variables += "\n" + themeVariables;
        }
        return variables;
    }

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

    /**
     * Overrides the default implementation to add LESS processing capabilities.
     *
     * @param path
     *            The path of the file being processed (used only for error output)
     * @param cssContents
     *            The CSS to process
     * @throws IOException
     *             when accessing file contents.
     */
    @Override
    public String processCssThemes(final String path, final StringBuilder cssContents) throws IOException
    {
        String compiledCss = null;
        // may be called on already processed CSS contents
        if (cssContents.indexOf(LESS_PROCESSED_MARKER) != 0)
        {
            LOGGER.debug("Processing CSS file {}", path);

            final String intialProcessResults = super.processCssThemes(path, cssContents);
            final String lessVariables = this.getLessVariables();
            final String fullCSS = lessVariables + intialProcessResults;
            try
            {
                compiledCss = LESS_PROCESSED_MARKER + "\n\n" + Less.compile(null, fullCSS, false);
            }
            catch (final LessException e)
            {
                LOGGER.error("Error processing CSS file", e);
                compiledCss = "/*JLessC error compiling: '" + path + "': " + e.getMessage() + "*/\n\n " + cssContents;
            }
        }
        else
        {
            compiledCss = cssContents.toString();
        }
        return compiledCss;
    }

}
