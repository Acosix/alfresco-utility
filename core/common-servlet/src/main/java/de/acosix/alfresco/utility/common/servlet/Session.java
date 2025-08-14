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

/**
 * An abstraction of the two Servlet API variants' Session API. This abstraction does not provide all of the operations present in
 * the underlying implementations - only those commonly used and required to bridge functionality for different versions of ACS.
 *
 * @author Axel Faust
 */
public interface Session
{

    /**
     *
     * Returns the time when this session was created, measured
     * in milliseconds since midnight January 1, 1970 GMT.
     *
     * @return a <code>long</code> specifying
     * when this session was created,
     * expressed in
     * milliseconds since 1/1/1970 GMT
     *
     * @exception IllegalStateException
     *     if this method is called on an
     *     invalidated session
     */
    long getCreationTime();

    /**
     * Returns a string containing the unique identifier assigned
     * to this session. The identifier is assigned
     * by the servlet container and is implementation dependent.
     *
     * @return a string specifying the identifier
     * assigned to this session
     */
    String getId();

    /**
     *
     * Returns the last time the client sent a request associated with
     * this session, as the number of milliseconds since midnight
     * January 1, 1970 GMT, and marked by the time the container received the
     * request.
     *
     * <p>
     * Actions that your application takes, such as getting or setting
     * a value associated with the session, do not affect the access
     * time.
     *
     * @return a <code>long</code>
     * representing the last time
     * the client sent a request associated
     * with this session, expressed in
     * milliseconds since 1/1/1970 GMT
     *
     * @exception IllegalStateException
     *     if this method is called on an
     *     invalidated session
     */
    long getLastAccessedTime();

    /**
     * Returns the object bound with the specified name in this session, or
     * <code>null</code> if no object is bound under the name.
     *
     * @param name
     *     a string specifying the name of the object
     *
     * @return the object with the specified name
     *
     * @exception IllegalStateException
     *     if this method is called on an
     *     invalidated session
     */
    Object getAttribute(String name);

    /**
     * Returns an <code>Enumeration</code> of <code>String</code> objects
     * containing the names of all the objects bound to this session.
     *
     * @return an <code>Enumeration</code> of
     * <code>String</code> objects specifying the
     * names of all the objects bound to
     * this session
     *
     * @exception IllegalStateException
     *     if this method is called on an
     *     invalidated session
     */
    Enumeration<String> getAttributeNames();

    /**
     * Binds an object to this session, using the name specified.
     * If an object of the same name is already bound to the session,
     * the object is replaced.
     *
     * <p>
     * After this method executes, and if the new object
     * implements <code>HttpSessionBindingListener</code>,
     * the container calls
     * <code>HttpSessionBindingListener.valueBound</code>. The container then
     * notifies any <code>HttpSessionAttributeListener</code>s in the web
     * application.
     * </p>
     *
     * <p>
     * If an object was already bound to this session of this name
     * that implements <code>HttpSessionBindingListener</code>, its
     * <code>HttpSessionBindingListener.valueUnbound</code> method is called.
     * </p>
     *
     * <p>
     * If the value passed in is null, this has the same effect as calling
     * <code>removeAttribute()</code>.
     * </p>
     *
     *
     * @param name
     *     the name to which the object is bound;
     *     cannot be null
     *
     * @param value
     *     the object to be bound
     *
     * @exception IllegalStateException
     *     if this method is called on an
     *     invalidated session
     */
    void setAttribute(String name, Object value);

    /**
     * Removes the object bound with the specified name from
     * this session. If the session does not have an object
     * bound with the specified name, this method does nothing.
     *
     * <p>
     * After this method executes, and if the object
     * implements <code>HttpSessionBindingListener</code>,
     * the container calls
     * <code>HttpSessionBindingListener.valueUnbound</code>. The container
     * then notifies any <code>HttpSessionAttributeListener</code>s in the web
     * application.
     *
     * @param name
     *     the name of the object to
     *     remove from this session
     *
     * @exception IllegalStateException
     *     if this method is called on an
     *     invalidated session
     */
    void removeAttribute(String name);

    /**
     * Invalidates this session then unbinds any objects bound
     * to it.
     *
     * @exception IllegalStateException
     *     if this method is called on an
     *     already invalidated session
     */
    void invalidate();

    /**
     * Returns <code>true</code> if the client does not yet know about the
     * session or if the client chooses not to join the session. For
     * example, if the server used only cookie-based sessions, and
     * the client had disabled the use of cookies, then a session would
     * be new on each request.
     *
     * @return <code>true</code> if the
     * server has created a session,
     * but the client has not yet joined
     *
     * @exception IllegalStateException
     *     if this method is called on an
     *     already invalidated session
     */
    boolean isNew();
}
