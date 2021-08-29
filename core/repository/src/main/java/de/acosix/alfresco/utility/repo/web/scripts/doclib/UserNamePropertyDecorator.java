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

import org.alfresco.repo.jscript.app.UsernamePropertyDecorator;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.PropertyCheck;
import org.json.simple.JSONArray;
import org.json.simple.JSONAware;
import org.springframework.beans.factory.InitializingBean;

/**
 * This implementation of a property decorator builds upon the default user name decorator of Alfresco and adds support of multi-valued
 * properties holding names of Alfresco users.
 *
 * @author Axel Faust
 */
public class UserNamePropertyDecorator extends UsernamePropertyDecorator implements InitializingBean
{

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "namespaceService", this.namespaceService);
        PropertyCheck.mandatory(this, "nodeService", this.nodeService);
        PropertyCheck.mandatory(this, "permissionService", this.permissionService);
        PropertyCheck.mandatory(this, "jsonConversionComponent", this.jsonConversionComponent);
        // cannot check personService due to visibility - duplicating member just for check would be excessive

        super.init();
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
                final JSONAware decoratedEl = super.decorate(propertyName, nodeRef, v);
                ((JSONArray) result).add(decoratedEl);
            });
        }
        else
        {
            result = super.decorate(propertyName, nodeRef, value);
        }

        return result;
    }
}
