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
package de.acosix.alfresco.utility.repo.action;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.alfresco.repo.action.constraint.BaseParameterConstraint;
import org.alfresco.repo.dictionary.constraint.ListOfValuesConstraint;
import org.alfresco.repo.dictionary.constraint.RegisteredConstraint;
import org.alfresco.service.cmr.dictionary.ClassDefinition;
import org.alfresco.service.cmr.dictionary.ConstraintDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.PropertyCheck;
import org.springframework.beans.factory.InitializingBean;

/**
 * Instances of this class provide a parameter constraint re-using existing dictionary model property constraints.
 *
 * @author Axel Faust
 */
public class ReferencePropertyParameterConstraint extends BaseParameterConstraint implements InitializingBean
{

    protected NamespaceService namespaceService;

    protected DictionaryService dictionaryService;

    protected String className;

    protected String propertyName;

    protected QName classQName;

    protected QName propertyQName;

    protected ListOfValuesConstraint constraint;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "namespaceService", this.namespaceService);
        PropertyCheck.mandatory(this, "dictionaryService", this.dictionaryService);
        PropertyCheck.mandatory(this, "propertyName", this.propertyName);

        this.propertyQName = QName.resolveToQName(this.namespaceService, this.propertyName);
        if (this.propertyQName == null)
        {
            throw new IllegalStateException(this.propertyName + " cannot be resolved to a qualified name");
        }
        PropertyDefinition property = this.dictionaryService.getProperty(this.propertyQName);
        if (property == null)
        {
            throw new IllegalStateException(this.propertyName + " is not a property defined in the dictionary model");
        }

        if (this.className != null)
        {
            this.classQName = QName.resolveToQName(this.namespaceService, this.className);
            if (this.classQName == null)
            {
                throw new IllegalStateException(this.className + " cannot be resolved to a qualified name");
            }
            final ClassDefinition cls = this.dictionaryService.getClass(this.classQName);
            if (cls == null)
            {
                throw new IllegalStateException(this.className + " is not a class defined in the dictionary model");
            }
        }

        property = this.classQName != null ? this.dictionaryService.getProperty(this.classQName, this.propertyQName) : property;
        final Optional<ListOfValuesConstraint> lovConstraint = property.getConstraints().stream().map(ConstraintDefinition::getConstraint)
                .map(constraint -> constraint instanceof RegisteredConstraint
                        ? ((RegisteredConstraint) constraint).getRegisteredConstraint()
                        : constraint)
                .filter(constraint -> ListOfValuesConstraint.CONSTRAINT_TYPE.equals(constraint.getType()))
                .filter(constraint -> constraint instanceof ListOfValuesConstraint).map(ListOfValuesConstraint.class::cast).findFirst();
        if (!lovConstraint.isPresent())
        {
            throw new IllegalStateException(property + " does not provide a list-of-values constraint");
        }
        this.constraint = lovConstraint.get();

        this.init();
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
     * @param className
     *            the className to set
     */
    public void setClassName(final String className)
    {
        this.className = className;
    }

    /**
     * @param propertyName
     *            the propertyName to set
     */
    public void setPropertyName(final String propertyName)
    {
        this.propertyName = propertyName;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected Map<String, String> getAllowableValuesImpl()
    {
        final List<String> allowedValues = this.constraint.getAllowedValues();
        final Map<String, String> allowedValuesWithLabels = new HashMap<>();
        allowedValues.forEach(val -> allowedValuesWithLabels.put(val, this.constraint.getDisplayLabel(val, this.dictionaryService)));
        return allowedValuesWithLabels;
    }
}
