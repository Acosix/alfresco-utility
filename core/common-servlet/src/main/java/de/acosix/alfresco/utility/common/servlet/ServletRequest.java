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

import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

/**
 * An abstraction of the two Servlet API variants' ServletRequest API. This abstraction does not provide all of the operations present in
 * the underlying implementations - only those commonly used and required to bridge functionality for different versions of ACS.
 *
 * @author Axel Faust
 */
public interface ServletRequest
{

    /**
     * Returns the value of a request parameter as a <code>String</code>,
     * or <code>null</code> if the parameter does not exist. Request parameters
     * are extra information sent with the request. For HTTP servlets,
     * parameters are contained in the query string or posted form data.
     *
     * <p>
     * You should only use this method when you are sure the
     * parameter has only one value. If the parameter might have
     * more than one value, use {@link #getParameterValues}.
     *
     * <p>
     * If you use this method with a multivalued
     * parameter, the value returned is equal to the first value
     * in the array returned by <code>getParameterValues</code>.
     *
     * <p>
     * If the parameter data was sent in the request body, such as occurs
     * with an HTTP POST request, then reading the body directly can
     * interfere with the execution of this method.
     *
     * @param name
     *     a <code>String</code> specifying the name of the parameter
     *
     * @return a <code>String</code> representing the single value of
     * the parameter
     *
     * @see #getParameterValues
     */
    String getParameter(String name);

    /**
     *
     * Returns an <code>Enumeration</code> of <code>String</code>
     * objects containing the names of the parameters contained
     * in this request. If the request has
     * no parameters, the method returns an empty <code>Enumeration</code>.
     *
     * @return an <code>Enumeration</code> of <code>String</code>
     * objects, each <code>String</code> containing the name of
     * a request parameter; or an empty <code>Enumeration</code>
     * if the request has no parameters
     */
    Enumeration<String> getParameterNames();

    /**
     * Returns an array of <code>String</code> objects containing
     * all of the values the given request parameter has, or
     * <code>null</code> if the parameter does not exist.
     *
     * <p>
     * If the parameter has a single value, the array has a length
     * of 1.
     *
     * @param name
     *     a <code>String</code> containing the name of
     *     the parameter whose value is requested
     *
     * @return an array of <code>String</code> objects
     * containing the parameter's values
     *
     * @see #getParameter
     */
    String[] getParameterValues(String name);

    /**
     * Returns a java.util.Map of the parameters of this request.
     *
     * <p>
     * Request parameters are extra information sent with the request.
     * For HTTP servlets, parameters are contained in the query string or
     * posted form data.
     *
     * @return an immutable java.util.Map containing parameter names as
     * keys and parameter values as map values. The keys in the parameter
     * map are of type String. The values in the parameter map are of type
     * String array.
     */
    Map<String, String[]> getParameterMap();

    /**
     * Returns the name and version of the protocol the request uses
     * in the form <i>protocol/majorVersion.minorVersion</i>, for
     * example, HTTP/1.1. For HTTP servlets, the value
     * returned is the same as the value of the CGI variable
     * <code>SERVER_PROTOCOL</code>.
     *
     * @return a <code>String</code> containing the protocol
     * name and version number
     */
    String getProtocol();

    /**
     * Returns the name of the scheme used to make this request,
     * for example,
     * <code>http</code>, <code>https</code>, or <code>ftp</code>.
     * Different schemes have different rules for constructing URLs,
     * as noted in RFC 1738.
     *
     * @return a <code>String</code> containing the name
     * of the scheme used to make this request
     */
    String getScheme();

    /**
     * Returns the host name of the server to which the request was sent.
     * It is the value of the part before ":" in the <code>Host</code>
     * header value, if any, or the resolved server name, or the server IP
     * address.
     *
     * @return a <code>String</code> containing the name of the server
     */
    String getServerName();

    /**
     * Returns the port number to which the request was sent.
     * It is the value of the part after ":" in the <code>Host</code>
     * header value, if any, or the server port where the client connection
     * was accepted on.
     *
     * @return an integer specifying the port number
     */
    int getServerPort();

