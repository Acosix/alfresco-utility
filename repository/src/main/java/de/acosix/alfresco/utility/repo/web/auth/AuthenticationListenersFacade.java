/*
 * Copyright 2017 Acosix GmbH
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
package de.acosix.alfresco.utility.repo.web.auth;

import java.util.List;

import org.alfresco.repo.web.auth.AuthenticationListener;
import org.alfresco.repo.web.auth.WebCredentials;
import org.alfresco.repo.web.scripts.servlet.BasicHttpAuthenticatorFactory;
import org.alfresco.repo.webdav.auth.BaseAuthenticationFilter;
import org.alfresco.util.PropertyCheck;
import org.springframework.beans.factory.InitializingBean;

/**
 * This facade provides delegation of authentication events to multiple listeners. This is a necessity since the default Alfresco
 * {@link BaseAuthenticationFilter#setAuthenticationListener(AuthenticationListener) base authentication filter} and
 * {@link BasicHttpAuthenticatorFactory#setAuthenticationListener(AuthenticationListener) basic HTTP authenticator factory} only support
 * registration of individual listener instances, and are already pre-configured to use a listener by core Spring configuration. This class
 * allows replacement of the default configuration while retaining the configured listener and adding additional, custom ones.
 *
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class AuthenticationListenersFacade implements AuthenticationListener, InitializingBean
{

    protected List<AuthenticationListener> authenticationListeners;

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "authenticationListeners", this.authenticationListeners);
    }

    /**
     * @param authenticationListeners
     *            the authenticationListeners to set
     */
    public void setAuthenticationListeners(final List<AuthenticationListener> authenticationListeners)
    {
        this.authenticationListeners = authenticationListeners;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void userAuthenticated(final WebCredentials credentials)
    {
        this.authenticationListeners.forEach(x -> {
            x.userAuthenticated(credentials);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void authenticationFailed(final WebCredentials credentials, final Exception ex)
    {
        this.authenticationListeners.forEach(x -> {
            x.authenticationFailed(credentials, ex);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void authenticationFailed(final WebCredentials credentials)
    {
        this.authenticationListeners.forEach(x -> {
            x.authenticationFailed(credentials);
        });
    }

}
