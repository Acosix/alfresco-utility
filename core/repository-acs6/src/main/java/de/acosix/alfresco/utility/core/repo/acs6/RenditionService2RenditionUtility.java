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
package de.acosix.alfresco.utility.core.repo.acs6;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Predicate;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.rendition2.RenditionDefinition2;
import org.alfresco.repo.rendition2.RenditionService2;
import org.alfresco.repo.web.scripts.content.ContentStreamer;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.springframework.context.ApplicationContext;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

/**
 * @author Axel Faust
 */
public class RenditionService2RenditionUtility
{

    private static final Method IS_ENABLED_METHOD;

    private static final Method IS_SUPPORTED_METHOD;

    static
    {
        Class<?> registryInterfaceClass = null;
        Class<?> registryBaseClass = null;
        // depending on ACS version, the base class differs in its qualified name
        for (final String className : Arrays.asList("org.alfresco.transform.client.model.config.TransformServiceRegistry",
                "org.alfresco.transform.client.registry.TransformServiceRegistry",
                "org.alfresco.transform.registry.TransformServiceRegistry"))
        {
            try
            {
                registryInterfaceClass = Class.forName(className);
                registryBaseClass = Class.forName(className + "Impl");
            }
            catch (final ClassNotFoundException ignore)
            {
                // ignored
            }
        }

        Method isEnabled = null;
        Method isSupported = null;
        if (registryBaseClass != null)
        {
            try
            {
                isEnabled = registryBaseClass.getDeclaredMethod("isEnabled");
            }
            catch (final NoSuchMethodException ignore)
            {
                // ignored - not supported in ACS 6.1.2
            }
        }
        if (registryInterfaceClass != null)
        {
            try
            {
                isSupported = registryInterfaceClass.getDeclaredMethod("isSupported", String.class, long.class, String.class, Map.class,
                        String.class);
            }
            catch (final NoSuchMethodException ignore)
            {
                // ignored - should never be
            }
        }
        IS_ENABLED_METHOD = isEnabled;
        IS_SUPPORTED_METHOD = isSupported;
    }

    private static final Predicate<Object> IS_ENABLED = o -> {
        if (IS_ENABLED_METHOD == null)
        {
            return true; // assume true
        }
        try
        {
            return Boolean.TRUE.equals(IS_ENABLED_METHOD.invoke(o));
        }
        catch (InvocationTargetException | IllegalAccessException e)
        {
            return false;
        }
    };

    /**
     * Checks whether the necessary APIs are available to invoke this utility. Note that the caller is still required to handle any
     * {@link NoClassDefFoundError classloading errors} to be safe.
     *
     * @return {@code true} if the API is available, {@code false} otherwise
     */
    public static final boolean isAvailable()
    {
        boolean available = false;
        try
        {
            Class.forName("org.alfresco.repo.rendition2.RenditionService2");
            available = IS_SUPPORTED_METHOD != null;
        }
        catch (final Exception ignore)
        {
            // ignored
        }

        return available;
    }

    /**
     * Streams a rendition of a node if available.
     *
     * @param applicationContext
     *     the application context from which to resolve service instances
     * @param nodeRef
     *     the reference to the node for which to lookup and stream a thumbnail
     * @param effectivePropertyQName
     *     the qualified name of the content property for which the thumbnail should be streamed
     * @param renditionName
     *     the name of the rendition to stream
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
     * @return {@code true} if the rendition was streamed, {@code false} otherwise
     */
    public static final boolean streamRenditionIfAvailable(final ApplicationContext applicationContext, final NodeRef nodeRef,
            final QName effectivePropertyQName, final String renditionName, final ContentStreamer delegate, final WebScriptRequest req,
            final WebScriptResponse res, final boolean attach, final String attachFileName, final Map<String, Object> model)
            throws IOException
    {
        if (ContentModel.PROP_CONTENT.equals(effectivePropertyQName))
        {
            final RenditionService2 renditionService = applicationContext.getBean("RenditionService2", RenditionService2.class);
            final ChildAssociationRef renditionByName = renditionService.getRenditionByName(nodeRef, renditionName);
            if (renditionByName != null)
            {
                final NodeRef renditionRef = renditionByName.getChildRef();
                delegate.streamContent(req, res, renditionRef, ContentModel.PROP_CONTENT, attach, attachFileName, model);
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a specific rendition of a node if possible to be created.
     *
     * @param applicationContext
     *     the application context from which to resolve service instances
     * @param nodeRef
     *     the reference to the node for which to check the thumbnail potential
     * @param effectivePropertyQName
     *     the qualified name of the content property for which the thumbnail potential should be checked
     * @param renditionName
     *     the name of the rendition to check
     *
     * @return {@code true} if the thumbnail is possible, {@code false} otherwise
     */
    public static boolean isRenditionPossible(final ApplicationContext applicationContext, final NodeRef nodeRef,
            final QName effectivePropertyQName, final String renditionName)
    {
        boolean possible = false;
        if (ContentModel.PROP_CONTENT.equals(effectivePropertyQName))
        {
            final RenditionService2 renditionService = applicationContext.getBean("RenditionService2", RenditionService2.class);
            final RenditionDefinition2 renditionDefinition = renditionService.getRenditionDefinitionRegistry2()
                    .getRenditionDefinition(renditionName);
            if (renditionDefinition != null)
            {
                final ContentService contentService = applicationContext.getBean("ContentSerivce", ContentService.class);
                final ContentReader reader = contentService.getReader(nodeRef, effectivePropertyQName);
                if (reader != null && reader.exists())
                {
                    final Object remoteRegistry = applicationContext.getBean("remoteTransformServiceRegistry");
                    final Object localRegistry = applicationContext.getBean("localTransformServiceRegistry");
                    if (IS_ENABLED.test(remoteRegistry))
                    {
                        possible = isSupported(remoteRegistry, reader.getMimetype(), reader.getSize(),
                                renditionDefinition.getTargetMimetype(), renditionDefinition.getTransformOptions(),
                                renditionDefinition.getRenditionName());
                    }

                    if (IS_ENABLED.test(localRegistry) && !possible)
                    {
                        possible = isSupported(localRegistry, reader.getMimetype(), reader.getSize(),
                                renditionDefinition.getTargetMimetype(), renditionDefinition.getTransformOptions(),
                                renditionDefinition.getRenditionName());
                    }

                }
            }
        }

        return possible;
    }

    private static boolean isSupported(final Object registry, final String sourceMimetype, final long sourceSize,
            final String targetMimetype, final Map<String, String> transformOptions, final String renditionName)
    {
        try
        {
            return Boolean.TRUE.equals(
                    IS_SUPPORTED_METHOD.invoke(registry, sourceMimetype, sourceSize, targetMimetype, transformOptions, renditionName));
        }
        catch (InvocationTargetException | IllegalAccessException e)
        {
            return false;
        }
    }
}
