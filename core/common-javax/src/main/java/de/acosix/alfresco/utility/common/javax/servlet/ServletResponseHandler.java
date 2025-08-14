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
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import de.acosix.alfresco.utility.common.servlet.Cookie;
import de.acosix.alfresco.utility.common.servlet.ServletResponse;

/**
 * Instances of this class handle invocations of a {@link ServletResponse}.
 *
 * @author Axel Faust
 */
public class ServletResponseHandler implements InvocationHandler
{

    private static final Map<Method, Method> METHOD_RESOLUTION = new HashMap<>();

    private final javax.servlet.ServletResponse backingInstance;

    /**
     * Creates a new instance of this class.
     *
     * @param backingInstance
     *     the backing instance
     */
    public ServletResponseHandler(final javax.servlet.ServletResponse backingInstance)
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
        if (declaringClass.equals(ServletResponse.class))
        {
            // handle special non-passthru operation(s) first
            if ("newCookie".equals(method.getName()))
            {
                result = new javax.servlet.http.Cookie((String) args[0], (String) args[1]);
            }
            else
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
                                r = javax.servlet.ServletResponse.class.getDeclaredMethod(m.getName(), m.getParameterTypes());
                            }
                            catch (final NoSuchMethodException e1)
                            {
                                if (this.backingInstance instanceof javax.servlet.http.HttpServletResponse)
                                {
                                    try
                                    {
                                        r = javax.servlet.http.HttpServletResponse.class.getDeclaredMethod(m.getName(),
                                                m.getParameterTypes());
                                    }
                                    catch (final NoSuchMethodException e2)
                                    {
                                        throw new IllegalStateException("Unsupported servlet response method " + m);
                                    }
                                }
                                else
                                {
                                    throw new IllegalStateException("Unsupported servlet response method " + m);
                                }
                            }

                            return r;
                        });
                    }
                }

                // handle special argument transformation(s)
                Object[] realArgs = args;
                if ("addCookie".equals(method.getName()))
                {
                    realArgs = new Object[1];
                    realArgs[0] = ((CookieHandler) Proxy.getInvocationHandler(args[0])).getBackingInstance();
                }

                result = resolvedMethod.invoke(this.backingInstance, realArgs);
            }

            if (result instanceof javax.servlet.http.Cookie)
            {
                result = Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class<?>[] { Cookie.class },
                        new CookieHandler((javax.servlet.http.Cookie) result));
            }
            else if (result instanceof javax.servlet.http.Cookie[])
            {
                final javax.servlet.http.Cookie[] arr = (javax.servlet.http.Cookie[]) result;
                result = new Cookie[arr.length];
                for (int idx = 0; idx < arr.length; idx++)
                {
                    ((Cookie[]) result)[idx] = (Cookie) Proxy.newProxyInstance(this.getClass().getClassLoader(),
                            new Class<?>[] { Cookie.class }, new CookieHandler(arr[idx]));
                }
            }
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
    protected javax.servlet.ServletResponse getBackingInstance()
    {
        return this.backingInstance;
    }
}
