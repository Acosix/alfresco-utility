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
package de.acosix.alfresco.utility.repo.form;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoField.YEAR;
import static org.alfresco.repo.forms.processor.node.FormFieldConstants.PROP_DATA_PREFIX;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.repo.forms.FieldDefinition;
import org.alfresco.repo.forms.Form;
import org.alfresco.repo.forms.FormData;
import org.alfresco.repo.forms.FormData.FieldData;
import org.alfresco.repo.forms.PropertyFieldDefinition;
import org.alfresco.repo.forms.processor.AbstractFilter;
import org.alfresco.repo.forms.processor.node.FormFieldConstants;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ISO8601DateFormat;
import org.alfresco.util.PropertyCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * This form filter ensures that all {@code d:date} property fields are properly fixated on {@code 00:00} in a specific timezone upon
 * submission from any client, regardless of the time component provided. It also ensures that the stored values of {@code d:date} values
 * are provided as date-only strings to clients when requesting a structured form definition with associated values, so as to force the
 * client to treat the fixed date as if in its local time zone. In essence, this filter is meant to remove any ambiguity with regards to
 * timezone handling of {@code d:date} values between the server and the client.
 *
 * @author Axel Faust
 */
public class FixateDateOnlyTimezoneFilter extends AbstractFilter<Object, Object> implements InitializingBean
{

    private static final ZoneId UTC = ZoneId.of("UTC");

    private static final Logger LOGGER = LoggerFactory.getLogger(FixateDateOnlyTimezoneFilter.class);

    private static final Pattern PROPERTY_NAME_PATTERN = Pattern.compile(PROP_DATA_PREFIX + "([a-zA-Z0-9-]+)_(.*)");

    // custom variant to the regular DateTimeFormatter.ISO_OFFSET_DATE_TIME constant because clients (*cough* Share *cough*) may not be able
    // to deal with variable nanosecond fraction length
    private static final DateTimeFormatter ISO_OFFSET_DATE_TIME;
    static
    {
        ISO_OFFSET_DATE_TIME = new DateTimeFormatterBuilder().parseCaseInsensitive().appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
                .appendLiteral('-').appendValue(MONTH_OF_YEAR, 2).appendLiteral('-').appendValue(DAY_OF_MONTH, 2).appendLiteral('T')
                .appendValue(HOUR_OF_DAY, 2).appendLiteral(':').appendValue(MINUTE_OF_HOUR, 2).optionalStart().appendLiteral(':')
                .appendValue(SECOND_OF_MINUTE, 2).optionalStart().appendFraction(NANO_OF_SECOND, 3, 3, true).appendOffsetId()
                .toFormatter(Locale.getDefault(Locale.Category.FORMAT));
    }

    protected NamespaceService namespaceService;

    protected DictionaryService dictionaryService;

    protected String fixedDateTimezone;

    protected String fixedDateDisplayTimezone;

    protected ZoneId timezoneId;

    protected ZoneId displayTimezoneId;

