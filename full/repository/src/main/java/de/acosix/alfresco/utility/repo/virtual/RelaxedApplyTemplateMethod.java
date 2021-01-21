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
package de.acosix.alfresco.utility.repo.virtual;

import org.alfresco.repo.virtual.ActualEnvironment;
import org.alfresco.repo.virtual.VirtualContext;
import org.alfresco.repo.virtual.ref.GetTemplatePathMethod;
import org.alfresco.repo.virtual.ref.ProtocolMethodException;
import org.alfresco.repo.virtual.ref.Reference;
import org.alfresco.repo.virtual.ref.Resource;
import org.alfresco.repo.virtual.ref.ResourceProcessingError;
import org.alfresco.repo.virtual.ref.VirtualProtocol;
import org.alfresco.repo.virtual.template.ApplyTemplateMethod;
import org.alfresco.repo.virtual.template.VirtualFolderDefinition;

/**
 * @author Axel Faust
 */
public class RelaxedApplyTemplateMethod extends ApplyTemplateMethod
{

    /**
     * Constructs a new instance of this class.
     *
     * @param environment
     *            the environment to use
     */
    public RelaxedApplyTemplateMethod(final ActualEnvironment environment)
    {
        super(environment);
    }

    /**
     *
     * {@inheritDoc}
     */
    // almost completely copied from base class except for use of custom template processor sub-class
    @Override
    public VirtualFolderDefinition execute(final VirtualProtocol virtualProtocol, final Reference reference, final VirtualContext context)
            throws ProtocolMethodException
    {
        final Resource resource = reference.getResource();

        try
        {
            VirtualFolderDefinition theStructure = resource.processWith(new RelaxedTemplateResourceProcessor(context));
            final String path = reference.execute(new GetTemplatePathMethod());

            if (!path.isEmpty())
            {
                final String[] pathElements = path.split(PATH_SEPARATOR);
                final int startIndex = path.startsWith(PATH_SEPARATOR) ? 1 : 0;
                for (int i = startIndex; i < pathElements.length; i++)
                {
                    theStructure = theStructure.findChildById(pathElements[i]);
                    if (theStructure == null)
                    {

                        throw new ProtocolMethodException("Invalid template path in " + reference.toString());

                    }
                }
            }

            return theStructure;
        }
        catch (final ResourceProcessingError e)
        {
            throw new ProtocolMethodException(e);
        }
    }
}
