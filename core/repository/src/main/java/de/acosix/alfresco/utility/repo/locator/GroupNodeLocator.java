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
package de.acosix.alfresco.utility.repo.locator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.nodelocator.AbstractNodeLocator;
import org.alfresco.repo.security.authority.AuthorityDAO;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.util.ParameterCheck;
import org.alfresco.util.PropertyCheck;
import org.springframework.beans.factory.InitializingBean;

public class GroupNodeLocator extends AbstractNodeLocator implements InitializingBean
{

    public static final String NAME = "group";

    public static final String PARAM_NAME = "name";

    protected AuthorityDAO authorityDAO;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "authorityDAO", this.authorityDAO);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeRef getNode(final NodeRef source, final Map<String, Serializable> params)
    {
        // source NodeRef is ignored by this locator - resolution occurs exclusively based on parameters
        String name = DefaultTypeConverter.INSTANCE.convert(String.class, params.get(PARAM_NAME));
        ParameterCheck.mandatoryString("name", name);

        final AuthorityType authorityType = AuthorityType.getAuthorityType(name);
        // users are the only type without a prefix
        if (authorityType == AuthorityType.USER)
        {
            name = AuthorityType.GROUP.getPrefixString() + name;
        }
        else if (authorityType != AuthorityType.GROUP)
        {
            throw new AlfrescoRuntimeException("Name " + name + " does not denote a group");
        }

        return this.authorityDAO.getAuthorityNodeRefOrNull(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ParameterDefinition> getParameterDefinitions()
    {
        final List<ParameterDefinition> paramDefs = new ArrayList<>(2);
        paramDefs.add(new ParameterDefinitionImpl(PARAM_NAME, DataTypeDefinition.TEXT, true, "Group Name"));
        return paramDefs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName()
    {
        return NAME;
    }

    /**
     * @param authorityDAO
     *     the authorityDAO to set
     */
    public void setAuthorityDAO(final AuthorityDAO authorityDAO)
    {
        this.authorityDAO = authorityDAO;
    }

}
