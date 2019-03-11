/*
 * Copyright 2016 - 2019 Acosix GmbH
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
package de.acosix.alfresco.utility.repo.datatype;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.repository.datatype.TypeConverter.Converter;
import org.alfresco.service.namespace.InvalidQNameException;
import org.alfresco.service.namespace.NamespaceException;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.PropertyCheck;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.extensions.surf.util.I18NUtil;

/**
 * This class serves as an initialiser for improved {@link Converter} implementations for various conversion directions.
 *
 * @author Axel Faust
 */
public class ImprovedTypeConverterInitialiser implements InitializingBean
{

    protected NamespaceService namespaceService;

    protected boolean stringToNodeRefEnabled;

    protected boolean stringToQNameEnabled;

    protected boolean stringToLocaleEnabled;

    // this could easily have been made static, but having an instance-local property makes it easier to reset + test
    private final Map<String, Locale> localeConversionCache = new HashMap<>();

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        if (this.stringToNodeRefEnabled)
        {
            DefaultTypeConverter.INSTANCE.addConverter(String.class, NodeRef.class, this::convertStringToNodeRef);
        }

        if (this.stringToQNameEnabled)
        {
            PropertyCheck.mandatory(this, "namesapceService", this.namespaceService);
            DefaultTypeConverter.INSTANCE.addConverter(String.class, QName.class, this::convertStringToQName);
        }

        if (this.stringToLocaleEnabled)
        {
            DefaultTypeConverter.INSTANCE.addConverter(String.class, Locale.class, this::convertStringToLocale);
        }
    }

    /**
     * @param namespaceService
     *            the namespaceService to set
     */
    public void setNamespaceService(final NamespaceService namespaceService)
    {
        this.namespaceService = namespaceService;
    }

    /**
     * @param stringToNodeRefEnabled
     *            the stringToNodeRefEnabled to set
     */
    public void setStringToNodeRefEnabled(final boolean stringToNodeRefEnabled)
    {
        this.stringToNodeRefEnabled = stringToNodeRefEnabled;
    }

    /**
     * @param stringToQNameEnabled
     *            the stringToQNameEnabled to set
     */
    public void setStringToQNameEnabled(final boolean stringToQNameEnabled)
    {
        this.stringToQNameEnabled = stringToQNameEnabled;
    }

    /**
     * @param stringToLocaleEnabled
     *            the stringToLocaleEnabled to set
     */
    public void setStringToLocaleEnabled(final boolean stringToLocaleEnabled)
    {
        this.stringToLocaleEnabled = stringToLocaleEnabled;
    }

    protected NodeRef convertStringToNodeRef(final String source)
    {
        // default converter does not check for empty string and fails with illegal argument
        final String trimmedSource = source != null ? source.trim() : null;
        final NodeRef value = trimmedSource == null || trimmedSource.isEmpty() ? null : new NodeRef(trimmedSource);
        return value;
    }

    protected QName convertStringToQName(final String source)
    {
        // default converter does not check for empty string and reports invalid QName
        final String trimmedSource = source != null ? source.trim() : null;
        final QName value = trimmedSource == null || trimmedSource.isEmpty() ? null
                : QName.resolveToQName(this.namespaceService, trimmedSource);
        if (trimmedSource != null && !trimmedSource.isEmpty() && value == null)
        {
            // can typically only occur if prefix cannot be resolved to namespace
            if (trimmedSource.indexOf(QName.NAMESPACE_PREFIX) != -1)
            {
                final String prefix = trimmedSource.substring(0, trimmedSource.indexOf(QName.NAMESPACE_PREFIX));
                throw new NamespaceException("Namespace prefix " + prefix + " is not mapped to a namespace URI");
            }
            throw new InvalidQNameException("String could not be converted to QName for unknown reason");
        }
        return value;
    }

    protected Locale convertStringToLocale(final String str)
    {
        // default converter (via I18nUtil) does not cache, always constructing new objects
        final Locale locale = this.localeConversionCache.computeIfAbsent(str, localeStr -> {
            final Locale parsed = I18NUtil.parseLocale(localeStr);
            return parsed;
        });
        return locale;
    }
}
