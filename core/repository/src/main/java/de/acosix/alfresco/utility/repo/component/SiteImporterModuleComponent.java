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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.importer.ImporterBootstrap;
import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.cmr.site.SiteVisibility;
import org.alfresco.util.PropertyCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.extensions.surf.util.I18NUtil;

/**
 * @author Axel Faust
 */
public class SiteImporterModuleComponent extends AbstractModuleComponent implements InitializingBean
{

    private static final Logger LOGGER = LoggerFactory.getLogger(SiteImporterModuleComponent.class);

    protected BehaviourFilter behaviourFilter;

    protected NodeService nodeService;

    protected AuthorityService authorityService;

    protected SiteService siteService;

    protected ImporterBootstrap spacesBootstrap;

    protected Properties groupsView;

    protected Properties contentView;

    protected String siteName;

    protected String sitePreset;

    protected String siteTitle;

    protected String siteVisibility;

    protected Locale locale;

    protected boolean skipIfSiteExists;

    protected String defaultManagerUserName = AuthenticationUtil.getAdminUserName();

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "behaviourFilter", this.behaviourFilter);
        PropertyCheck.mandatory(this, "nodeService", this.nodeService);
        PropertyCheck.mandatory(this, "authorityService", this.authorityService);
        PropertyCheck.mandatory(this, "siteService", this.siteService);
        PropertyCheck.mandatory(this, "spacesBootstrap", this.spacesBootstrap);
        PropertyCheck.mandatory(this, "siteName", this.siteName);
        PropertyCheck.mandatory(this, "sitePreset", this.sitePreset);
        PropertyCheck.mandatory(this, "siteTitle", this.siteTitle);
        PropertyCheck.mandatory(this, "defaultManagerUserName", this.defaultManagerUserName);
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
     * @param nodeService
     *            the nodeService to set
     */
    public void setNodeService(final NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    /**
     * @param authorityService
     *            the authorityService to set
     */
    public void setAuthorityService(final AuthorityService authorityService)
    {
        this.authorityService = authorityService;
    }

    /**
     * @param siteService
     *            the siteService to set
     */
    public void setSiteService(final SiteService siteService)
    {
        this.siteService = siteService;
    }

    /**
     * @param spacesBootstrap
     *            the spacesBootstrap to set
     */
    public void setSpacesBootstrap(final ImporterBootstrap spacesBootstrap)
    {
        this.spacesBootstrap = spacesBootstrap;
    }

    /**
     * @param groupsView
     *            the groupsView to set
     */
    public void setGroupsView(final Properties groupsView)
    {
        this.groupsView = groupsView;
    }

    /**
     * @param contentView
     *            the contentView to set
     */
    public void setContentView(final Properties contentView)
    {
        this.contentView = contentView;
    }

    /**
     * @param siteName
     *            the siteName to set
     */
    public void setSiteName(final String siteName)
    {
        this.siteName = siteName;
    }

    /**
     * @param sitePreset
     *            the sitePreset to set
     */
    public void setSitePreset(final String sitePreset)
    {
        this.sitePreset = sitePreset;
    }

    /**
     * @param siteTitle
     *            the siteTitle to set
     */
    public void setSiteTitle(final String siteTitle)
    {
        this.siteTitle = siteTitle;
    }

    /**
     * @param siteVisibility
     *            the siteVisibility to set
     */
    public void setSiteVisibility(final String siteVisibility)
    {
        this.siteVisibility = siteVisibility;
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
     * @param skipIfSiteExists
     *            the skipIfSiteExists to set
     */
    public void setSkipIfSiteExists(final boolean skipIfSiteExists)
    {
        this.skipIfSiteExists = skipIfSiteExists;
    }

    /**
     * @param defaultManagerUserName
     *            the defaultManagerUserName to set
     */
    public void setDefaultManagerUserName(final String defaultManagerUserName)
    {
        this.defaultManagerUserName = defaultManagerUserName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    // partially taken from Alfresco SiteLoadPatch, but adapted / simplified quite a bit
    protected void executeInternal() throws Throwable
    {
        SiteInfo site = this.siteService.getSite(this.siteName);
        if (site != null && this.skipIfSiteExists)
        {
            LOGGER.info(
                    "Site {} already exists and module component {} configured to skip if site exists - treating component as 'sucessfully executed'",
                    this.siteName, this.getName());
        }
        else
        {
            LOGGER.info("Bootstrapping site {}", this.siteName);

            final Properties baseConfiguration = this.spacesBootstrap.getConfiguration();

            AuthenticationUtil.pushAuthentication();
            final Locale currentLocale = I18NUtil.getLocaleOrNull();
            if (this.locale != null)
            {
                I18NUtil.setLocale(this.locale);
            }
            // specific user instead of System is unfortunately required due to internal coding of SiteService
            // (there must be an initial manager)
            AuthenticationUtil.setFullyAuthenticatedUser(this.defaultManagerUserName);
            try
            {
                SiteVisibility visibility = SiteVisibility.PUBLIC;
                if (this.siteVisibility != null)
                {
                    visibility = SiteVisibility.valueOf(this.siteVisibility.toUpperCase(Locale.ENGLISH));
                }

                final boolean newSite = site == null;
                site = this.siteService.createSite(this.sitePreset, this.siteName, this.siteTitle, null, visibility);

                if (this.groupsView != null)
                {
                    this.doGroupImport(this.groupsView.getProperty("location"));
                }

                if (this.contentView != null)
                {
                    if (newSite)
                    {
                        // we expect bootstraps to contain the full structure of the site including itself
                        // so we delete the site node, but only if created earlier (existing site will not be deleted - in that case the
                        // content view should aim to update)
                        this.behaviourFilter.disableBehaviour(site.getNodeRef(), ContentModel.ASPECT_UNDELETABLE);
                        try
                        {
                            LOGGER.debug("Deleting empty shell of site {} before running content bootstrap", this.siteName);
                            this.nodeService.addAspect(site.getNodeRef(), ContentModel.ASPECT_TEMPORARY, null);
                            this.nodeService.deleteNode(site.getNodeRef());
                        }
                        finally
                        {
                            this.behaviourFilter.enableBehaviour(site.getNodeRef(), ContentModel.ASPECT_UNDELETABLE);
                        }
                    }

                    this.spacesBootstrap.setBootstrapViews(Collections.singletonList(this.contentView));

                    // pass configured name etc. via configuration for consistency
                    // avoid modifying configuration by reference since multiple
                    // bootstrap beans may share same instance
                    final Properties configuration = new Properties(baseConfiguration);
                    configuration.put("siteName", this.siteName);
                    configuration.put("siteTitle", this.siteTitle);
                    configuration.put("sitePreset", this.sitePreset);
                    configuration.put("siteVisibility", visibility.name());
                    this.spacesBootstrap.setConfiguration(configuration);

                    LOGGER.debug("Running content bootstrap for site {}", this.siteName);
                    this.spacesBootstrap.bootstrap();
                }

                ModuleComponentFlags.flagTransactionalChanges();
            }
            finally
            {
                I18NUtil.setLocale(currentLocale);
                AuthenticationUtil.popAuthentication();

                // bootstrap beans are AbstractLifecycleBean instances
                // avoid executing the same bootstrap on context refresh
                this.spacesBootstrap.setBootstrapViews(Collections.emptyList());
                this.spacesBootstrap.setConfiguration(baseConfiguration);
            }
        }
    }

    // taken from Alfresco SiteLoadPatch
    protected void doGroupImport(final String location) throws Throwable
    {
        LOGGER.debug("Importing site {} role memberships from {}", this.siteName, location);
        final File groupFile = ImporterBootstrap.getFile(location);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(groupFile), "UTF-8"));

        String line;
        while ((line = reader.readLine()) != null)
        {
            final int splitAt = line.indexOf('=');
            if (splitAt == -1)
            {
                LOGGER.warn("Invalid group line {}", line);
                continue;
            }

            final String user = line.substring(0, splitAt);
            final Set<String> currentGroups = this.authorityService.getAuthoritiesForUser(user);

            final StringTokenizer groups = new StringTokenizer(line.substring(splitAt + 1), ",");
            while (groups.hasMoreTokens())
            {
                String group = groups.nextToken();
                group = group.replace("${siteName}", this.siteName);
                if (!currentGroups.contains(group))
                {
                    this.authorityService.addAuthority(group, user);
                    LOGGER.debug("Added user {} to group {}", user, group);
                }
            }
        }
        reader.close();
    }
}
