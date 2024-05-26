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
package de.acosix.alfresco.utility.core.share.javax.servlet;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.alfresco.web.site.servlet.MTAuthenticationFilter;
import org.springframework.extensions.surf.ServletUtil;

import de.acosix.alfresco.utility.common.servlet.ServletHelperOperations;

/**
 *
 * @author Axel Faust
 */
public class ServletHelperOperationsImpl implements ServletHelperOperations
{

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRemoteUser()
    {
        final HttpServletRequest rq = this.getRequest();
        return rq != null ? rq.getRemoteUser() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRequestHeader(final String name)
    {
        final HttpServletRequest rq = this.getRequest();
        return rq != null ? rq.getHeader(name) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Enumeration<String> getRequestHeaders(final String name)
    {
        final HttpServletRequest rq = this.getRequest();
        return rq != null ? rq.getHeaders(name) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Enumeration<String> getRequestHeaderNames()
    {
        final HttpServletRequest rq = this.getRequest();
        return rq != null ? rq.getHeaderNames() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getSessionAttribute(final String name)
    {
        final HttpServletRequest rq = this.getRequest();
        final HttpSession session = rq != null ? rq.getSession() : null;
        return session != null ? session.getAttribute(name) : null;
    }

    private HttpServletRequest getRequest()
    {
        HttpServletRequest rq = ServletUtil.getRequest();
        if (rq == null)
        {
            rq = MTAuthenticationFilter.getCurrentServletRequest();
        }
        return rq;
    }

}
