/*
 * Copyright 2019 Acosix GmbH
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

import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.transform.ContentTransformer;
import org.alfresco.repo.thumbnail.ThumbnailDefinition;
import org.alfresco.repo.thumbnail.ThumbnailRegistry;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.thumbnail.ThumbnailService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.PropertyCheck;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

/**
 * @author Axel Faust
 */
public class RenditionInfoWithStatus extends ContentInfoWithStatus implements InitializingBean
{

    protected ContentService contentService;

    protected ThumbnailService thumbnailService;

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "contentService", this.contentService);
        PropertyCheck.mandatory(this, "thumbnailService", this.thumbnailService);
    }

    /**
     * @param contentService
     *            the contentService to set
     */
    public void setContentService(final ContentService contentService)
    {
        this.contentService = contentService;
    }

    /**
     * @param thumbnailService
     *            the thumbnailService to set
     */
    public void setThumbnailService(final ThumbnailService thumbnailService)
    {
        this.thumbnailService = thumbnailService;
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
                throw new WebScriptException(HttpServletResponse.SC_BAD_REQUEST, "Content property malformed");
            }
            final String propertyName = contentPart.substring(1);
            if (propertyName.length() > 0)
            {
                effectivePropertyQName = QName.createQName(propertyName, this.namespaceService);
            }
        }

        final NodeRef thumbnailNodeRef = this.thumbnailService.getThumbnailByName(nodeRef, effectivePropertyQName, renditionName);

        if (thumbnailNodeRef == null)
        {
            final ThumbnailRegistry registry = this.thumbnailService.getThumbnailRegistry();
            final ThumbnailDefinition details = registry.getThumbnailDefinition(renditionName);
            if (details == null)
            {
                throw new WebScriptException(Status.STATUS_NOT_FOUND,
                        "The rendition variant " + renditionName + " has not been defined in the system");
            }

            final ContentReader reader = this.contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
            if (reader != null && reader.exists())
            {
                final ContentTransformer transformer = this.contentService.getTransformer(reader.getContentUrl(), reader.getMimetype(),
                        reader.getSize(), details.getMimetype(), details.getTransformationOptions());
                if (transformer != null)
                {
                    // thumbnail is valid in principle, currently does not exist, and maybe re-created
                    // tell client to reset cached content
                    res.setStatus(Status.STATUS_RESET_CONTENT);
                }
                else
                {
                    res.setStatus(Status.STATUS_NOT_FOUND);
                }
            }
            else
            {
                res.setStatus(Status.STATUS_NOT_FOUND);
            }
        }
        else
        {
            this.delegate.streamContent(req, res, thumbnailNodeRef, ContentModel.PROP_CONTENT, attach, attachFileName, model);
        }
    }
}
