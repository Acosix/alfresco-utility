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
package de.acosix.alfresco.utility.repo.web.scripts;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.repo.web.scripts.content.ContentGet;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

/**
 * @author Axel Faust
 */
public class ContentGetWithStatus extends ContentGet
{

    protected final ThreadLocal<Boolean> streamCalled = new ThreadLocal<>();

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void execute(final WebScriptRequest req, final WebScriptResponse res) throws IOException
    {
        try
        {
            super.execute(req, res);
        }
        catch (final WebScriptException wsex)
        {
            if (Boolean.TRUE.equals(this.streamCalled.get()))
            {
                // bubble up - we can't just do normal status handling anymore since content may have already been streamed to the response
                throw wsex;
            }

            final Status status = new Status();
            status.setCode(wsex.getStatus(), wsex.getMessage());
            status.setException(wsex);

            final Cache cache = new Cache(this.getDescription().getRequiredCache());

            final Map<String, Object> model = new HashMap<>();
            model.put("cache", cache);
            model.put("status", status);

            model.put("templateVars", req.getServiceMatch().getTemplateVars());

            final Map<String, Object> templateModel = this.createTemplateParameters(req, res, model);
            String format = req.getFormat();
            if (format == null || format.trim().isEmpty())
            {
                format = "html";
            }
            this.sendStatus(req, res, status, cache, format, templateModel);
        }
        finally
        {
            this.streamCalled.remove();
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void streamContentLocal(final WebScriptRequest req, final WebScriptResponse res, final NodeRef nodeRef, final boolean attach,
            final QName propertyQName, final Map<String, Object> model) throws IOException
    {
        this.streamCalled.set(Boolean.TRUE);
        super.streamContentLocal(req, res, nodeRef, attach, propertyQName, model);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void streamContent(final WebScriptRequest req, final WebScriptResponse res, final NodeRef nodeRef, final QName propertyQName,
            final boolean attach, final String attachFileName, final Map<String, Object> model) throws IOException
    {
        final Map<String, String> templateArgs = req.getServiceMatch().getTemplateVars();
        // in case web script is ever mapped to a URL with an explicit file name token, that should override the regular node name
        final String effectiveFileName = templateArgs.getOrDefault("filename", attachFileName);
        this.delegate.streamContent(req, res, nodeRef, propertyQName, attach, effectiveFileName, model);
    }
}
