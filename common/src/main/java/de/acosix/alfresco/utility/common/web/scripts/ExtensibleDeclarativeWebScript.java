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
package de.acosix.alfresco.utility.common.web.scripts;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.surf.extensibility.HandlesExtensibility;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.Container;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.FormatRegistry;
import org.springframework.extensions.webscripts.ScriptContent;
import org.springframework.extensions.webscripts.ScriptProcessor;

/**
 * This sub-class fixes some extensibility issues with the base {@link AbstractWebScript} and {@link DeclarativeWebScript} classes.
 *
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class ExtensibleDeclarativeWebScript extends DeclarativeWebScript
{

    // reuse the logger from the base class for overridden functionality
    private static final Logger logger = LoggerFactory.getLogger(AbstractWebScript.class);

    // we need to remember the mimetype from script lookup for later extension lookup
    // (otherwise we'd have to override the entire execute method from DeclarativeWebScript to pass it along)
    protected final ThreadLocal<String> scriptLookupMimetype = new ThreadLocal<>();

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected ScriptDetails getExecuteScript(final String mimetype)
    {
        this.scriptLookupMimetype.set(mimetype);

        return super.getExecuteScript(mimetype);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void executeScript(final ScriptContent location, final Map<String, Object> model)
    {
        final boolean debug = logger.isDebugEnabled();
        long start = 0L;
        if (debug)
        {
            start = System.nanoTime();
        }

        final Container container = this.getContainer();
        final ScriptProcessor scriptProcessor = container.getScriptProcessorRegistry().getScriptProcessor(location);
        scriptProcessor.executeScript(location, model);

        if (container instanceof HandlesExtensibility)
        {
            // main difference from AbstractWebScript handling: we take care of web script format during lookup

            // AbstractWebScript instance variable is not accessible so use the description from which it was initialised
            final String basePath = this.getDescription().getId();

            final FormatRegistry formatRegistry = container.getFormatRegistry();
            String generalizedMimetype = this.scriptLookupMimetype.get();
            while (generalizedMimetype != null)
            {
                final String format = formatRegistry.getFormat(null, generalizedMimetype);
                if (format != null)
                {
                    final String expectedPath = basePath + "." + format;
                    this.executeExtendingScripts(model, expectedPath);
                }

                generalizedMimetype = formatRegistry.generalizeMimetype(generalizedMimetype);
            }

            this.executeExtendingScripts(model, basePath);
        }
    }

    protected void executeExtendingScripts(final Map<String, Object> model, final String basePath)
    {
        final Container container = this.getContainer();
        for (final String moduleScriptPath : ((HandlesExtensibility) container).getExtendingModuleFiles(basePath))
        {
            final String validScriptPath = container.getScriptProcessorRegistry().findValidScriptPath(moduleScriptPath);
            if (validScriptPath != null)
            {
                final ScriptProcessor scriptProcessor = container.getScriptProcessorRegistry().getScriptProcessor(validScriptPath);
                final ScriptContent scriptContent = scriptProcessor.findScript(validScriptPath);
                scriptProcessor.executeScript(scriptContent, model);
            }
        }
    }

    // TODO Find a way to add support for surf.include.resources to customizatoin resource bundles
    // Unfortunately getResources() is final and can't be overriden
}
