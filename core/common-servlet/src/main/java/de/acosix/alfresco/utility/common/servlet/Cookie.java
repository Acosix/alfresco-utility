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

/**
 * An abstraction of the two Servlet API variants' Cookie API. This abstraction does not provide all of the oeprations present in
 * the underlying implementations - only those commonly used and required to bridge functionality for different versions of ACS.
 * 
 * @author Axel Faust
 */
public interface Cookie
{

    /**
     * Sets the maximum age in seconds for this Cookie.
     *
     * <p>
     * A positive value indicates that the cookie will expire
     * after that many seconds have passed. Note that the value is
     * the <i>maximum</i> age when the cookie will expire, not the cookie's
     * current age.
     *
     * <p>
     * A negative value means
     * that the cookie is not stored persistently and will be deleted
     * when the Web browser exits. A zero value causes the cookie
     * to be deleted.
     *
     * @param expiry
     *     an integer specifying the maximum age of the
     *     cookie in seconds; if negative, means
     *     the cookie is not stored; if zero, deletes
     *     the cookie
     *
     * @see #getMaxAge
     */
    void setMaxAge(int expiry);

    /**
     * Gets the maximum age in seconds of this Cookie.
     *
     * <p>
     * By default, <code>-1</code> is returned, which indicates that
     * the cookie will persist until browser shutdown.
     *
     * @return an integer specifying the maximum age of the
     * cookie in seconds; if negative, means
     * the cookie persists until browser shutdown
     *
     * @see #setMaxAge
     */
    int getMaxAge();

    /**
     * Specifies a path for the cookie
     * to which the client should return the cookie.
     *
     * <p>
     * The cookie is visible to all the pages in the directory
     * you specify, and all the pages in that directory's subdirectories.
     * A cookie's path must include the servlet that set the cookie,
     * for example, <i>/catalog</i>, which makes the cookie
     * visible to all directories on the server under <i>/catalog</i>.
     *
     * <p>
     * Consult RFC 2109 (available on the Internet) for more
     * information on setting path names for cookies.
     *
     *
     * @param uri
     *     a <code>String</code> specifying a path
     *
     * @see #getPath
     */
    void setPath(String uri);

    /**
     * Returns the path on the server
     * to which the browser returns this cookie. The
     * cookie is visible to all subpaths on the server.
     *
     * @return a <code>String</code> specifying a path that contains
     * a servlet name, for example, <i>/catalog</i>
     *
     * @see #setPath
     */
    String getPath();

    /**
     * Indicates to the browser whether the cookie should only be sent
     * using a secure protocol, such as HTTPS or SSL.
     *
     * <p>
     * The default value is <code>false</code>.
     *
     * @param flag
     *     if <code>true</code>, sends the cookie from the browser
     *     to the server only when using a secure protocol; if <code>false</code>,
     *     sent on any protocol
     *
     * @see #getSecure
     */
    void setSecure(boolean flag);

    /**
     * Returns <code>true</code> if the browser is sending cookies
     * only over a secure protocol, or <code>false</code> if the
     * browser can send cookies using any protocol.
     *
     * @return <code>true</code> if the browser uses a secure protocol,
     * <code>false</code> otherwise
     *
     * @see #setSecure
     */
    boolean getSecure();

    /**
     * Returns the name of the cookie. The name cannot be changed after
     * creation.
     *
     * @return the name of the cookie
     */
    String getName();

    /**
     * Assigns a new value to this Cookie.
     * 
     * <p>
     * If you use a binary value, you may want to use BASE64 encoding.
     *
     * <p>
     * With Version 0 cookies, values should not contain white
     * space, brackets, parentheses, equals signs, commas,
     * double quotes, slashes, question marks, at signs, colons,
     * and semicolons. Empty values may not behave the same way
     * on all browsers.
     *
     * @param newValue
     *     the new value of the cookie
     *
     * @see #getValue
     */
    void setValue(String newValue);

    /**
     * Gets the current value of this Cookie.
     *
     * @return the current value of this Cookie
     *
     * @see #setValue
     */
    String getValue();

    /**
     * Marks or unmarks this Cookie as <i>HttpOnly</i>.
     *
     * <p>
     * If <tt>isHttpOnly</tt> is set to <tt>true</tt>, this cookie is
     * marked as <i>HttpOnly</i>, by adding the <tt>HttpOnly</tt> attribute
     * to it.
     *
     * <p>
     * <i>HttpOnly</i> cookies are not supposed to be exposed to
     * client-side scripting code, and may therefore help mitigate certain
     * kinds of cross-site scripting attacks.
     *
     * @param isHttpOnly
     *     true if this cookie is to be marked as
     *     <i>HttpOnly</i>, false otherwise
     */
    void setHttpOnly(boolean isHttpOnly);

    /**
     * Checks whether this Cookie has been marked as <i>HttpOnly</i>.
     *
     * @return true if this Cookie has been marked as <i>HttpOnly</i>,
     * false otherwise
     */
    boolean isHttpOnly();
}
