/*
 * Copyright 2017 Acosix GmbH
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
package de.acosix.alfresco.utility.repo.content.transformer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.filestore.FileContentWriter;
import org.alfresco.repo.content.transform.AbstractContentTransformer2;
import org.alfresco.repo.content.transform.ContentTransformer;
import org.alfresco.repo.content.transform.ContentTransformerRegistry;
import org.alfresco.repo.content.transform.TransformerConfig;
import org.alfresco.repo.content.transform.UnsupportedTransformationException;
import org.alfresco.service.cmr.module.ModuleService;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.alfresco.service.descriptor.DescriptorService;
import org.alfresco.util.ParameterCheck;
import org.alfresco.util.PropertyCheck;
import org.alfresco.util.TempFileProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.extensions.surf.util.I18NUtil;

/**
 * This is a slightly improved variant of the Alfresco default {@link org.alfresco.repo.content.transform.FailoverContentTransformer} with
 * support for dynamic lookup of potential transformer delegates and fail-over on empty content results (already a {@code TODO} in the
 * original Alfresco source)
 *
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class FailoverContentTransformer extends AbstractContentTransformer2 implements InitializingBean
{

    // need to avoid re-use of the default TransformerConfig.FAILOVER suffix since TransformerConfigDynamicTransformers handles these
    private static final String SUFFIX_ALT_FAILOVER = ".altFailover";

    protected static final String CONTROL_CHARACTER_REGEX;
    static
    {
        final StringBuilder regexBuilder = new StringBuilder();
        for (char c = 'A'; c <= 'Z'; c++)
        {
            if (regexBuilder.length() != 0)
            {
                regexBuilder.append("|");
            }
            regexBuilder.append("\\c").append(c);
        }
        CONTROL_CHARACTER_REGEX = regexBuilder.toString();
    }

    private final static Logger LOGGER = LoggerFactory.getLogger(FailoverContentTransformer.class);

    protected DescriptorService descriptorService;

    protected ModuleService moduleService;

    protected ContentTransformerRegistry transformerRegistry;

    protected final List<ContentTransformer> transformers = new ArrayList<>();

    protected boolean failoverOnEmptyContent = true;

    protected long textFailoverCheckThreshold = 1024l;

    protected boolean supportedForEditionAndModule = true;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj)
    {
        return super.equals(obj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "descriptorService", this.descriptorService);
        PropertyCheck.mandatory(this, "moduleService", this.moduleService);
        PropertyCheck.mandatory(this, "transformerRegistry", this.transformerRegistry);

        final String edition = this.getPropertyValue(TransformerConfig.EDITION);
        final String moduleId = this.getPropertyValue(TransformerConfig.AMP);

        if (this.supportedForEditionAndModule && edition != null)
        {
            this.supportedForEditionAndModule = this.descriptorService.getServerDescriptor().getEdition().equals(edition);
        }

        if (this.supportedForEditionAndModule && moduleId != null)
        {
            this.supportedForEditionAndModule = this.moduleService.getModule(moduleId) != null;
        }

        if (this.transformers.isEmpty())
        {
            final String failover = this.getPropertyValue(SUFFIX_ALT_FAILOVER);
            if (failover != null)
            {
                final String[] subTransformersAndMimetypes = failover.split("\\|");
                if (subTransformersAndMimetypes.length >= 2)
                {
                    for (final String name : subTransformersAndMimetypes)
                    {
                        try
                        {
                            final ContentTransformer subTransformer = TransformerConfig.ANY.equals(name) ? null
                                    : this.transformerRegistry.getTransformer(TransformerConfig.TRANSFORMER + name);
                            this.transformers.add(subTransformer);
                        }
                        catch (final IllegalArgumentException e)
                        {
                            LOGGER.trace("{} did not find {}{}", this.getBeanName(), TransformerConfig.TRANSFORMER, name);
                            throw e;
                        }
                    }
                }
            }

            if (this.transformers.isEmpty())
            {
                throw new AlfrescoRuntimeException("At least one inner transformer must be supplied: " + this);
            }
        }

        if (this.getMimetypeService() == null)
        {
            throw new AlfrescoRuntimeException("'mimetypeService' is a required property");
        }

        this.register();
    }

    /**
     * @param descriptorService
     *            the descriptorService to set
     */
    public void setDescriptorService(final DescriptorService descriptorService)
    {
        this.descriptorService = descriptorService;
    }

    /**
     * @param moduleService
     *            the moduleService to set
     */
    public void setModuleService(final ModuleService moduleService)
    {
        this.moduleService = moduleService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRegistry(final ContentTransformerRegistry registry)
    {
        super.setRegistry(registry);
        this.transformerRegistry = registry;
    }

    /**
     * The list of transformers to use. There must be at least one, but for failover behaviour to work
     * there should be at least two.
     *
     * @param transformers
     *            list of transformers.
     */
    public void setTransformers(final List<ContentTransformer> transformers)
    {
        ParameterCheck.mandatory("transformers", transformers);
        this.transformers.clear();
        this.transformers.addAll(transformers);
    }

    /**
     * @param failoverOnEmptyContent
     *            the failoverOnEmptyContent to set
     */
    public void setFailoverOnEmptyContent(final boolean failoverOnEmptyContent)
    {
        this.failoverOnEmptyContent = failoverOnEmptyContent;
    }

    /**
     * @param textFailoverCheckThreshold
     *            the textFailoverCheckThreshold to set
     */
    public void setTextFailoverCheckThreshold(final long textFailoverCheckThreshold)
    {
        this.textFailoverCheckThreshold = textFailoverCheckThreshold;
    }

    /**
     * Overrides super class method to avoid calling
     * {@link #isTransformableMimetype(String, String, TransformationOptions)}
     * twice on each transformer in the list, as
     * {@link #isTransformableSize(String, long, String, TransformationOptions)}
     * in this class must check the mimetype too.
     */
    @Override
    public boolean isTransformable(final String sourceMimetype, final long sourceSize, final String targetMimetype,
            final TransformationOptions options)
    {
        return this.isSupportedTransformation(sourceMimetype, targetMimetype, options) &&
        // isTransformableSize must check the mimetype anyway
                (sourceSize >= 0 && this.isTransformableSize(sourceMimetype, sourceSize, targetMimetype, options))
                || (sourceSize < 0 && this.isTransformableMimetype(sourceMimetype, targetMimetype, options));
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isTransformableMimetype(final String sourceMimetype, final String targetMimetype, final TransformationOptions options)
    {
        return this.isTransformableMimetypeAndSize(sourceMimetype, -1, targetMimetype, options);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isTransformableSize(final String sourceMimetype, final long sourceSize, final String targetMimetype,
            final TransformationOptions options)
    {
        // first check our configuration, then those of the constituent transformers
        return (sourceSize < 0 || super.isTransformableSize(sourceMimetype, sourceSize, targetMimetype, options))
                && this.isTransformableMimetypeAndSize(sourceMimetype, sourceSize, targetMimetype, options);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("deprecation")
    public boolean isExplicitTransformation(final String sourceMimetype, final String targetMimetype, final TransformationOptions options)
    {
        boolean result = true;
        for (final ContentTransformer ct : this.transformers)
        {
            if (ct.isExplicitTransformation(sourceMimetype, targetMimetype, options) == false)
            {
                result = false;
            }
        }
        return result;
    }

    /**
     * Returns the transformer properties predefined (hard coded or implied) by this transformer.
     */
    @Override
    public String getComments(final boolean available)
    {
        final StringBuilder sb = new StringBuilder();
        sb.append(super.getComments(available));
        sb.append("# ");
        sb.append(TransformerConfig.CONTENT);
        sb.append(this.getName());
        sb.append(TransformerConfig.FAILOVER);
        sb.append('=');
        boolean first = true;
        for (final ContentTransformer transformer : this.transformers)
        {
            if (!first)
            {
                sb.append(TransformerConfig.PIPE);
            }
            first = false;
            sb.append(transformer != null ? getSimpleName(transformer) : TransformerConfig.ANY);
        }
        sb.append('\n');
        return sb.toString();
    }

    protected String getPropertyValue(final String propertySuffix)
    {
        final String property = this.transformerConfig.getProperty(TransformerConfig.CONTENT + this.getBeanName() + propertySuffix);
        return property;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void transformInternal(final ContentReader sourceReader, final ContentWriter targetWriter,
            final TransformationOptions options) throws Exception
    {
        final String outputMimetype = targetWriter.getMimetype();
        final String outputFileExt = this.getMimetypeService().getExtension(outputMimetype);

        // most complex transformers don't pass locale along - so we at least set the content locale in the thread scope
        final Locale originalContentLocale = I18NUtil.getContentLocaleOrNull();
        final Locale sourceLocale = sourceReader.getLocale();
        if (sourceLocale != null)
        {
            I18NUtil.setContentLocale(sourceLocale);
        }

        try
        {
            Exception transformationException = null;

            for (int i = 0; i < this.transformers.size(); i++)
            {
                final int oneBasedCount = i + 1;
                final ContentTransformer transf = this.transformers.get(i);

                // get a fresh reader for every attempt
                final ContentReader currentReader = sourceReader.getReader();
                ContentWriter currentWriter = null;

                File tempFile = null;
                try
                {
                    LOGGER.debug("Transformation attempt {} of {}: {}", oneBasedCount, this.transformers.size(), transf);

                    if (!transf.isTransformable(currentReader.getMimetype(), currentReader.getSize(), outputMimetype, options))
                    {
                        throw new UnsupportedTransformationException(
                                "Unsupported transformation: " + currentReader.getMimetype() + " to " + outputMimetype);
                    }

                    tempFile = TempFileProvider.createTempFile(
                            "FailoverTransformer_intermediate_" + transf.getClass().getSimpleName() + "_", "." + outputFileExt);
                    currentWriter = new FileContentWriter(tempFile);
                    currentWriter.setMimetype(outputMimetype);
                    currentWriter.setEncoding(targetWriter.getEncoding());
                    currentWriter.setLocale(currentReader.getLocale());

                    transf.transform(currentReader, currentWriter, options);

                    // zero-length output check missing in Alfresco FailoverContentTransformer
                    final long writtenSize = currentWriter.getSize();
                    if (this.failoverOnEmptyContent)
                    {
                        if (writtenSize == 0)
                        {
                            // consider failed
                            LOGGER.debug("Transformation {} was unsuccessful due to empty output.", oneBasedCount);
                            continue;
                        }
                        else if (outputMimetype.startsWith(MimetypeMap.PREFIX_TEXT) && writtenSize < this.textFailoverCheckThreshold)
                        {
                            final String content = currentWriter.getReader().getContentString();
                            final String contentWithoutControlChars = content.replaceAll(CONTROL_CHARACTER_REGEX, "");
                            if (contentWithoutControlChars.trim().isEmpty())
                            {
                                // consider failed
                                LOGGER.debug("Transformation {} was unsuccessful due to empty output.", oneBasedCount);
                                continue;
                            }
                        }
                    }

                    if (tempFile != null)
                    {
                        targetWriter.putContent(tempFile);
                    }
                }
                catch (final Exception are)
                {
                    if (transformationException == null)
                    {
                        transformationException = are;
                    }

                    LOGGER.debug("Transformation {} was unsuccessful.", oneBasedCount);
                    if (i != this.transformers.size() - 1)
                    {
                        // We don't log the last exception as we're going to throw it.
                        LOGGER.debug("The below exception is provided for information purposes only.", are);
                    }

                    continue;
                }
                finally
                {
                    // do eager cleanup of temporary files
                    if (tempFile != null && !tempFile.delete())
                    {
                        tempFile.deleteOnExit();
                    }
                }

                // At this point the current transformation was successful i.e. it did not throw an exception.
                LOGGER.info("Transformation was successful");
                return;
            }

            this.transformerDebug.debug("          No more transformations to failover to");
            if (transformationException != null)
            {
                LOGGER.debug("All transformations were unsuccessful. Throwing first exception.", transformationException);
                throw transformationException;
            }

            if (!this.failoverOnEmptyContent)
            {
                targetWriter.putContent(new ByteArrayInputStream(new byte[0]));
            }
            else
            {
                LOGGER.debug("All transformations were unsuccessful due to lack of output");
                throw new ContentIOException("Failed to transform content - all failover transformations yielded no output");
            }
        }
        finally
        {
            I18NUtil.setContentLocale(originalContentLocale);
        }
    }

    protected boolean isTransformableMimetypeAndSize(final String sourceMimetype, final long sourceSize, final String targetMimetype,
            final TransformationOptions options)
    {
        boolean result = false;
        for (final ContentTransformer ct : this.transformers)
        {
            if (ct.isTransformableMimetype(sourceMimetype, targetMimetype, options))
            {
                // for unspecified source size it has to suffice that we have a transformer able to handle the mimetype
                if (sourceSize < 0)
                {
                    result = true;
                    break;
                }

                try
                {
                    this.transformerDebug.pushIsTransformableSize(this);
                    if (ct.isTransformableSize(sourceMimetype, sourceSize, targetMimetype, options))
                    {
                        result = true;
                        break;
                    }
                }
                finally
                {
                    this.transformerDebug.popIsTransformableSize();
                }
            }
        }
        return result;
    }
}
