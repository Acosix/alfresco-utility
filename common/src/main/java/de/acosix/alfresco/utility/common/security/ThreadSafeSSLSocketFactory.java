/*
 * Copyright 2016, 2017 Acosix GmbH
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
package de.acosix.alfresco.utility.common.security;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.alfresco.error.AlfrescoRuntimeException;

/**
 * This class can be used as an alternative to {@code AlfrescoSSLSocketFactory} which is not thread safe, especially when multiple instances
 * are initialised with different trust stores.
 *
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class ThreadSafeSSLSocketFactory extends SSLSocketFactory
{

    private static SSLContext LAST_INITIALISED_CONTEXT;

    private static ThreadLocal<SSLContext> THREAD_INITIALISED_CONTEXT;

    protected final SSLContext context;

    public ThreadSafeSSLSocketFactory()
    {
        if (THREAD_INITIALISED_CONTEXT.get() != null)
        {
            this.context = THREAD_INITIALISED_CONTEXT.get();
        }
        else if (LAST_INITIALISED_CONTEXT != null)
        {
            this.context = LAST_INITIALISED_CONTEXT;
        }
        else
        {
            throw new IllegalStateException("No SSL context has been initialised");
        }
    }

    /**
     * Initialize the factory with custom trustStore
     *
     * @param trustStore
     */
    public static synchronized void initTrustedSSLSocketFactory(final KeyStore trustStore)
    {
        try
        {
            final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
            trustManagerFactory.init(trustStore);
            THREAD_INITIALISED_CONTEXT.set(SSLContext.getInstance("SSL"));
            LAST_INITIALISED_CONTEXT = THREAD_INITIALISED_CONTEXT.get();
            THREAD_INITIALISED_CONTEXT.get().init(null, trustManagerFactory.getTrustManagers(), SecureRandom.getInstance("SHA1PRNG"));
        }
        catch (final NoSuchAlgorithmException nsae)
        {
            throw new AlfrescoRuntimeException("The SSL socket factory cannot be initialized.", nsae);
        }
        catch (final KeyStoreException kse)
        {
            throw new AlfrescoRuntimeException("The SSL socket factory cannot be initialized.", kse);
        }
        catch (final KeyManagementException kme)
        {
            throw new AlfrescoRuntimeException("The SSL socket factory cannot be initialized.", kme);
        }
    }

    public static synchronized SocketFactory getDefault()
    {
        return new ThreadSafeSSLSocketFactory();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String[] getDefaultCipherSuites()
    {
        return this.context.getSocketFactory().getDefaultCipherSuites();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String[] getSupportedCipherSuites()
    {
        return this.context.getSocketFactory().getSupportedCipherSuites();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Socket createSocket(final Socket socket, final String s, final int i, final boolean b) throws IOException
    {
        return this.context.getSocketFactory().createSocket(socket, s, i, b);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Socket createSocket(final String s, final int i) throws IOException, UnknownHostException
    {
        return this.context.getSocketFactory().createSocket(s, i);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Socket createSocket(final String s, final int i, final InetAddress inetAddress, final int i2)
            throws IOException, UnknownHostException
    {
        return this.context.getSocketFactory().createSocket(s, i, inetAddress, i2);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Socket createSocket(final InetAddress inetAddress, final int i) throws IOException
    {
        return this.context.getSocketFactory().createSocket(inetAddress, i);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Socket createSocket(final InetAddress inetAddress, final int i, final InetAddress inetAddress2, final int i2) throws IOException
    {
        return this.context.getSocketFactory().createSocket(inetAddress, i, inetAddress2, i2);
    }
}
