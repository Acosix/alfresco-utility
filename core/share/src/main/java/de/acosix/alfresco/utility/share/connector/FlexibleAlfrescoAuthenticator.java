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
package de.acosix.alfresco.utility.share.connector;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.alfresco.web.site.servlet.MTAuthenticationFilter;
import org.alfresco.web.site.servlet.SlingshotAlfrescoConnector;
import org.springframework.extensions.surf.ServletUtil;
import org.springframework.extensions.webscripts.connector.AlfrescoAuthenticator;
import org.springframework.extensions.webscripts.connector.ConnectorSession;

/**
 * Instances of this class provide ad-hoc authentication / handshake capabilities for Alfresco Share and Content Services deployment
 * scenarios which are not so clear-cut as what Alfresco assumes in their defaults / documentation. The primary change applies to the scope
 * of scenarios in which this connector determines a user to be authenticated:
 * <ul>
 * <li>presence of an authentication ticket in the connector session (inherited from base class)</li>
 * <li>presence of a remote user header in the current request</li>
 * <li>presence of a backend session cookie in the connector session (indicating session-based SSO mechanisms)</li>
 * </ul>
 *
 * Instances of this class continue to only support ad-hoc authentication / handshake when the user has authenticated using the Share login
 * form.
 *
 * @author Axel Faust
 */
public class FlexibleAlfrescoAuthenticator extends AlfrescoAuthenticator
{

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isAuthenticated(final String endpoint, final ConnectorSession connectorSession)
    {
        boolean authenticated = super.isAuthenticated(endpoint, connectorSession);

        if (!authenticated)
        {
            authenticated = this.isRemoteUserAuthenticated(connectorSession);
        }

        if (!authenticated)
        {
            if (connectorSession != null)
            {
                final List<String> cookieNames = Arrays.asList(connectorSession.getCookieNames());
                // TODO make cookie name to check configurable
                // authenticator is unfortunately instantiated per reflection
                authenticated = cookieNames.contains("JSESSIONID");
            }
        }

        return authenticated;
    }

    protected boolean isRemoteUserAuthenticated(final ConnectorSession connectorSession)
    {
        boolean authenticated = false;
        final String userHeader = connectorSession.getParameter(SlingshotAlfrescoConnector.CS_PARAM_USER_HEADER);
        if (userHeader != null)
        {
            HttpServletRequest req = ServletUtil.getRequest();
            if (req == null)
            {
                req = MTAuthenticationFilter.getCurrentServletRequest();
            }
            if (req != null)
            {
                String user = req.getHeader(userHeader);
                if (user == null)
                {
                    user = req.getRemoteUser();
                }
                if (user != null)
                {
                    authenticated = true;
                }
            }
        }
        return authenticated;
    }
}
