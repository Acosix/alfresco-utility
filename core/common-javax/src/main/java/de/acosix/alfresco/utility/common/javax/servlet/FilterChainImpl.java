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
package de.acosix.alfresco.utility.common.javax.servlet;

import java.io.IOException;
import java.lang.reflect.Proxy;

import javax.servlet.ServletException;

import de.acosix.alfresco.utility.common.servlet.FilterChain;
import de.acosix.alfresco.utility.common.servlet.ServletRequest;
import de.acosix.alfresco.utility.common.servlet.ServletResponse;
import de.acosix.alfresco.utility.common.servlet.WrappedServletException;

/**
 * Instances of this class handle invocations of a {@link FilterChain}.
 *
 * @author Axel Faust
 */
public class FilterChainImpl implements FilterChain
{

    private final javax.servlet.FilterChain filterChain;

    /**
     * Creates a new instance of this class.
     *
     * @param filterChain
     *     the backing instance
     */
    public FilterChainImpl(final javax.servlet.FilterChain filterChain)
    {
        this.filterChain = filterChain;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response) throws IOException, WrappedServletException
    {
        final javax.servlet.ServletRequest req = ((ServletRequestHandler) Proxy.getInvocationHandler(request)).getBackingInstance();
        final javax.servlet.ServletResponse res = ((ServletResponseHandler) Proxy.getInvocationHandler(request)).getBackingInstance();

        try
        {
            this.filterChain.doFilter(req, res);
        }
        catch (final ServletException ex)
        {
            throw new WrappedServletException(ex);
        }

    }

}
