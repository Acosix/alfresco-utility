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
package de.acosix.alfresco.utility.repo.web.scripts.doclib;

import java.io.Serializable;
import java.util.Collection;

import org.alfresco.repo.security.authority.AuthorityDAO;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.PropertyCheck;
import org.json.simple.JSONArray;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.InitializingBean;

/**
 * This implementation of a property decorator handles both group and user authority names.
 *
 * @author Axel Faust
 */
public class AuthorityNamePropertyDecorator extends UserNamePropertyDecorator implements InitializingBean
{

    protected AuthorityDAO authorityDAO;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "authorityDAO", this.authorityDAO);

        super.afterPropertiesSet();
    }

    /**
     * @param authorityDAO
     *     the authorityDAO to set
     */
    public void setAuthorityDAO(final AuthorityDAO authorityDAO)
    {
        this.authorityDAO = authorityDAO;
    }

    /**
     *
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public JSONAware decorate(final QName propertyName, final NodeRef nodeRef, final Serializable value)
    {
        final JSONAware result;
        if (value instanceof Collection<?>)
        {
            result = new JSONArray();
            ((Collection<?>) value).stream().map(Serializable.class::cast).forEach(v -> {
                final JSONAware decoratedEl = this.decorate(propertyName, nodeRef, v);
                ((JSONArray) result).add(decoratedEl);
            });
        }
        else
        {
            final String strValue = DefaultTypeConverter.INSTANCE.convert(String.class, value);
            switch (AuthorityType.getAuthorityType(strValue))
            {
                case GROUP:
                {
                    final JSONObject obj = new JSONObject();
                    final String shortName = this.authorityDAO.getShortName(strValue);
                    obj.put("value", strValue);
                    obj.put("name", shortName);

                    final String displayName = this.authorityDAO.getAuthorityDisplayName(strValue);
                    if (displayName != null)
                    {
                        obj.put("displayName", displayName);
                    }
                    result = obj;
                }
                    break;
                case USER:
                    result = super.decorate(propertyName, nodeRef, value);
                    break;
                default:
                {
                    final JSONObject obj = new JSONObject();
                    obj.put("name", strValue);
                    result = obj;
                }
            }
        }

        return result;
    }
}
