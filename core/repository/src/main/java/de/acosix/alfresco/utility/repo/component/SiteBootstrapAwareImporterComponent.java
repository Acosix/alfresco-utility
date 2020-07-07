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
package de.acosix.alfresco.utility.repo.component;

import java.io.Reader;

import org.alfresco.repo.importer.ImporterComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.view.ImportPackageHandler;
import org.alfresco.service.cmr.view.ImporterBinding;
import org.alfresco.service.cmr.view.ImporterProgress;
import org.alfresco.service.cmr.view.Location;

/**
 * @author Axel Faust
 */
public class SiteBootstrapAwareImporterComponent extends ImporterComponent
{

    protected String siteName;

    /**
     * Sets the value of siteName.
     *
     * @param siteName
     *            the siteName to set
     */
    public void setSiteName(final String siteName)
    {
        this.siteName = siteName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void parserImport(final NodeRef nodeRef, final Location location, final Reader viewReader,
            final ImportPackageHandler streamHandler, final ImporterBinding binding, final ImporterProgress progress)
    {
        ImportPackageHandler effectiveStreamHandler = streamHandler;
        if (this.siteName != null && !this.siteName.trim().isEmpty())
        {
            effectiveStreamHandler = new SiteBootstrapAwareImportPackageHandlerFacade(streamHandler, this.siteName);
        }
        super.parserImport(nodeRef, location, viewReader, effectiveStreamHandler, binding, progress);
    }
}
