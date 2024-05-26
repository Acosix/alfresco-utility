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
package de.acosix.alfresco.utility.common.servlet;

import java.util.Enumeration;

/**
 *
 * @author Axel Faust
 */
public interface ServletHelperOperations
{

    /**
     * Returns the login of the user making this request, if the user has been authenticated, or <code>null</code> if the user has not
     * been authenticated. Whether the user name is sent with each subsequent request depends on the browser and type of authentication.
     * Same as the value of the CGI variable REMOTE_USER.
     *
     * @return a string specifying the login of the user making this request, or {@code null} if the user login is not known
     */
    String getRemoteUser();

    /**
     * Returns the value of the specified request header as a string. If the request did not include a header of the specified name,
     * this method returns <code>null</code>. If there are multiple headers with the same name, this method returns the first head in
     * the request. The header name is case insensitive. You can use this method with any request header.
     *
     * @param name
     *     a string specifying the header name
     * @return a string containing the value of the requested header, or {@code null} if the current request does not have a header of
     * that name
     */
    String getRequestHeader(String name);

    /**
     * Returns all values of the specified request header as a strings. If the request did not include a header of the specified name,
     * this method returns <code>null</code>. If there are multiple headers with the same name, this method returns the first head in
     * the request. The header name is case insensitive. You can use this method with any request header.
     *
     * @param name
     *     a string specifying the header name
     * @return an enumeration of strings containing the values of the requested header, or {@code null} if the current request does not
     * have a header of that name
     */
    Enumeration<String> getRequestHeaders(String name);

    /**
     * Returns all the header names set in the current request.
     *
     * @return an enumeration of all the header names sent with the current request
     */
    Enumeration<String> getRequestHeaderNames();

    /**
     * Returns the value of an attribute with the specified name in an active session
     *
     * @param name
     *     a string specifying the name of the attribute
     * @return the value of the requested attribute, or {@code null} if the active session does not have an attribute of that name
     */
    Object getSessionAttribute(String name);
}
