/*
 * Copyright 2016 - 2025 Acosix GmbH
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
package de.acosix.alfresco.utility.common.jakarta.servlet;

import java.io.IOException;
import java.lang.reflect.Proxy;

import de.acosix.alfresco.utility.common.servlet.Filter;
import de.acosix.alfresco.utility.common.servlet.ServletRequest;
import de.acosix.alfresco.utility.common.servlet.ServletResponse;
import de.acosix.alfresco.utility.common.servlet.WrappedServletException;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Axel Faust
 */
public class FilterBridge implements jakarta.servlet.Filter
{

    private final Filter filter;

    /**
     * Creates a new instance of this class.
     *
     * @param filter
     *     the actual filter logic
     */
    public FilterBridge(final Filter filter)
    {
        this.filter = filter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final FilterConfig filterConfig) throws ServletException
    {
        // NO-OP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doFilter(final jakarta.servlet.ServletRequest request, final jakarta.servlet.ServletResponse response,
            final jakarta.servlet.FilterChain chain) throws IOException, ServletException
    {
        final ServletRequest req = (ServletRequest) Proxy.newProxyInstance(this.getClass().getClassLoader(),
                new Class<?>[] { request instanceof HttpServletRequest ? HttpServletRequest.class : ServletRequest.class },
                new ServletRequestHandler(request));
        final ServletResponse res = (ServletResponse) Proxy.newProxyInstance(this.getClass().getClassLoader(),
                new Class<?>[] { response instanceof HttpServletResponse ? HttpServletResponse.class : ServletResponse.class },
                new ServletResponseHandler(response));
        final FilterChainImpl chainImpl = new FilterChainImpl(chain);
        try
        {
            this.filter.doFilter(req, res, chainImpl);
        }
        catch (final WrappedServletException ex)
        {
            throw (ServletException) ex.getCause();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy()
    {
        // NO-OP
    }

}
