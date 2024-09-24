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
package de.acosix.alfresco.utility.core.share.javax.surf;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.ApplicationContext;
import org.springframework.extensions.surf.RequestContext;
import org.springframework.extensions.surf.RequestContextUtil;
import org.springframework.extensions.surf.ServletUtil;
import org.springframework.extensions.surf.support.ThreadLocalRequestContext;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.extensions.webscripts.servlet.WebScriptServletRuntime;

import de.acosix.alfresco.utility.common.function.Callback;

/**
 * Instances of this utility handle the execution of an operation in a correctly initialised request context.
 *
 * @author Axel Faust
 */
public class JavaxRequestContextUtility
{

    /**
     * Executes an operation in an initialised request context.
     *
     * @param applicationContext
     *     the application context for the operation
     * @param scriptReq
     *     the script request
     * @param scriptRes
     *     the script response
     * @param contextGetter
     *     the getter for the currently set request context
     * @param contextSetter
     *     the setter for the request context
     * @param contextUnsetter
     *     the unsetter for the request context
     * @param contextDependentOperation
     *     the operation to execute
     * @throws IOException
     *     if the request context cannot be initialised
     */
    public static void doInRequestContext(final ApplicationContext applicationContext, final WebScriptRequest scriptReq,
            final WebScriptResponse scriptRes, final Supplier<RequestContext> contextGetter, final Consumer<RequestContext> contextSetter,
            final Callback<RuntimeException> contextUnsetter, final Callback<IOException> contextDependentOperation) throws IOException
    {
        boolean handleBinding = false;

        RequestContext rc = null;

        try
        {
            // ensure the request is stored onto the request attributes
            if (ServletUtil.getRequest() == null)
            {
                final HttpServletRequest request = WebScriptServletRuntime.getHttpServletRequest(scriptReq);
                if (request != null)
                {
                    try
                    {
                        rc = RequestContextUtil.initRequestContext(applicationContext, request);
                    }
                    catch (final Exception e)
                    {
                        throw new IOException("Failed to initialize RequestContext for local WebScript runtime", e);
                    }
                }
            }

            // check whether a render context already exists
            RequestContext context = contextGetter.get();
            if (context == null)
            {
                final HttpServletResponse response = WebScriptServletRuntime.getHttpServletResponse(scriptRes);
                if (response != null)
                {
                    context = ThreadLocalRequestContext.getRequestContext();
                    context.setResponse(response);

                    // flag that we will manually handle the bindings
                    handleBinding = true;
                }
            }

            // manually handle binding of RequestContext to current thread
            if (handleBinding)
            {
                contextSetter.accept(context);
            }

            try
            {
                contextDependentOperation.execute();
            }
            finally
            {
                // manually handle unbinding of RequestContext from current thread
                if (handleBinding)
                {
                    contextUnsetter.execute();
                }
            }
        }
        finally
        {
            // unbind RequestContext from current thread
            if (rc != null)
            {
                rc.release();
            }
        }
    }
}
