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
package de.acosix.alfresco.utility.common.servlet;

import java.io.IOException;

/**
 * Instances of this class denote a filter implementation using the abstractions provided by the interfaces in this packages. It is used in
 * lieu of a specific Servlet API's interface to bridge ACS versions.
 * 
 * @author Axel Faust
 */
public interface Filter
{

    /**
     * The <code>doFilter</code> method of the Filter is called by the
     * container each time a request/response pair is passed through the
     * chain due to a client request for a resource at the end of the chain.
     * The FilterChain passed in to this method allows the Filter to pass
     * on the request and response to the next entity in the chain.
     *
     * <p>
     * A typical implementation of this method would follow the following
     * pattern:
     * <ol>
     * <li>Examine the request
     * <li>Optionally wrap the request object with a custom implementation to
     * filter content or headers for input filtering
     * <li>Optionally wrap the response object with a custom implementation to
     * filter content or headers for output filtering
     * <li>
     * <ul>
     * <li><strong>Either</strong> invoke the next entity in the chain
     * using the FilterChain object
     * (<code>chain.doFilter()</code>),
     * <li><strong>or</strong> not pass on the request/response pair to
     * the next entity in the filter chain to
     * block the request processing
     * </ul>
     * <li>Directly set headers on the response after invocation of the
     * next entity in the filter chain.
     * </ol>
     *
     * @param request
     *     the <code>ServletRequest</code> object contains the client's request
     * @param response
     *     the <code>ServletResponse</code> object contains the filter's response
     * @param chain
     *     the <code>FilterChain</code> for invoking the next filter or the resource
     * @throws IOException
     *     if an I/O related error has occurred during the processing
     * @throws WrappedServletException
     *     if an exception occurs that interferes with the
     *     filter's normal operation
     */
    void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, WrappedServletException;
}
