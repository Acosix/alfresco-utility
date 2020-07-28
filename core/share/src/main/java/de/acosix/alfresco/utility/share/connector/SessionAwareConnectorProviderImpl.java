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
package de.acosix.alfresco.utility.share.connector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.alfresco.util.PropertyCheck;
import org.alfresco.web.site.servlet.MTAuthenticationFilter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.extensions.surf.ServletUtil;
import org.springframework.extensions.surf.WebFrameworkConnectorProvider;
import org.springframework.extensions.surf.exception.ConnectorProviderException;
import org.springframework.extensions.surf.exception.ConnectorServiceException;
import org.springframework.extensions.surf.site.AuthenticationUtil;
import org.springframework.extensions.surf.support.ThreadLocalRequestContext;
import org.springframework.extensions.webscripts.connector.Connector;
import org.springframework.extensions.webscripts.connector.ConnectorProvider;
import org.springframework.extensions.webscripts.connector.ConnectorProviderImpl;
import org.springframework.extensions.webscripts.connector.ConnectorService;

/**
 * Instances of this class provide {@link Connector} instances that have proper access to the established {@link ServletUtil#getSession()
 * HTTP session} of the current Slingshot request. The {@link ConnectorProviderImpl default implementation} used in Share is not
 * session-aware, and as a result, requests issued by web scripts or other framework components not explicitly creating a connector via a
 * {@link ConnectorService#getConnector(String, javax.servlet.http.HttpSession) session aware connector service operation} may not submit
 * any authentication details in the call to the remote backend.
 *
 * This class is similar in purpose to {@link WebFrameworkConnectorProvider}, albeit not solely reliant on the
 * {@link ThreadLocalRequestContext#getRequestContent() request context}, the state of the credential vault in the context, and instead
 * aware of alternative Share utilities to get access to the current {@link HttpServletRequest servlet request}.
 *
 * @author Axel Faust
 */
public class SessionAwareConnectorProviderImpl implements ConnectorProvider, InitializingBean
{

    protected ConnectorService connectorService;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "connectorService", this.connectorService);
    }

    /**
     * @param connectorService
     *            the connectorService to set
     */
    public void setConnectorService(final ConnectorService connectorService)
    {
        this.connectorService = connectorService;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Connector provide(final String endpoint) throws ConnectorProviderException
    {
        final Connector conn;
        try
        {
            HttpServletRequest rq = ServletUtil.getRequest();
            // weird workaround for any instance where servlet util does not have the request (yet)
            // (taken e.g. from SlingshotAlfrescoConnector)
            if (rq == null)
            {
                rq = MTAuthenticationFilter.getCurrentServletRequest();
            }

            if (rq != null)
            {
                final String userId = AuthenticationUtil.getUserId(rq);
                final HttpSession session = rq.getSession();
                conn = this.connectorService.getConnector(endpoint, userId, session);
            }
            else
            {
                conn = this.connectorService.getConnector(endpoint);
            }
        }
        catch (final ConnectorServiceException cse)
        {
            throw new ConnectorProviderException("Unable to provision connector for endpoint: " + endpoint, cse);
        }

        return conn;
    }
}
