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

import java.lang.reflect.Method;

import org.alfresco.repo.content.transform.ContentTransformer;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.springframework.context.ApplicationContext;

/**
 * @author Axel Faust
 */
public class ContentServiceTransformerCheck
{

    /**
     * Checks whether the necessary APIs are available to invoke this check utility. Note that the caller is still required to handle any
     * {@link NoClassDefFoundError classloading errors} to be safe.
     *
     * @return {@code true} if the API is available, {@code false} otherwise
     */
    public static boolean isAvailable()
    {
        boolean available = false;
        try
        {
            final Class<?> contentServiceCls = Class.forName("org.alfresco.service.cmr.repository.ContentService");
            for (final Method m : contentServiceCls.getDeclaredMethods())
            {
                if ("getTransformer".equals(m.getName()))
                {
                    available = true;
                    break;
                }
            }
        }
        catch (final Exception ignore)
        {
            // ignored
        }

        return available;
    }

    /**
     * Checks whether a transformer exists for a particular transformation.
     *
     * @param applicationContext
     *     the application context from which to resolve service instances
     * @param contentUrl
     *     the source content URL
     * @param sourceMimetype
     *     the source content mimetype
     * @param size
     *     the source content size
     * @param targetMimetype
     *     the target mimetype
     * @param transformationOptions
     *     the transformation options
     * @return {@code true} if a transformer is available
     */
    public static boolean hasTransformer(final ApplicationContext applicationContext, final String contentUrl, final String sourceMimetype,
            final long size, final String targetMimetype, final TransformationOptions transformationOptions)
    {
        final ContentService contentService = applicationContext.getBean("ContentService", ContentService.class);
        final ContentTransformer transformer = contentService.getTransformer(contentUrl, sourceMimetype, size, targetMimetype,
                transformationOptions);
        return transformer != null;
    }
}
