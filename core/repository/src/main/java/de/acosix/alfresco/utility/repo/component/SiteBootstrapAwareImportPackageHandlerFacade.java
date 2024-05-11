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
package de.acosix.alfresco.utility.repo.component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.service.cmr.view.ImportPackageHandler;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Axel Faust
 */
public class SiteBootstrapAwareImportPackageHandlerFacade implements ImportPackageHandler
{

    private final static Logger LOGGER = LoggerFactory.getLogger(SiteBootstrapAwareImportPackageHandlerFacade.class);

    private final static String SITE_NAME_PLACEHOLDER = "${siteName}";

    protected final ImportPackageHandler delegate;

    protected final String siteName;

    public SiteBootstrapAwareImportPackageHandlerFacade(final ImportPackageHandler delegate, final String siteName)
    {
        this.delegate = delegate;
        this.siteName = siteName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startImport()
    {
        this.delegate.startImport();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Reader getDataStream()
    {
        return this.delegate.getDataStream();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream importStream(final String content)
    {
        InputStream is;
        if (content.endsWith(".xml"))
        {
            try
            {
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                IOUtils.copy(this.delegate.importStream(content), bos);
                String xmlContent = new String(bos.toByteArray(), StandardCharsets.UTF_8);
                xmlContent = xmlContent.replace(SITE_NAME_PLACEHOLDER, this.siteName);
                is = new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8));
            }
            catch (final IOException ex)
            {
                LOGGER.error("Error handling import of site XML content", ex);
                throw new AlfrescoRuntimeException("Error handling site XML content", ex);
            }
        }
        else
        {
            is = this.delegate.importStream(content);
        }
        return is;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endImport()
    {
        this.delegate.endImport();
    }

}