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
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import de.acosix.alfresco.utility.core.repo.acs6.RenditionService2RenditionUtility;
import de.acosix.alfresco.utility.core.repo.acs6.ThumbnailServiceRenditionUtility;

/**
 * @author Axel Faust
 */
public class RenditionInfoWithStatus extends ContentInfoWithStatus implements ApplicationContextAware
{

    private static final boolean THUMBNAIL_SERVICE_RENDITION_UTILITY_AVAILABLE;

    private static final boolean RENDITION_SERVICE_RENDITION_UTILITY_AVAILABLE;
    static
    {
        boolean tsruAvailable = false;
        boolean rsruAvailable = false;
        try
        {
            tsruAvailable = ThumbnailServiceRenditionUtility.isAvailable();
        }
        catch (final Exception ignore)
        {
            // ignored
        }
        try
        {
            rsruAvailable = RenditionService2RenditionUtility.isAvailable();
        }
        catch (final Exception ignore)
        {
            // ignored
        }
        THUMBNAIL_SERVICE_RENDITION_UTILITY_AVAILABLE = tsruAvailable;
        RENDITION_SERVICE_RENDITION_UTILITY_AVAILABLE = rsruAvailable;
    }

    protected ApplicationContext applicationContext;

    /**
     * {@inheritDoc}
     */
    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException
    {
        this.applicationContext = applicationContext;
    }

    // Note: Base class ContentInfo has a bug wherein streamContentImpl is not called, instead regular stream logic is performed
    // this is why we override streamContent
    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void streamContent(final WebScriptRequest req, final WebScriptResponse res, final NodeRef nodeRef, final QName propertyQName,
            final boolean attach, final String attachFileName, final Map<String, Object> model) throws IOException
    {
        final Map<String, String> templateVars = req.getServiceMatch().getTemplateVars();
        final String renditionName = templateVars.get("rendition");

        QName effectivePropertyQName = propertyQName != null ? propertyQName : ContentModel.PROP_CONTENT;
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

        boolean renditionStreamed = false;

        if (RENDITION_SERVICE_RENDITION_UTILITY_AVAILABLE)
        {
            renditionStreamed = RenditionService2RenditionUtility.streamRenditionIfAvailable(this.applicationContext, nodeRef,
                    effectivePropertyQName, renditionName, this.delegate, req, res, attach, attachFileName, model);
        }

        if (!renditionStreamed && THUMBNAIL_SERVICE_RENDITION_UTILITY_AVAILABLE)
        {
            renditionStreamed = ThumbnailServiceRenditionUtility.streamThumbnailIfAvailable(this.applicationContext, nodeRef,
                    effectivePropertyQName, renditionName, this.delegate, req, res, attach, attachFileName, model);
        }

        if (!renditionStreamed)
        {
            boolean renditionAvailable = false;

            if (RENDITION_SERVICE_RENDITION_UTILITY_AVAILABLE)
            {
                renditionAvailable = RenditionService2RenditionUtility.isRenditionPossible(this.applicationContext, nodeRef,
                        effectivePropertyQName, renditionName);
            }

            if (!renditionAvailable && THUMBNAIL_SERVICE_RENDITION_UTILITY_AVAILABLE)
            {
                renditionAvailable = ThumbnailServiceRenditionUtility.isThumbnailPossible(this.applicationContext, nodeRef,
                        effectivePropertyQName, renditionName);
            }

            if (renditionAvailable)
            {
                // rendition is valid in principle, currently does not exist, and maybe re-created
                // tell client to reset cached content
                res.setStatus(Status.STATUS_RESET_CONTENT);
            }
            else
            {
                res.setStatus(Status.STATUS_NOT_FOUND);
            }
        }
    }
}
