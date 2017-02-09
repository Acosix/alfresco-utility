/*
 * Copyright 2016, 2017 Acosix GmbH
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
package de.acosix.alfresco.utility.repo.entities;

import java.util.List;
import java.util.Map;

import org.alfresco.rest.api.model.SiteImpl;

/**
 * This site entity can be used for calling site-related internal web scripts of Alfresco and extension modules via JAX-RS based client
 * tooling and a JSON entity provider / converter. This entity is not compatible with the public API entity {@link SiteImpl}. Some of the
 * properties included in this class are only relevant for response data.
 *
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class Site
{

    public static class CustomProperty
    {

        private String name;

        private Object value;

        private String type;

        private String title;

        /**
         * @return the name
         */
        public String getName()
        {
            return this.name;
        }

        /**
         * @param name
         *            the name to set
         */
        public void setName(final String name)
        {
            this.name = name;
        }

        /**
         * @return the value
         */
        public Object getValue()
        {
            return this.value;
        }

        /**
         * @param value
         *            the value to set
         */
        public void setValue(final Object value)
        {
            this.value = value;
        }

        /**
         * @return the type
         */
        public String getType()
        {
            return this.type;
        }

        /**
         * @param type
         *            the type to set
         */
        public void setType(final String type)
        {
            this.type = type;
        }

        /**
         * @return the title
         */
        public String getTitle()
        {
            return this.title;
        }

        /**
         * @param title
         *            the title to set
         */
        public void setTitle(final String title)
        {
            this.title = title;
        }

    }

    private String shortName;

    private String sitePreset;

    private String title;

    private String description;

    private String visibility;

    private Boolean isPublic;

    private String url;

    private String node;

    private String tagScope;

    private Map<String, CustomProperty> customProperties;

    private String siteRole;

    private List<String> siteManagers;

    /**
     * @return the shortName
     */
    public String getShortName()
    {
        return this.shortName;
    }

    /**
     * @param shortName
     *            the shortName to set
     */
    public void setShortName(final String shortName)
    {
        this.shortName = shortName;
    }

    /**
     * @return the sitePreset
     */
    public String getSitePreset()
    {
        return this.sitePreset;
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
     * @return the title
     */
    public String getTitle()
    {
        return this.title;
    }

    /**
     * @param title
     *            the title to set
     */
    public void setTitle(final String title)
    {
        this.title = title;
    }

    /**
     * @return the description
     */
    public String getDescription()
    {
        return this.description;
    }

    /**
     * @param description
     *            the description to set
     */
    public void setDescription(final String description)
    {
        this.description = description;
    }

    /**
     * @return the visibility
     */
    public String getVisibility()
    {
        return this.visibility;
    }

    /**
     * @param visibility
     *            the visibility to set
     */
    public void setVisibility(final String visibility)
    {
        this.visibility = visibility;
    }

    /**
     * @return the isPublic
     */
    public Boolean getIsPublic()
    {
        return this.isPublic;
    }

    /**
     * @param isPublic
     *            the isPublic to set
     */
    public void setIsPublic(final Boolean isPublic)
    {
        this.isPublic = isPublic;
    }

    /**
     * @return the url
     */
    public String getUrl()
    {
        return this.url;
    }

    /**
     * @param url
     *            the url to set
     */
    public void setUrl(final String url)
    {
        this.url = url;
    }

    /**
     * @return the node
     */
    public String getNode()
    {
        return this.node;
    }

    /**
     * @param node
     *            the node to set
     */
    public void setNode(final String node)
    {
        this.node = node;
    }

    /**
     * @return the tagScope
     */
    public String getTagScope()
    {
        return this.tagScope;
    }

    /**
     * @param tagScope
     *            the tagScope to set
     */
    public void setTagScope(final String tagScope)
    {
        this.tagScope = tagScope;
    }

    /**
     * @return the customProperties
     */
    public Map<String, CustomProperty> getCustomProperties()
    {
        return this.customProperties;
    }

    /**
     * @param customProperties
     *            the customProperties to set
     */
    public void setCustomProperties(final Map<String, CustomProperty> customProperties)
    {
        this.customProperties = customProperties;
    }

    /**
     * @return the siteRole
     */
    public String getSiteRole()
    {
        return this.siteRole;
    }

    /**
     * @param siteRole
     *            the siteRole to set
     */
    public void setSiteRole(final String siteRole)
    {
        this.siteRole = siteRole;
    }

    /**
     * @return the siteManagers
     */
    public List<String> getSiteManagers()
    {
        return this.siteManagers;
    }

    /**
     * @param siteManagers
     *            the siteManagers to set
     */
    public void setSiteManagers(final List<String> siteManagers)
    {
        this.siteManagers = siteManagers;
    }

}
