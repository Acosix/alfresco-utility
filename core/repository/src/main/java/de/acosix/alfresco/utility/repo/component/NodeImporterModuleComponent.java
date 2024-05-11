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

import java.util.Collections;
import java.util.Locale;
import java.util.Properties;

import org.alfresco.repo.importer.ImporterBootstrap;
import org.alfresco.repo.module.ImporterModuleComponent;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.util.PropertyCheck;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.extensions.surf.util.I18NUtil;

/**
 * This class provides a partially enhanced variant of the Alfresco default importer module component. The improvements include:
 * <ul>
 * <li>cleanup of importer state to avoid issues with duplicate execution on context refresh</li>
 * <li>ability to disable all behaviours during bootstrap without having to set up a complex, custom importer + importer component...</li>
 * <li>ability to specify specific locale for import execution</li>
 * <li>ability to set a simple skip flag for disabling the component (e.g. in specific environments)</lI>
 * </ul>
 *
 * @author Axel Faust
 */
public class NodeImporterModuleComponent extends ImporterModuleComponent implements InitializingBean
{

    protected ImporterBootstrap importer;

    protected BehaviourFilter behaviourFilter;

    protected Locale locale;

    protected boolean disableBehaviours;

    protected boolean skip;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "importer", this.importer);
        if (this.disableBehaviours)
        {
            PropertyCheck.mandatory(this, "behaviourFilter", this.behaviourFilter);
        }

        if (this.skip)
        {
            // use lowest possible version number to effectively disable this component
            this.setAppliesFromVersion("0");
            this.setAppliesToVersion("0");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setImporter(final ImporterBootstrap importer)
    {
        super.setImporter(importer);
        this.importer = importer;
    }

    /**
     * @param behaviourFilter
     *            the behaviourFilter to set
     */
    public void setBehaviourFilter(final BehaviourFilter behaviourFilter)
    {
        this.behaviourFilter = behaviourFilter;
    }

    /**
     * @param locale
     *            the locale to set
     */
    public void setLocale(final Locale locale)
    {
        this.locale = locale;
    }

    /**
     * @param disableBehaviours
     *            the disableBehaviours to set
     */
    public void setDisableBehaviours(final boolean disableBehaviours)
    {
        this.disableBehaviours = disableBehaviours;
    }

    /**
     * @param skip
     *            the skip to set
     */
    public void setSkip(final boolean skip)
    {
        this.skip = skip;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void executeInternal() throws Throwable
    {
        final Properties baseConfiguration = this.importer.getConfiguration();

        final Locale currentLocale = I18NUtil.getLocaleOrNull();
        if (this.locale != null)
        {
            I18NUtil.setLocale(this.locale);
        }

        boolean behavioursWereEnabled = true;
        if (this.disableBehaviours)
        {
            behavioursWereEnabled = this.behaviourFilter.isEnabled();
            this.behaviourFilter.disableBehaviour();
        }
        try
        {
            super.executeInternal();
            ModuleComponentFlags.flagTransactionalChanges();
        }
        finally
        {
            if (this.disableBehaviours && behavioursWereEnabled)
            {
                this.behaviourFilter.enableBehaviour();
            }

            I18NUtil.setLocale(currentLocale);

            // bootstrap beans are AbstractLifecycleBean instances
            // avoid executing the same bootstrap on context refresh
            this.importer.setBootstrapViews(Collections.emptyList());
            this.importer.setConfiguration(baseConfiguration);
        }
    }
}