    protected List<String> namespaceUris;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "namespaceService", this.namespaceService);
        PropertyCheck.mandatory(this, "dictionaryService", this.dictionaryService);
        PropertyCheck.mandatory(this, "fixedDateTimezone", this.fixedDateTimezone);
        PropertyCheck.mandatory(this, "fixedDateDisplayTimezone", this.fixedDateDisplayTimezone);

        this.timezoneId = ZoneId.of(this.fixedDateTimezone);
        this.displayTimezoneId = ZoneId.of(this.fixedDateDisplayTimezone);
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
     * @param dictionaryService
     *            the dictionaryService to set
     */
    public void setDictionaryService(final DictionaryService dictionaryService)
    {
        this.dictionaryService = dictionaryService;
    }

    /**
     * @param fixedDateTimezone
     *            the fixedDateTimezone to set
     */
    public void setFixedDateTimezone(final String fixedDateTimezone)
    {
        this.fixedDateTimezone = fixedDateTimezone;
    }

    /**
     * @param fixedDateDisplayTimezone
     *            the fixedDateDisplayTimezone to set
     */
    public void setFixedDateDisplayTimezone(final String fixedDateDisplayTimezone)
    {
        this.fixedDateDisplayTimezone = fixedDateDisplayTimezone;
    }

    /**
     * @param namespaceUris
     *            the namespaceUris to set
     */
    public void setNamespaceUris(final List<String> namespaceUris)
    {
        this.namespaceUris = namespaceUris;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeGenerate(final Object item, final List<String> fields, final List<String> forcedFields, final Form form,
            final Map<String, Object> context)
    {
        // NO-OP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterGenerate(final Object item, final List<String> fields, final List<String> forcedFields, final Form form,
            final Map<String, Object> context)
    {
        final List<FieldDefinition> fieldDefinitions = form.getFieldDefinitions();
        if (fieldDefinitions != null && form.getFormData() != null)
        {
            fieldDefinitions.stream().filter(PropertyFieldDefinition.class::isInstance).map(PropertyFieldDefinition.class::cast)
                    .filter(d -> this.namespaceUris == null
                            || this.namespaceUris.contains(QName.resolveToQName(this.namespaceService, d.getName()).getNamespaceURI()))
                    .filter(d -> DataTypeDefinition.DATE
                            .equals(QName.createQName(NamespaceService.DICTIONARY_MODEL_1_0_URI, d.getDataType())))
                    .forEach(fieldDef -> {
                        final FieldData fieldData = form.getFormData().getFieldData(fieldDef.getDataKeyName());
                        if (fieldData != null)
                        {
                            final Object value = fieldData.getValue();

                            Date dValue = null;
                            if (value instanceof Date)
                            {
                                dValue = (Date) value;
                            }
                            else if (value instanceof String)
                            {
                                dValue = ISO8601DateFormat.parse((String) value);
                            }
                            else
                            {
                                LOGGER.debug("Unable to support value {} of type {} in field {}", value, value.getClass(),
                                        fieldDef.getName());
                            }

                            if (dValue != null)
                            {
                                // we need to use a full ISO8601 date for rendering in Share date controls, which use Spring Surf
                                // ISO8601DateFormatMethod that cannot deal with date-only values
                                // we use a separately configured display timezone (typically Share server timezone) to ensure we don't have
                                // issues with timezone shifts
                                final String timezoneShiftedValue = dValue.toInstant().atZone(this.timezoneId)
                                                .withZoneSameLocal(this.displayTimezoneId).format(ISO_OFFSET_DATE_TIME);
                                LOGGER.debug("Fixed date {} of field {} to timezone {} local value shifted to {} as {}", dValue,
                                        fieldDef.getName(), this.timezoneId, this.displayTimezoneId, timezoneShiftedValue);
                                form.getFormData().addFieldData(fieldDef.getDataKeyName(), timezoneShiftedValue, true);
                            }
                        }
                    });
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforePersist(final Object item, final FormData data)
    {
        data.getFieldNames().stream().filter(fn -> fn.startsWith(FormFieldConstants.PROP_DATA_PREFIX)).filter(fn -> {
            boolean dateOnlyField = false;
            final Matcher m = PROPERTY_NAME_PATTERN
                    .matcher(fn.replaceAll(FormFieldConstants.DOT_CHARACTER_REPLACEMENT, FormFieldConstants.DOT_CHARACTER));
            if (m.matches())
            {
                final String qNamePrefix = m.group(1);
                final String localName = m.group(2);
                final QName fullQName = QName.createQName(qNamePrefix, localName, this.namespaceService);
                final PropertyDefinition property = this.dictionaryService.getProperty(fullQName);
                dateOnlyField = (this.namespaceUris == null || this.namespaceUris.contains(fullQName.getNamespaceURI())) && property != null
                        && DataTypeDefinition.DATE.equals(property.getDataType().getName());
            }

            return dateOnlyField;
        }).forEach(fn -> {
            final FieldData fieldData = data.getFieldData(fn);
            final Object value = fieldData.getValue();

            Date dValue = null;
            if (value instanceof Date)
            {
                // unlikely but maybe there was some pre-filtering going on
                dValue = (Date) value;
            }
            else if (value instanceof String)
            {
                dValue = ISO8601DateFormat.parse((String) value);
            }
            else
            {
                LOGGER.debug("Unable to support value {} of type {} in field {}", value, value != null ? value.getClass() : null, fn);
            }

            if (dValue != null)
            {
                final String timezoneFixedDateOnlyValue = dValue.toInstant().atZone(this.timezoneId).withHour(0).withMinute(0).withSecond(0)
                        .withNano(0).withZoneSameInstant(UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                LOGGER.debug("Fixed date {} of field {} to timezone {} local midnight time value of {}", dValue, fn, this.timezoneId,
                        timezoneFixedDateOnlyValue);
                data.addFieldData(fn, timezoneFixedDateOnlyValue, true);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPersist(final Object item, final FormData data, final Object persistedObject)
    {
        // NO-OP
    }

}
