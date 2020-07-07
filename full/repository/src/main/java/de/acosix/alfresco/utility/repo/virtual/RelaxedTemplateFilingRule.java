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
package de.acosix.alfresco.utility.repo.virtual;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alfresco.repo.virtual.ActualEnvironment;
import org.alfresco.repo.virtual.VirtualizationException;
import org.alfresco.repo.virtual.config.NodeRefPathExpression;
import org.alfresco.repo.virtual.ref.Reference;
import org.alfresco.repo.virtual.template.FilingData;
import org.alfresco.repo.virtual.template.FilingParameters;
import org.alfresco.repo.virtual.template.TemplateFilingRule;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ISO9075;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Axel Faust
 */
public class RelaxedTemplateFilingRule extends TemplateFilingRule
{

    private static Logger LOGGER = LoggerFactory.getLogger(TemplateFilingRule.class);

    // copied from base class due to visibility restrictions
    protected ActualEnvironment env;

    protected String path;

    protected String type;

    protected Set<String> aspects;

    protected Map<String, String> stringProperties;

    /**
     * Constructs a new instance of this class.
     *
     * @param environment
     *            the environment to use
     * @param path
     *            the path to use
     * @param type
     *            the type to use
     * @param aspects
     *            the aspects to use
     * @param properties
     *            the properties to use
     */
    public RelaxedTemplateFilingRule(final ActualEnvironment environment, final String path, final String type, final Set<String> aspects,
            final Map<String, String> properties)
    {
        super(environment, path, type, aspects, properties);
        this.env = environment;
        this.path = path;
        this.type = type;
        this.aspects = aspects;
        this.stringProperties = properties;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public FilingData createFilingData(final FilingParameters parameters) throws VirtualizationException
    {
        return this.createFilingData(parameters.getParentRef(), parameters.getAssocTypeQName(), parameters.getAssocQName(),
                parameters.getNodeTypeQName(), parameters.getProperties());
    }

    // almost completely copied from base class
    protected FilingData createFilingData(final Reference parentRef, final QName assocTypeQName, final QName assocQName,
            final QName nodeTypeQName, final Map<QName, Serializable> properties) throws VirtualizationException
    {
        NodeRef fParentRef = null;
        QName fType = null;
        Set<QName> fAspects = null;
        Map<QName, Serializable> fProperties = null;

        final NamespacePrefixResolver nsPrefixResolver = this.env.getNamespacePrefixResolver();

        if (this.type == null || this.type.length() == 0)
        {
            fType = nodeTypeQName;
        }
        else
        {
            fType = QName.resolveToQName(nsPrefixResolver, this.type);

            if (this.env.isSubClass(nodeTypeQName, fType))
            {
                fType = nodeTypeQName;
            }
            else if (!this.env.isSubClass(fType, nodeTypeQName))
            {
                throw new VirtualizationException("The filing rule for the virtual folder specifies an incompatible node type.");
            }
        }

        fParentRef = this.parentNodeRefFor(parentRef, false);

        fProperties = new HashMap<>(properties);

        final Set<Entry<String, String>> propertyEntries = this.stringProperties.entrySet();

        for (final Entry<String, String> propertyEntry : propertyEntries)
        {
            final String name = propertyEntry.getKey();
            final QName qName = QName.resolveToQName(nsPrefixResolver, name);
            if (!fProperties.containsKey(qName))
            {
                fProperties.put(qName, this.stringProperties.get(name));
            }
        }

        fAspects = new HashSet<>();

        for (final String aspect : this.aspects)
        {
            fAspects.add(QName.resolveToQName(nsPrefixResolver, aspect));
        }

        return new FilingData(fParentRef, assocTypeQName, assocQName, fType, fAspects, fProperties);

    }

    // copied from base class, adapted to remove path == null default handling
    protected NodeRef parentNodeRefFor(final Reference parentReference, final boolean failIfNotFound)
    {
        NodeRef fParentRef = null;
        if (this.path != null && !this.path.isEmpty())
        {
            final String[] pathElements = NodeRefPathExpression.splitAndNormalizePath(this.path);
            for (int i = 0; i < pathElements.length; i++)
            {
                pathElements[i] = ISO9075.decode(pathElements[i]);
            }
            fParentRef = this.env.findQNamePath(pathElements);
        }

        boolean noReadPermissions = false;
        if (fParentRef != null && !this.env.hasPermission(fParentRef, PermissionService.READ_PERMISSIONS))
        {
            fParentRef = null;
            noReadPermissions = true;
        }

        if (fParentRef == null)
        {
            if (noReadPermissions)
            {
                LOGGER.debug("Current user does not have READ_PERMISSIONS for filing path {}.", this.path);
            }
            else
            {
                LOGGER.debug("The filing path {} doesn't exist.", this.path);
            }
        }

        if (failIfNotFound && fParentRef == null)
        {
            throw new VirtualizationException("The filing path " + this.path + " could not be resolved.");
        }

        return fParentRef;
    }
}
