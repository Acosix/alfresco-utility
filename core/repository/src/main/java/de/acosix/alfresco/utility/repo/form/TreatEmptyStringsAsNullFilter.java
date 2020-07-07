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
package de.acosix.alfresco.utility.repo.form;

import static org.alfresco.repo.forms.processor.node.FormFieldConstants.PROP_DATA_PREFIX;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.repo.forms.Form;
import org.alfresco.repo.forms.FormData;
import org.alfresco.repo.forms.FormData.FieldData;
import org.alfresco.repo.forms.processor.AbstractFilter;
import org.alfresco.repo.forms.processor.node.FormFieldConstants;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.PropertyCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * This form filter ensures that all property fields which the UI attempts to persist as empty strings are properly treated as non-existent
 * values instead. Empty strings are never valid values, but Alfresco by default will store them (much like null-values, which also are not
 * valid values, but in this case preferable to empty string values).
 *
 * @author Axel Faust
 */
public class TreatEmptyStringsAsNullFilter extends AbstractFilter<Object, NodeRef> implements InitializingBean
{

    private static final Logger LOGGER = LoggerFactory.getLogger(TreatEmptyStringsAsNullFilter.class);

    private static final Pattern PROPERTY_NAME_PATTERN = Pattern.compile(PROP_DATA_PREFIX + "([a-zA-Z0-9-]+)_(.*)");

    protected NamespaceService namespaceService;

    protected List<String> namespaceUris;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "namespaceService", this.namespaceService);
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
        // NO-OP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforePersist(final Object item, final FormData data)
    {
        data.getFieldNames().stream().filter(fn -> fn.startsWith(FormFieldConstants.PROP_DATA_PREFIX)).filter(fn -> {
            final Matcher m = PROPERTY_NAME_PATTERN
                    .matcher(fn.replaceAll(FormFieldConstants.DOT_CHARACTER_REPLACEMENT, FormFieldConstants.DOT_CHARACTER));

            boolean handleField = false;
            if (m.matches())
            {
                final String qNamePrefix = m.group(1);
                final String localName = m.group(2);
                final QName fullQName = QName.createQName(qNamePrefix, localName, this.namespaceService);
                handleField = this.namespaceUris == null || this.namespaceUris.contains(fullQName.getNamespaceURI());
            }

            return handleField;
        }).forEach(fn -> {
            final FieldData fieldData = data.getFieldData(fn);
            final Object value = fieldData.getValue();

            if (value instanceof String && ((String) value).trim().isEmpty())
            {
                LOGGER.debug("Clearing empty string value in field {}", fn);
                data.addFieldData(fn, null, true);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPersist(final Object item, final FormData data, final NodeRef persistedObject)
    {
        // NO-OP
    }

}
