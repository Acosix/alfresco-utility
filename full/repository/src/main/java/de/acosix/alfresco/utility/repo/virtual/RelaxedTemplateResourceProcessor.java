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
package de.acosix.alfresco.utility.repo.virtual;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.virtual.VirtualContext;
import org.alfresco.repo.virtual.ref.ResourceProcessingError;
import org.alfresco.repo.virtual.template.FilingRule;
import org.alfresco.repo.virtual.template.NullFilingRule;
import org.alfresco.repo.virtual.template.TemplateResourceProcessor;

/**
 * @author Axel Faust
 */
public class RelaxedTemplateResourceProcessor extends TemplateResourceProcessor
{

    // copied from base class due to visibility restrictions
    private static final String CLASSIFICATION_KEY = "classification";

    private static final String TYPE_KEY = "type";

    private static final String ASPECTS_KEY = "aspects";

    private static final String PROPERTIES_KEY = "properties";

    private static final String PATH_KEY = "path";

    /**
     * Constructs a new instance of this class.
     *
     * @param context
     *            the context to use
     */
    public RelaxedTemplateResourceProcessor(final VirtualContext context)
    {
        super(context);
    }

    /**
     *
     * {@inheritDoc}
     */
    // almost completely copied from base class except for use of custom template filing rule
    @Override
    @SuppressWarnings("unchecked")
    protected FilingRule asFilingRule(final VirtualContext context, final Object filing) throws ResourceProcessingError
    {
        if (filing == null)
        {
            return null;
        }

        final Map<String, Object> filingMap = (Map<String, Object>) filing;

        if (filingMap.isEmpty())
        {
            return null;
        }

        final String path = (String) filingMap.get(PATH_KEY);

        final Map<String, Object> classificationMap = (Map<String, Object>) filingMap.get(CLASSIFICATION_KEY);

        String type = null;

        List<String> aspects = null;

        if (classificationMap != null)
        {
            type = (String) classificationMap.get(TYPE_KEY);

            aspects = (List<String>) classificationMap.get(ASPECTS_KEY);
        }

        if (aspects == null)
        {
            aspects = Collections.emptyList();
        }

        Map<String, String> properties = (Map<String, String>) filingMap.get(PROPERTIES_KEY);
        if (properties == null)
        {
            properties = Collections.emptyMap();
        }

        if (path == null && type == null && aspects.isEmpty() && properties.isEmpty())
        {
            return new NullFilingRule(context.getActualEnviroment());
        }
        else
        {
            return new RelaxedTemplateFilingRule(context.getActualEnviroment(), path, type, new HashSet<>(aspects), properties);

        }

    }
}
