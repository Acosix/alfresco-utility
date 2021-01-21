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
import java.io.Serializable;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.thumbnail.ThumbnailDefinition;
import org.alfresco.repo.thumbnail.ThumbnailRegistry;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
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
public class RenditionGetWithStatus extends ContentGetWithStatus implements InitializingBean
{

    protected ThumbnailService thumbnailService;

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "thumbnailService", this.thumbnailService);
    }

    /**
     * @param thumbnailService
     *            the thumbnailService to set
     */
    public void setThumbnailService(final ThumbnailService thumbnailService)
    {
        this.thumbnailService = thumbnailService;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void streamContentLocal(final WebScriptRequest req, final WebScriptResponse res, final NodeRef nodeRef, final boolean attach,
            final QName propertyQName, final Map<String, Object> model) throws IOException
    {
        // lookup rendition and adapt parameters for call to super implementation
        final Map<String, String> templateVars = req.getServiceMatch().getTemplateVars();
        final String renditionName = templateVars.get("rendition");

        NodeRef thumbnailNodeRef = this.thumbnailService.getThumbnailByName(nodeRef, propertyQName, renditionName);

        if (thumbnailNodeRef == null)
        {
            final ThumbnailRegistry registry = this.thumbnailService.getThumbnailRegistry();
            final ThumbnailDefinition details = registry.getThumbnailDefinition(renditionName);
            if (details == null)
            {
                throw new WebScriptException(Status.STATUS_NOT_FOUND,
                        "The rendition variant " + renditionName + " has not been defined in the system");
            }

            final Serializable value = this.nodeService.getProperty(nodeRef, ContentModel.PROP_CONTENT);
            final ContentData contentData = DefaultTypeConverter.INSTANCE.convert(ContentData.class, value);

            if (!ContentData.hasContent(contentData))
            {
                throw new WebScriptException(Status.STATUS_BAD_REQUEST,
                        "The node " + nodeRef + " has no content, and thus nothing for which to show a rendition");
            }

            try
            {
                thumbnailNodeRef = AuthenticationUtil.runAsSystem(() -> this.thumbnailService.createThumbnail(nodeRef,
                        ContentModel.PROP_CONTENT, details.getMimetype(), details.getTransformationOptions(), details.getName()));
            }
            catch (final AlfrescoRuntimeException are)
            {
                throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR, "Failed to create rendition", are);
            }
        }

        super.streamContentLocal(req, res, thumbnailNodeRef, attach, ContentModel.PROP_CONTENT, model);
    }
}
