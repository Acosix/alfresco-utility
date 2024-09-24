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
package de.acosix.alfresco.utility.repo.email.imap;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.alfresco.error.AlfrescoRuntimeException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Instances of this class provide the common functionality for abstraction to interact with an IMAP account.
 *
 * @author Axel Faust
 */
public abstract class BaseClient implements Client
{

    /**
     *
     * @author Axel Faust
     */
    private static class ProtocolSocketFactoryWrapper implements ProtocolSocketFactory
    {

        protected final SocketFactory socketFactory;

        /**
         * Creates a new instance of this class.
         *
         * @param socketFactory
         *     the socket factory to wrap
         */
        protected ProtocolSocketFactoryWrapper(final SocketFactory socketFactory)
        {
            this.socketFactory = socketFactory;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Socket createSocket(final String host, final int port, final InetAddress localAddress, final int localPort)
                throws IOException
        {
            return this.socketFactory.createSocket(host, port, localAddress, localPort);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Socket createSocket(final String host, final int port, final InetAddress localAddress, final int localPort,
                final HttpConnectionParams params) throws IOException
        {
            if (params == null)
            {
                throw new IllegalArgumentException("Parameters may not be null");
            }

            final int timeout = params.getConnectionTimeout();
            if (timeout == 0)
            {
                return this.createSocket(host, port, localAddress, localPort);
            }

            final Socket socket = this.socketFactory.createSocket(host, port, localAddress, localPort);
            socket.setSoTimeout(timeout);
            return socket;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Socket createSocket(final String host, final int port) throws IOException
        {
            return this.socketFactory.createSocket(host, port);
        }
    }

    /**
     *
     * @author Axel Faust
     */
    private static class SecureProtocolSocketFactoryWrapper extends ProtocolSocketFactoryWrapper implements SecureProtocolSocketFactory
    {

        /**
         * Creates a new instance of this class.
         *
         * @param socketFactory
         *     the socket factory to wrap
         */
        protected SecureProtocolSocketFactoryWrapper(final SSLSocketFactory socketFactory)
        {
            super(socketFactory);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Socket createSocket(final Socket socket, final String host, final int port, final boolean autoClose)
                throws IOException, UnknownHostException
        {
            return ((SSLSocketFactory) this.socketFactory).createSocket(socket, host, port, autoClose);
        }

    }

    /**
     * Prepares the parameters for an IMAP session.
     *
     * @param imapConfig
     *     the IMAP configuration
     * @param connections
     *     the number of connections to support
     * @param socketFactory
     *     the socket factory to use for connections
     * @return the prepared parameters
     */
    protected static Properties prepareParameters(final Config imapConfig, final int connections, final SocketFactory socketFactory)
    {
        final Properties props = new Properties();

        final String protocol = imapConfig.getProtocol();

        if (protocol == null || !protocol.toLowerCase(Locale.ENGLISH).matches("^imaps?$"))
        {
            throw new AlfrescoRuntimeException("Undefined/unsupported protocol: " + protocol);
        }

        final String prefix = "mail." + protocol.toLowerCase(Locale.ENGLISH);

        props.put(prefix + ".user", imapConfig.getUser());
        props.put(prefix + ".host", imapConfig.getHost());
        props.put(prefix + ".port", imapConfig.getPort());

        if (socketFactory != null)
        {
            props.put(prefix + ".socketFactory", socketFactory);
            if (socketFactory instanceof SSLSocketFactory)
            {
                props.put(prefix + ".ssl.socketFactory", socketFactory);
            }
        }
        props.put(prefix + ".ssl.checkserveridentity", "true");

        props.put(prefix + ".connectionpoolsize", Math.max(1, connections));

        props.put(prefix + ".starttls.enable", String.valueOf(imapConfig.isStartTlsEnabled()));
        props.put(prefix + ".starttls.required", String.valueOf(imapConfig.isStartTlsRequired()));

        props.put(prefix + ".connectiontimeout", imapConfig.getConnectionTimeout());
        props.put(prefix + ".timeout", imapConfig.getReadTimeout());
        props.put(prefix + ".writetimeout", imapConfig.getWriteTimeout());

        props.put(prefix + ".compress.enable", imapConfig.isCompressionEnabled());
        props.put(prefix + ".compress.level", imapConfig.getCompressionLevel());
        props.put(prefix + ".compress.strategy", imapConfig.getCompressionStrategy());

        props.put(prefix + ".auth.mechanisms", imapConfig.getAuthMechanisms());

        final String saslMechanisms = imapConfig.getSaslMechanisms();
        if (saslMechanisms != null && !saslMechanisms.isEmpty())
        {
            props.put(prefix + ".sasl.enable", "true");
            props.put(prefix + ".sasl.authorizationid", imapConfig.getSaslAuthorizationId());
            props.put(prefix + ".sasl.mechanisms", saslMechanisms);
            props.put(prefix + ".sasl.realm", imapConfig.getSaslRealm());
        }

        return props;
    }

    /**
     * Checks whether the configuration specifies use of XOAUTH2 authorisation mechanism.
     *
     * @param imapConfig
     *     the config to check
     * @return {@code true} if the config specifies use of XOAUTH2, {@code false} otherwise
     */
    protected static boolean usesOAuth(final Config imapConfig)
    {
        final String authMechanisms = imapConfig.getAuthMechanisms();
        final String saslMechanisms = imapConfig.getSaslMechanisms();

        return (authMechanisms != null && authMechanisms.contains("XOAUTH2"))
                || (saslMechanisms != null && saslMechanisms.contains("XOAUTH2"));
    }

    /**
     * Checks whether the configuration has all required OAuth parameters set.
     *
     * @param imapConfig
     *     the config to check
     * @return {@code true} if the config has all parameters required to obtain an OAuth access token, {@code false} otherwise
     */
    protected static boolean hasRequiredOAuthParameters(final Config imapConfig)
    {
        final String tokenUrl = imapConfig.getOauthTokenUrl();
        final String clientId = imapConfig.getOauthClientId();
        final String clientSecret = imapConfig.getOauthClientSecret();

        return tokenUrl != null && !tokenUrl.isEmpty() && clientId != null && !clientId.isEmpty() && clientSecret != null
                && !clientSecret.isEmpty();
    }

    protected static String obtainOAuthAccessToken(final Config imapConfig, final SocketFactory socketFactory)
    {
        final String tokenUrl = imapConfig.getOauthTokenUrl();

        final HttpClient httpClient = buildHttpClient(socketFactory, tokenUrl);

        final PostMethod postMethod = new PostMethod(tokenUrl);
        final String scope = imapConfig.getOauthScope();
        if (scope != null && !scope.isEmpty())
        {
            postMethod.setRequestBody(new NameValuePair[] { new NameValuePair("grant_type", "client_credentials"),
                    new NameValuePair("client_id", imapConfig.getOauthClientId()),
                    new NameValuePair("client_secret", imapConfig.getOauthClientSecret()), new NameValuePair("scope", scope) });
        }
        else
        {
            postMethod.setRequestBody(new NameValuePair[] { new NameValuePair("grant_type", "client_credentials"),
                    new NameValuePair("client_id", imapConfig.getOauthClientId()),
                    new NameValuePair("client_secret", imapConfig.getOauthClientSecret()) });
        }

        try
        {
            final int status = httpClient.executeMethod(postMethod);
            if (status != 200)
            {
                throw new AlfrescoRuntimeException("Failed to obtain OAuth access token");
            }

            try (final InputStream response = postMethod.getResponseBodyAsStream())
            {
                final JSONObject responseObj = new JSONObject(new JSONTokener(new InputStreamReader(response, StandardCharsets.UTF_8)));
                return responseObj.getString("access_token");
            }
            catch (final JSONException e)
            {
                throw new AlfrescoRuntimeException("Error readong OAuth access token from JSON", e);
            }
        }
        catch (final IOException e)
        {
            throw new AlfrescoRuntimeException("IO error obtaining OAuth access token", e);
        }
        finally
        {
            postMethod.releaseConnection();
        }
    }

    private static HttpClient buildHttpClient(final SocketFactory socketFactory, final String url)
    {
        final HttpClientParams params = new HttpClientParams();
        params.setUriCharset(StandardCharsets.UTF_8.name());
        params.setContentCharset(StandardCharsets.UTF_8.name());
        params.setSoTimeout(2500);
        params.setParameter(HttpMethodParams.USER_AGENT, "ACOSIX-ALFRESCO-UTILITY/1.0");
        params.setIntParameter(HttpClientParams.MAX_REDIRECTS, 0);

        final HttpClient httpClient = new HttpClient(params);

        if (url.startsWith("https://"))
        {
            String host;
            int port = 443;
            final int protocolEndIdx = url.indexOf("://") + 3;
            final int portSepIdx = url.indexOf(':', protocolEndIdx);
            final int pathSepIdx = url.indexOf('/', protocolEndIdx);
            if (portSepIdx == -1 || portSepIdx > pathSepIdx)
            {
                host = url.substring(protocolEndIdx, pathSepIdx);
            }
            else
            {
                host = url.substring(protocolEndIdx, portSepIdx);
            }
            if (portSepIdx != -1 && portSepIdx < pathSepIdx)
            {
                port = Integer.parseInt(url.substring(portSepIdx + 1, pathSepIdx));
            }

            ProtocolSocketFactory psf;
            if (socketFactory instanceof SSLSocketFactory)
            {
                psf = new SecureProtocolSocketFactoryWrapper((SSLSocketFactory) socketFactory);
            }
            else
            {
                psf = new ProtocolSocketFactoryWrapper(socketFactory);
            }
            httpClient.getHostConfiguration().setHost(host, port, new Protocol("https", psf, port));
        }
        return httpClient;
    }
}