    /**
     * Returns the preferred <code>Locale</code> that the client will
     * accept content in, based on the Accept-Language header.
     * If the client request doesn't provide an Accept-Language header,
     * this method returns the default locale for the server.
     *
     * @return the preferred <code>Locale</code> for the client
     */
    Locale getLocale();

    /**
     * Returns an <code>Enumeration</code> of <code>Locale</code> objects
     * indicating, in decreasing order starting with the preferred locale, the
     * locales that are acceptable to the client based on the Accept-Language
     * header.
     * If the client request doesn't provide an Accept-Language header,
     * this method returns an <code>Enumeration</code> containing one
     * <code>Locale</code>, the default locale for the server.
     *
     * @return an <code>Enumeration</code> of preferred
     * <code>Locale</code> objects for the client
     */
    Enumeration<Locale> getLocales();

    /**
     *
     * Returns a boolean indicating whether this request was made using a
     * secure channel, such as HTTPS.
     *
     * @return a boolean indicating if the request was made using a
     * secure channel
     */
    boolean isSecure();

    /**
     * Returns an array containing all of the <code>Cookie</code>
     * objects the client sent with this request.
     * This method returns <code>null</code> if no cookies were sent.
     *
     * @return an array of all the <code>Cookies</code>
     * included with this request, or <code>null</code>
     * if the request has no cookies
     */
    Cookie[] getCookies();

    /**
     * Returns the value of the specified request header
     * as a <code>String</code>. If the request did not include a header
     * of the specified name, this method returns <code>null</code>.
     * If there are multiple headers with the same name, this method
     * returns the first head in the request.
     * The header name is case insensitive. You can use
     * this method with any request header.
     *
     * @param name
     *     a <code>String</code> specifying the
     *     header name
     *
     * @return a <code>String</code> containing the
     * value of the requested
     * header, or <code>null</code>
     * if the request does not
     * have a header of that name
     */
    String getHeader(String name);

    /**
     * Returns all the values of the specified request header
     * as an <code>Enumeration</code> of <code>String</code> objects.
     *
     * <p>
     * Some headers, such as <code>Accept-Language</code> can be sent
     * by clients as several headers each with a different value rather than
     * sending the header as a comma separated list.
     *
     * <p>
     * If the request did not include any headers
     * of the specified name, this method returns an empty
     * <code>Enumeration</code>.
     * The header name is case insensitive. You can use
     * this method with any request header.
     *
     * @param name
     *     a <code>String</code> specifying the
     *     header name
     *
     * @return an <code>Enumeration</code> containing
     * the values of the requested header. If
     * the request does not have any headers of
     * that name return an empty
     * enumeration. If
     * the container does not allow access to
     * header information, return null
     */
    Enumeration<String> getHeaders(String name);

    /**
     * Returns an enumeration of all the header names
     * this request contains. If the request has no
     * headers, this method returns an empty enumeration.
     *
     * <p>
     * Some servlet containers do not allow
     * servlets to access headers using this method, in
     * which case this method returns <code>null</code>
     *
     * @return an enumeration of all the
     * header names sent with this
     * request; if the request has
     * no headers, an empty enumeration;
     * if the servlet container does not
     * allow servlets to use this method,
     * <code>null</code>
     */
    Enumeration<String> getHeaderNames();

    /**
     * Returns the name of the HTTP method with which this
     * request was made, for example, GET, POST, or PUT.
     * Same as the value of the CGI variable REQUEST_METHOD.
     *
     * @return a <code>String</code>
     * specifying the name
     * of the method with which
     * this request was made
     */
    String getMethod();

    /**
     * Returns any extra path information associated with
     * the URL the client sent when it made this request.
     * The extra path information follows the servlet path
     * but precedes the query string and will start with
     * a "/" character.
     *
     * <p>
     * This method returns <code>null</code> if there
     * was no extra path information.
     *
     * <p>
     * Same as the value of the CGI variable PATH_INFO.
     *
     * @return a <code>String</code>, decoded by the
     * web container, specifying
     * extra path information that comes
     * after the servlet path but before
     * the query string in the request URL;
     * or <code>null</code> if the URL does not have
     * any extra path information
     */
    String getPathInfo();

