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
package de.acosix.alfresco.utility.repo.datatype;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.repository.datatype.TypeConversionException;
import org.alfresco.service.cmr.repository.datatype.TypeConverter.Converter;
import org.alfresco.service.namespace.InvalidQNameException;
import org.alfresco.service.namespace.NamespaceException;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ISO8601DateFormat;
import org.alfresco.util.PropertyCheck;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.extensions.surf.exception.PlatformRuntimeException;
import org.springframework.extensions.surf.util.I18NUtil;

/**
 * This class serves as an initialiser for improved {@link Converter} implementations for various conversion directions.
 *
 * @author Axel Faust
 */
public class ImprovedTypeConverterInitialiser implements InitializingBean
{

    // RFC 822 / RFC 1123
    private static final String RFC822_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss Z";

    protected NamespaceService namespaceService;

    protected boolean stringToNodeRefEnabled;

    protected boolean stringToQNameEnabled;

    protected boolean stringToLocaleEnabled;

    protected boolean stringToDateEnabled;

    // this could easily have been made static, but having an instance-local property makes it easier to reset + test
    private final Map<String, Locale> localeConversionCache = new HashMap<>();

    private final ReentrantReadWriteLock localeConversionCacheLock = new ReentrantReadWriteLock(true);

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

        if (this.stringToDateEnabled)
        {
            DefaultTypeConverter.INSTANCE.addConverter(String.class, Date.class, this::convertStringToDate);
        }
    }

    /**
     * @param namespaceService
     *     the namespaceService to set
     */
    public void setNamespaceService(final NamespaceService namespaceService)
    {
        this.namespaceService = namespaceService;
    }

    /**
     * @param stringToNodeRefEnabled
     *     the stringToNodeRefEnabled to set
     */
    public void setStringToNodeRefEnabled(final boolean stringToNodeRefEnabled)
    {
        this.stringToNodeRefEnabled = stringToNodeRefEnabled;
    }

    /**
     * @param stringToQNameEnabled
     *     the stringToQNameEnabled to set
     */
    public void setStringToQNameEnabled(final boolean stringToQNameEnabled)
    {
        this.stringToQNameEnabled = stringToQNameEnabled;
    }

    /**
     * @param stringToLocaleEnabled
     *     the stringToLocaleEnabled to set
     */
    public void setStringToLocaleEnabled(final boolean stringToLocaleEnabled)
    {
        this.stringToLocaleEnabled = stringToLocaleEnabled;
    }

    /**
     * @param stringToDateEnabled
     *     the stringToDateEnabled to set
     */
    public void setStringToDateEnabled(final boolean stringToDateEnabled)
    {
        this.stringToDateEnabled = stringToDateEnabled;
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
        Locale locale;

        this.localeConversionCacheLock.readLock().lock();
        try
        {
            locale = this.localeConversionCache.get(str);
        }
        finally
        {
            this.localeConversionCacheLock.readLock().unlock();
        }

        if (locale == null)
        {
            this.localeConversionCacheLock.writeLock().lock();
            try
            {
                locale = I18NUtil.parseLocale(str);
                this.localeConversionCache.put(str, locale);
            }
            finally
            {
                this.localeConversionCacheLock.writeLock().unlock();
            }
        }
        return locale;
    }

    protected Date convertStringToDate(final String source)
    {
        Date date = null;
        try
        {
            try
            {
                date = ISO8601DateFormat.parse(source);
            }
            catch (final AlfrescoRuntimeException e)
            {
                final SimpleDateFormat df = new SimpleDateFormat(RFC822_DATE_FORMAT, Locale.ENGLISH);
                df.setTimeZone(TimeZone.getTimeZone("UTC"));
                try
                {
                    date = df.parse(source);
                }
                catch (final ParseException pe)
                {
                    throw new TypeConversionException("Failed to convert date " + source + " to string", pe);
                }
            }
        }
        catch (final PlatformRuntimeException | AlfrescoRuntimeException e)
        {
            throw new TypeConversionException("Failed to convert date " + source + " to string", e);
        }

        return date;
    }
}
