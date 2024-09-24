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
package de.acosix.alfresco.utility.share.surf;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.surf.extensibility.ExtensibilityModel;
import org.springframework.extensions.webscripts.Authenticator;
import org.springframework.extensions.webscripts.Description;
import org.springframework.extensions.webscripts.Description.RequiredAuthentication;
import org.springframework.extensions.webscripts.LocalWebScriptRuntimeContainer;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScript;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import de.acosix.alfresco.utility.common.function.Callback;
import de.acosix.alfresco.utility.core.share.jakarta.surf.JakartaRequestContextUtility;
import de.acosix.alfresco.utility.core.share.javax.surf.JavaxRequestContextUtility;

/**
 * This sub-class addresses concerns with regards to extensibility handling, specifically thread-safety of its suppression as well as
 * {@link WebScriptResponse#getOutputStream() output stream} vss {@link WebScriptResponse#getWriter() writer} use, which can cause issues
 * with streaming web scripts (see <a href="https://issues.alfresco.com/jira/browse/ALF-21949">ALF-21949</a>).
 *
 * @author Axel Faust
 */
public class ExtensibilityFixedLocalWebScriptRuntimeContainer extends LocalWebScriptRuntimeContainer
{

    private static final boolean USE_JAVAX_CONTEXT_UTILITY;
    static
    {
        boolean useJavaxUtility = false;
        try
        {
            Class.forName("jakarta.servlet.http.HttpServletRequest");
        }
        catch (final ClassNotFoundException cnf)
        {
            useJavaxUtility = true;
        }
        USE_JAVAX_CONTEXT_UTILITY = useJavaxUtility;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtensibilityFixedLocalWebScriptRuntimeContainer.class);

    /**
     * This keeps track of wether the application of extensibility has been suppressed for the current thread.
     */
    protected final ThreadLocal<Boolean> extensibilitySuppressed = new ThreadLocal<>();

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void suppressExtensibility()
    {
        this.extensibilitySuppressed.set(Boolean.TRUE);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void unsuppressExtensibility()
    {
        this.extensibilitySuppressed.set(Boolean.FALSE);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isExtensibilitySuppressed()
    {
        final boolean extensibilityIsSuppressed = Boolean.TRUE.equals(this.extensibilitySuppressed.get());
        return extensibilityIsSuppressed;
    }

    // copied almost verbatim from spring-surf 6.12
    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void executeScript(final WebScriptRequest scriptReq, final WebScriptResponse scriptRes, final Authenticator auth)
            throws IOException
    {
        final Callback<IOException> cb = () -> {
            // call through to the parent container to perform the WebScript processing
            final ExtensibilityModel extModel = this.openExtensibilityModel();
            boolean exceptionOccurred = false;
            try
            {
                this.executeScriptImpl(scriptReq, scriptRes, auth);
            }
            catch (final Exception e)
            {
                LOGGER.debug(
                        "{} occurred during script execution - not closing extensibility model and thus not flushing response (relegated to container status handling)",
                        e.getClass());
                exceptionOccurred = true;
                if (e instanceof RuntimeException || e instanceof IOException)
                {
                    throw e;
                }
                throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR, "Unexpected error", e);
            }
            finally
            {
                // It's only necessary to close the model if it's actually been used. Not all WebScripts will make use of the
                // model. An example of this would be the StreamContent WebScript. It is important not to attempt to close
                // an unused model since the WebScript executed may have already flushed the response if it has overridden
                // the default .execute() method.
                if (!exceptionOccurred && extModel.isModelStarted())
                {
                    this.closeExtensibilityModel(extModel, scriptRes.getWriter());
                }
            }
        };

        if (USE_JAVAX_CONTEXT_UTILITY)
        {
            JavaxRequestContextUtility.doInRequestContext(this.applicationContext, scriptReq, scriptRes, this::getRequestContext,
                    this::bindRequestContext, this::unbindRequestContext, cb);
        }
        else
        {
            JakartaRequestContextUtility.doInRequestContext(this.applicationContext, scriptReq, scriptRes, this::getRequestContext,
                    this::bindRequestContext, this::unbindRequestContext, cb);
        }
    }

    // copied from PresentationContainer because we cannot skip base class executeScript via super keyword
    protected void executeScriptImpl(final WebScriptRequest scriptReq, final WebScriptResponse scriptRes, final Authenticator auth)
            throws IOException
    {
        // Handle authentication of scripts on a case-by-case basis.
        // Currently we assume that if a webscript servlet has any authenticator
        // applied then it must be for some kind of remote user auth as supplied.
        final WebScript script = scriptReq.getServiceMatch().getWebScript();
        script.setURLModelFactory(this.getUrlModelFactory());
        final Description desc = script.getDescription();
        final RequiredAuthentication required = desc.getRequiredAuthentication();
        if (auth == null || RequiredAuthentication.none == required || auth.authenticate(required, false))
        {
            script.execute(scriptReq, scriptRes);
        }
    }
}