    /**
     * Returns the portion of the request URI that indicates the context
     * of the request. The context path always comes first in a request
     * URI. The path starts with a "/" character but does not end with a "/"
     * character. For servlets in the default (root) context, this method
     * returns "". The container does not decode this string.
     *
     * <p>
     * It is possible that a servlet container may match a context by
     * more than one context path. In such cases this method will return the
     * actual context path used by the request and it may differ from the
     * path returned by the {@code getContextPath()} method.
     * The context path returned by {@code getContextPath()}
     * should be considered as the prime or preferred context path of the
     * application.
     *
     * @return a <code>String</code> specifying the
     * portion of the request URI that indicates the context
     * of the request
     */
    String getContextPath();

    /**
     * Returns the query string that is contained in the request
     * URL after the path. This method returns <code>null</code>
     * if the URL does not have a query string. Same as the value
     * of the CGI variable QUERY_STRING.
     *
     * @return a <code>String</code> containing the query
     * string or <code>null</code> if the URL
     * contains no query string. The value is not
     * decoded by the container.
     */
    String getQueryString();

    /**
     * Returns the login of the user making this request, if the
     * user has been authenticated, or <code>null</code> if the user
     * has not been authenticated.
     * Whether the user name is sent with each subsequent request
     * depends on the browser and type of authentication. Same as the
     * value of the CGI variable REMOTE_USER.
     *
     * @return a <code>String</code> specifying the login
     * of the user making this request, or <code>null</code>
     * if the user login is not known
     */
    String getRemoteUser();

    /**
     * Returns the part of this request's URL from the protocol
     * name up to the query string in the first line of the HTTP request.
     * The web container does not decode this String.
     * For example:
     *
     * <table summary="Examples of Returned Values">
     * <tr align=left>
     * <th>First line of HTTP request</th>
     * <th>Returned Value</th>
     * <tr>
     * <td>POST /some/path.html HTTP/1.1
     * <td>
     * <td>/some/path.html
     * <tr>
     * <td>GET http://foo.bar/a.html HTTP/1.0
     * <td>
     * <td>/a.html
     * <tr>
     * <td>HEAD /xyz?a=b HTTP/1.1
     * <td>
     * <td>/xyz
     * </table>
     *
     * @return a <code>String</code> containing
     * the part of the URL from the
     * protocol name up to the query string
     */
    String getRequestURI();

    /**
     * Returns the part of this request's URL that calls
     * the servlet. This path starts with a "/" character
     * and includes either the servlet name or a path to
     * the servlet, but does not include any extra path
     * information or a query string. Same as the value of
     * the CGI variable SCRIPT_NAME.
     *
     * <p>
     * This method will return an empty string ("") if the
     * servlet used to process this request was matched using
     * the "/*" pattern.
     *
     * @return a <code>String</code> containing
     * the name or path of the servlet being
     * called, as specified in the request URL,
     * decoded, or an empty string if the servlet
     * used to process the request is matched
     * using the "/*" pattern.
     */
    String getServletPath();

    /**
     * Returns the current <code>HttpSession</code>
     * associated with this request or, if there is no
     * current session and <code>create</code> is true, returns
     * a new session.
     *
     * <p>
     * If <code>create</code> is <code>false</code>
     * and the request has no valid <code>HttpSession</code>,
     * this method returns <code>null</code>.
     *
     * <p>
     * To make sure the session is properly maintained,
     * you must call this method before
     * the response is committed. If the container is using cookies
     * to maintain session integrity and is asked to create a new session
     * when the response is committed, an IllegalStateException is thrown.
     *
     * @param create
     *     <code>true</code> to create
     *     a new session for this request if necessary;
     *     <code>false</code> to return <code>null</code>
     *     if there's no current session
     *
     * @return the <code>HttpSession</code> associated
     * with this request or <code>null</code> if
     * <code>create</code> is <code>false</code>
     * and the request has no valid session
     *
     * @see #getSession()
     */
    Session getSession(boolean create);

    /**
     * Returns the current session associated with this request,
     * or if the request does not have a session, creates one.
     *
     * @return the <code>HttpSession</code> associated
     * with this request
     *
     * @see #getSession(boolean)
     */
    Session getSession();
}
