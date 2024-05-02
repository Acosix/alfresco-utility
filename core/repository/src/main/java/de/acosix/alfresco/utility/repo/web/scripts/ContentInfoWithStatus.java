/*
 * Copyright 2016 - 2021 Acosix GmbH
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

import org.alfresco.repo.web.scripts.content.ContentInfo;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.PropertyCheck;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

/**
 * @author Axel Faust
 */
public class ContentInfoWithStatus extends ContentInfo implements InitializingBean
{
    // Note: Base class ContentInfo has a bug wherein streamContentImpl is not called, instead regular stream logic is performed

    protected NamespaceService namespaceService;

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "namespaceService", this.namespaceService);
    }

    /**
     * @param namespaceService
     *            the namespaceService to set
     */
    public void setNamespaceService(final NamespaceService namespaceService)
    {
        this.namespaceService = namespaceService;
    }

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
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void streamContent(final WebScriptRequest req, final WebScriptResponse res, final NodeRef nodeRef, final QName propertyQName,
            final boolean attach, final String attachFileName, final Map<String, Object> model) throws IOException
    {
        // bug in base class: "property" template arg is not used

        final Map<String, String> templateVars = req.getServiceMatch().getTemplateVars();

        QName effectivePropertyQName = propertyQName;
        final String contentPart = templateVars.get("property");
        if (contentPart != null && !contentPart.trim().isEmpty() && contentPart.charAt(0) == ';')
        {
            if (contentPart.length() < 2)
            {
                throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Content property malformed");
            }
            final String propertyName = contentPart.substring(1);
            if (propertyName.length() > 0)
            {
                effectivePropertyQName = QName.createQName(propertyName, this.namespaceService);
            }
        }

        super.streamContent(req, res, nodeRef, effectivePropertyQName, attach, attachFileName, model);
    }
}
