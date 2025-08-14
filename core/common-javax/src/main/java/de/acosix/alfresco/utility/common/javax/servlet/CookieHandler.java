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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import de.acosix.alfresco.utility.common.servlet.Cookie;

/**
 * Instances of this class handle invocations of a {@link Cookie}.
 *
 * @author Axel Faust
 */
public class CookieHandler implements InvocationHandler
{

    private static final Map<Method, Method> METHOD_RESOLUTION = new HashMap<>();

    private final javax.servlet.http.Cookie backingInstance;

    /**
     * Creates a new instance of this class.
     *
     * @param backingInstance
     *     the backing instance
     */
    public CookieHandler(final javax.servlet.http.Cookie backingInstance)
    {
        this.backingInstance = backingInstance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable
    {
        Object result;
        final Class<?> declaringClass = method.getDeclaringClass();
        if (declaringClass.equals(Cookie.class))
        {
            Method resolvedMethod = METHOD_RESOLUTION.get(method);
            if (resolvedMethod == null)
            {
                synchronized (METHOD_RESOLUTION)
                {
                    resolvedMethod = METHOD_RESOLUTION.computeIfAbsent(method, m -> {
                        Method r = null;
                        try
                        {
                            r = javax.servlet.http.Cookie.class.getDeclaredMethod(m.getName(), m.getParameterTypes());
                        }
                        catch (final NoSuchMethodException e)
                        {
                            throw new IllegalStateException("Unsupported cookie method " + m);
                        }

                        return r;
                    });
                }
            }

            result = resolvedMethod.invoke(this.backingInstance, args);
        }
        else if (declaringClass.equals(Object.class))
        {
            result = method.invoke(this.backingInstance, args);
        }
        else
        {
            throw new IllegalStateException(declaringClass + " not supported for proxied object");
        }
        return result;
    }

    /**
     * Retrieves the backing instance.
     *
     * @return the backing instance
     */
    protected javax.servlet.http.Cookie getBackingInstance()
    {
        return this.backingInstance;
    }
}
