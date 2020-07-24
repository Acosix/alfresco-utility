/*
 * Copyright 2016 - 2020 Acosix GmbH
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.alfresco.web.scripts.SlingshotRemoteClient;

/**
 * Instances of this class allow for simpler modification of complex request settings in order to dynamically adapt the behaviour depending
 * on the authentication state. Since remote client instances are handled via a prototype bean, it is safe to modify the state of these
 * objects in concurrent requests / threads, as each contex uses its own, isolated instance.
 *
 * @author Axel Faust
 */
public class MutableSlingshotRemoteClient extends SlingshotRemoteClient
{

    protected final Set<String> removeRequestHeaders = new HashSet<>();

    protected final Set<String> removeResponseHeaders = new HashSet<>();

    protected final Map<String, String> requestProperties = new HashMap<>();

    protected final Map<String, String> requestHeaders = new HashMap<>();

    protected final Map<String, String> cookies = new HashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRemoveRequestHeaders(final Set<String> removeRequestHeaders)
    {
        this.removeRequestHeaders.clear();
        if (removeRequestHeaders != null)
        {
            this.removeRequestHeaders.addAll(removeRequestHeaders);
        }
        super.setRemoveRequestHeaders(removeRequestHeaders);
    }

    /**
     * Adds a header to remove from the client request when performing request forwarding.
     *
     * @param removeRequestHeader
     *            the name of the header to remove
     */
    public void addRemoveRequestHeader(final String removeRequestHeader)
    {
        this.removeRequestHeaders.add(removeRequestHeader);
        super.setRemoveRequestHeaders(this.removeRequestHeaders);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRemoveResponseHeaders(final Set<String> removeResponseHeaders)
    {
        this.removeResponseHeaders.clear();
        if (removeResponseHeaders != null)
        {
            this.removeResponseHeaders.addAll(removeResponseHeaders);
        }
        super.setRemoveResponseHeaders(removeResponseHeaders);
    }

    /**
     * Adds a header to remove when processing responses.
     *
     * @param removeResponseHeader
     *            the name of the header to remove
     */
    public void addRemoveResponseHeader(final String removeResponseHeader)
    {
        this.removeResponseHeaders.add(removeResponseHeader);
        super.setRemoveResponseHeaders(this.removeResponseHeaders);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRequestProperties(final Map<String, String> requestProperties)
    {
        this.requestProperties.clear();
        if (requestProperties != null)
        {
            this.requestProperties.putAll(requestProperties);
        }
        super.setRequestProperties(requestProperties);
    }

    /**
     * Adds a request property to the existing map of properties, overriding any existing value for the same property name.
     *
     * @param name
     *            the name of the property to add
     * @param value
     *            the value of the property
     */
    public void addRequestProperty(final String name, final String value)
    {
        this.requestProperties.put(name, value);
        super.setRequestProperties(this.requestProperties);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRequestHeaders(final Map<String, String> requestHeaders)
    {
        this.requestHeaders.clear();
        if (requestHeaders != null)
        {
            this.requestHeaders.putAll(requestHeaders);
        }
        super.setRequestHeaders(requestHeaders);
    }

    /**
     * Adds a request header to the existing map of headers, overriding any existing value for the same header name.
     *
     * @param name
     *            the name of the header to add
     * @param value
     *            the value of the header
     */
    public void addRequestHeader(final String name, final String value)
    {
        this.requestHeaders.put(name, value);
        super.setRequestHeaders(this.requestHeaders);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCookies(final Map<String, String> cookies)
    {
        this.cookies.clear();
        if (cookies != null)
        {
            this.cookies.putAll(cookies);
        }
        super.setCookies(cookies);
    }

    /**
     * Adds a cookie to the existing map of cookies, overriding any existing value for the same cookie name.
     *
     * @param name
     *            the name of the cookie to add
     * @param value
     *            the value of the cookie
     */
    public void addCookie(final String name, final String value)
    {
        this.cookies.put(name, value);
        super.setCookies(this.cookies);
    }

}
