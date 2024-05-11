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
package de.acosix.alfresco.utility.core.repo.acs6;

import java.io.IOException;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.thumbnail.ThumbnailDefinition;
import org.alfresco.repo.thumbnail.ThumbnailRegistry;
import org.alfresco.repo.web.scripts.content.ContentStreamer;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.thumbnail.ThumbnailService;
import org.alfresco.service.namespace.QName;
import org.springframework.context.ApplicationContext;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

/**
 * @author Axel Faust
 */
public class ThumbnailServiceRenditionUtility
{

    private static final boolean CONTENT_SERVICE_TRANSFORMER_CHECK_AVAILABLE;
    static
    {
        boolean ctcAvailable = false;
        try
        {
            ctcAvailable = ContentServiceTransformerUtility.isAvailable();
        }
        catch (final Exception ignore)
        {
            // ignored
        }
        CONTENT_SERVICE_TRANSFORMER_CHECK_AVAILABLE = ctcAvailable;
    }

    /**
     * Checks whether the necessary APIs are available to invoke this utility. Note that the caller is still required to handle any
     * {@link NoClassDefFoundError classloading errors} to be safe.
     *
     * @return {@code true} if the API is available, {@code false} otherwise
     */
    public static boolean isAvailable()
    {
        boolean available = false;
        try
        {
            Class.forName("org.alfresco.service.cmr.thumbnail.ThumbnailService");
            available = true;
        }
        catch (final Exception ignore)
        {
            // ignored
        }

        return available;
    }

    /**
     * Streams a thumbnail of a node if available.
     *
     * @param applicationContext
     *     the application context from which to resolve service instances
     * @param nodeRef
     *     the reference to the node for which to lookup and stream a thumbnail
     * @param effectivePropertyQName
     *     the qualified name of the content property for which the thumbnail should be streamed
     * @param thumbnailName
     *     the name of the thumbnail to stream
     * @param delegate
     *     the content streamer delegate
     * @param req
     *     the web script request
     * @param res
     *     the web script response
     * @param attach
     *     whether the thumbnail's content should be streamed with the attachment disposition or not
     * @param attachFileName
     *     the file name to use when streaming with attachment disposition
     * @param model
     *     the web script response model
     *
     * @return {@code true} if the thumbnail was streamed, {@code false} otherwise
     */
    public static boolean streamThumbnailIfAvailable(final ApplicationContext applicationContext, final NodeRef nodeRef,
            final QName effectivePropertyQName, final String thumbnailName, final ContentStreamer delegate, final WebScriptRequest req,
            final WebScriptResponse res, final boolean attach, final String attachFileName, final Map<String, Object> model)
            throws IOException
    {
        final ThumbnailService thumbnailService = applicationContext.getBean("ThumbnailService", ThumbnailService.class);
        final NodeRef thumbnailNodeRef = thumbnailService.getThumbnailByName(nodeRef, effectivePropertyQName, thumbnailName);
        if (thumbnailNodeRef != null)
        {
            delegate.streamContent(req, res, thumbnailNodeRef, ContentModel.PROP_CONTENT, attach, attachFileName, model);
            return true;
        }
        return false;
    }

    /**
     * Checks if a specific thumbnail of a node if possible to be created.
     *
     * @param applicationContext
     *     the application context from which to resolve service instances
     * @param nodeRef
     *     the reference to the node for which to check the thumbnail potential
     * @param effectivePropertyQName
     *     the qualified name of the content property for which the thumbnail potential should be checked
     * @param thumbnailName
     *     the name of the thumbnail to check
     *
     * @return {@code true} if the thumbnail is possible, {@code false} otherwise
     */
    public static boolean isThumbnailPossible(final ApplicationContext applicationContext, final NodeRef nodeRef,
            final QName effectivePropertyQName, final String thumbnailName)
    {
        boolean possible = false;
        if (CONTENT_SERVICE_TRANSFORMER_CHECK_AVAILABLE)
        {
            final ThumbnailService thumbnailService = applicationContext.getBean("ThumbnailService", ThumbnailService.class);
            final ThumbnailRegistry registry = thumbnailService.getThumbnailRegistry();
            final ThumbnailDefinition details = registry.getThumbnailDefinition(thumbnailName);

            if (details != null)
            {
                final ContentService contentService = applicationContext.getBean("ContentSerivce", ContentService.class);
                final ContentReader reader = contentService.getReader(nodeRef, effectivePropertyQName);
                if (reader != null && reader.exists())
                {
                    possible = ContentServiceTransformerUtility.hasTransformer(applicationContext, reader.getContentUrl(),
                            reader.getMimetype(), reader.getSize(), details.getMimetype(), details.getTransformationOptions());
                }
            }
        }

        return possible;
    }
}
