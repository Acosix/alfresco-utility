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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.Deflater;

import javax.net.SocketFactory;

import org.alfresco.util.ParameterCheck;

/**
 * Instances of this class encapsulate the configuration of an inbound IMAP connection.
 *
 * @author Axel Faust
 */
public class Config
{

    private boolean debug = false;

    private SocketFactory socketFactory;

    private String protocol = "imap";

    private String host;

    private int port = 143;

    private String user;

    private String password;

    private String authMechanisms;

    private String saslMechanisms;

    private String saslAuthorizationId;

    private String saslRealm;

    private boolean startTlsEnabled = true;

    private boolean startTlsRequired = false;

    private int connectionTimeout = 10000;

    private int readTimeout = 10000;

    private int writeTimeout = 10000;

    private boolean compressionEnabled = true;

    private int compressionLevel = (Deflater.BEST_COMPRESSION + Deflater.BEST_SPEED) / 2;

    private int compressionStrategy = Deflater.DEFAULT_STRATEGY;

    private String defaultFromOverride;

    private String defaultToOverride;

    private String processTriggerCronExpression;

    private boolean processFilterByFlagEnabled = false;

    private int processFilterByFlagBits;

    private int processFilterByUnsetFlagBits;

    private String processFilterByFlagName;

    private String processFilterByUnsetFlagName;

    private boolean flagProcessedEnabled = false;

    private int flagProcessedWithBits;

    private int flagProcessedWithUnsetBits;

    private String flagProcessedWithName;

    private String flagProcessedWithUnsetName;

    private boolean flagRejectedEnabled = false;

    private int flagRejectedWithBits;

    private int flagRejectedWithUnsetBits;

    private String flagRejectedWithName;

    private String flagRejectedWithUnsetName;

    private final Set<String> folders = new LinkedHashSet<>();

    private final Map<String, String> pathByFolder = new HashMap<>();

    private final Map<String, String> fromOverrideByFolder = new HashMap<>();

    private final Map<String, String> toOverrideByFolder = new HashMap<>();

    private final Map<String, String> moveProcessedToPathByFolder = new HashMap<>();

    private final Map<String, String> moveRejectedToPathByFolder = new HashMap<>();

    /**
     * Retrieves whether to enable debug mode.
     *
     * @return the debug
     */
    public boolean isDebug()
    {
        return this.debug;
    }

    /**
     * Sets whether to enable debug mode.
     *
     * @param debug
     *     the debug to set
     */
    public void setDebug(final boolean debug)
    {
        this.debug = debug;
    }

    /**
     * Retrieves the socket factory to use.
     *
     * @return the socketFactory
     */
    public SocketFactory getSocketFactory()
    {
        return this.socketFactory;
    }

    /**
     * Sets the socket factory to use.
     *
     * @param socketFactory
     *     the socketFactory to set
     */
    public void setSocketFactory(final SocketFactory socketFactory)
    {
        this.socketFactory = socketFactory;
    }

    /**
     * Retrieves the protocol to use connecting to the IMAP account.
     *
     * @return the protocol
     */
    public String getProtocol()
    {
        return this.protocol;
    }

    /**
     * Sets the protocol to use connecting to the IMAP account.
     *
     * @param protocol
     *     the protocol to set
     */
    public void setProtocol(final String protocol)
    {
        ParameterCheck.mandatoryString("protocol", protocol);
        if (!protocol.toLowerCase(Locale.ENGLISH).matches("^imaps?$"))
        {
            throw new IllegalArgumentException(protocol + " is not a supported protocol");
        }
        this.protocol = protocol;
    }

    /**
     * Retrieves the host to use connecting to the IMAP account.
     *
     * @return the host
     */
    public String getHost()
    {
        return this.host;
    }

    /**
     * Sets the host to use connecting to the IMAP account.
     *
     * @param host
     *     the host to set
     */
    public void setHost(final String host)
    {
        ParameterCheck.mandatoryString("host", host);
        this.host = host;
    }

    /**
     * Retrieves the port to use connecting to the IMAP account.
     *
     * @return the port
     */
    public int getPort()
    {
        return this.port;
    }

    /**
     * Sets the port to use connecting to the IMAP account.
     *
     * @param port
     *     the port to set
     */
    public void setPort(final int port)
    {
        this.port = port;
    }

    /**
     * Retrieves the user to use connecting to the IMAP account.
     *
     * @return the user
     */
    public String getUser()
    {
        return this.user;
    }

    /**
     * Sets the user to use connecting to the IMAP account.
     *
     * @param user
     *     the user to set
     */
    public void setUser(final String user)
    {
        ParameterCheck.mandatoryString("user", user);
        this.user = user;
    }

    /**
     * Retrieves the password to use connecting to the IMAP account.
     *
     * @return the password
     */
    public String getPassword()
    {
        return this.password;
    }

    /**
     * Sets the password to use connecting to the IMAP account.
     *
     * @param password
     *     the password to set
     */
    public void setPassword(final String password)
    {
        ParameterCheck.mandatoryString("password", password);
        this.password = password;
    }

    /**
     * Retrieves the authentication mechanisms to enable.
     *
     * @return the authMechanisms
     */
    public String getAuthMechanisms()
    {
        return this.authMechanisms;
    }

    /**
     * Sets the authentication mechanisms to enable.
     *
     * @param authMechanisms
     *     the authMechanisms to set
     */
    public void setAuthMechanisms(final String authMechanisms)
    {
        this.authMechanisms = authMechanisms;
    }

    /**
     * Retrieves the SASL authentication mechanisms to enable.
     *
     * @return the saslMechanisms
     */
    public String getSaslMechanisms()
    {
        return this.saslMechanisms;
    }

    /**
     * Sets the SASL authentication mechanisms to enable.
     *
     * @param saslMechanisms
     *     the saslMechanisms to set
     */
    public void setSaslMechanisms(final String saslMechanisms)
    {
        this.saslMechanisms = saslMechanisms;
    }

    /**
     * Retrieves the SASL authorisation ID.
     *
     * @return the saslAuthorizationId
     */
    public String getSaslAuthorizationId()
    {
        return this.saslAuthorizationId;
    }

    /**
     * Sets the SASL authorisation ID.
     *
     * @param saslAuthorizationId
     *     the saslAuthorizationId to set
     */
    public void setSaslAuthorizationId(final String saslAuthorizationId)
    {
        this.saslAuthorizationId = saslAuthorizationId;
    }

    /**
     * Retrieves the SASL realm.
     *
     * @return the saslRealm
     */
    public String getSaslRealm()
    {
        return this.saslRealm;
    }

    /**
     * Sets the SASL realm.
     *
     * @param saslRealm
     *     the saslRealm to set
     */
    public void setSaslRealm(final String saslRealm)
    {
        this.saslRealm = saslRealm;
    }

    /**
     * Retrieves whether to enable support of {@code STARTTLS}.
     *
     * @return the startTlsEnabled
     */
    public boolean isStartTlsEnabled()
    {
        return this.startTlsEnabled;
    }

    /**
     * Sets whether to enable support of {@code STARTTLS}.
     *
     * @param startTlsEnabled
     *     the startTlsEnabled to set
     */
    public void setStartTlsEnabled(final boolean startTlsEnabled)
    {
        this.startTlsEnabled = startTlsEnabled;
    }

    /**
     * Retrieves whether to require support of {@code STARTTLS}.
     *
     * @return the startTlsRequired
     */
    public boolean isStartTlsRequired()
    {
        return this.startTlsRequired;
    }

    /**
     * Sets whether to require support of {@code STARTTLS}.
     *
     * @param startTlsRequired
     *     the startTlsRequired to set
     */
    public void setStartTlsRequired(final boolean startTlsRequired)
    {
        this.startTlsRequired = startTlsRequired;
    }

    /**
     * Retrieves the connection timeout to use.
     *
     * @return the connectionTimeout
     */
    public int getConnectionTimeout()
    {
        return this.connectionTimeout;
    }

    /**
     * Sets the connection timeout to use.
     *
     * @param connectionTimeout
     *     the connectionTimeout to set
     */
    public void setConnectionTimeout(final int connectionTimeout)
    {
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * Retrieves the read timeout to use.
     *
     * @return the readTimeout
     */
    public int getReadTimeout()
    {
        return this.readTimeout;
    }

    /**
     * Sets the read timeout to use.
     *
     * @param readTimeout
     *     the readTimeout to set
     */
    public void setReadTimeout(final int readTimeout)
    {
        this.readTimeout = readTimeout;
    }

    /**
     * Retrieves the write timeout to use.
     *
     * @return the writeTimeout
     */
    public int getWriteTimeout()
    {
        return this.writeTimeout;
    }

    /**
     * Sets the write timeout to use.
     *
     * @param writeTimeout
     *     the writeTimeout to set
     */
    public void setWriteTimeout(final int writeTimeout)
    {
        this.writeTimeout = writeTimeout;
    }

    /**
     * Retrieves whether to enable transfer compression.
     *
     * @return the compressionEnabled
     */
    public boolean isCompressionEnabled()
    {
        return this.compressionEnabled;
    }

    /**
     * Sets whether to enable transfer compression.
     *
     * @param compressionEnabled
     *     the compressionEnabled to set
     */
    public void setCompressionEnabled(final boolean compressionEnabled)
    {
        this.compressionEnabled = compressionEnabled;
    }

    /**
     * Retrieves the transfer compression level to use.
     *
     * @return the compressionLevel
     */
    public int getCompressionLevel()
    {
        return this.compressionLevel;
    }

    /**
     * Sets the transfer compression level to use.
     *
     * @param compressionLevel
     *     the compressionLevel to set
     */
    public void setCompressionLevel(final int compressionLevel)
    {
        this.compressionLevel = compressionLevel;
    }

    /**
     * Retrieves the transfer compression strategy to use.
     *
     * @return the compressionStrategy
     */
    public int getCompressionStrategy()
    {
        return this.compressionStrategy;
    }

    /**
     * Sets the transfer compression strategy to use.
     *
     * @param compressionStrategy
     *     the compressionStrategy to set
     */
    public void setCompressionStrategy(final int compressionStrategy)
    {
        this.compressionStrategy = compressionStrategy;
    }

    /**
     * Retrieves the cron expression for the synchronisation trigger.
     *
     * @return the processTriggerCronExpression
     */
    public String getProcessTriggerCronExpression()
    {
        return this.processTriggerCronExpression;
    }

    /**
     * Sets the cron expression for the synchronisation trigger.
     *
     * @param processTriggerCronExpression
     *     the processTriggerCronExpression to set
     */
    public void setProcessTriggerCronExpression(final String processTriggerCronExpression)
    {
        ParameterCheck.mandatoryString("processTriggerCronExpression", processTriggerCronExpression);
        this.processTriggerCronExpression = processTriggerCronExpression;
    }

    /**
     * Retrieves the default override to use for the from address.
     *
     * @return the defaultFromOverride
     */
    public String getDefaultFromOverride()
    {
        return this.defaultFromOverride;
    }

    /**
     * Sets the default override to use for the from address.
     *
     * @param defaultFromOverride
     *     the defaultFromOverride to set
     */
    public void setDefaultFromOverride(final String defaultFromOverride)
    {
        this.defaultFromOverride = defaultFromOverride;
    }

    /**
     * Retrieves the default override to use for the to address.
     *
     * @return the defaultToOverride
     */
    public String getDefaultToOverride()
    {
        return this.defaultToOverride;
    }

    /**
     * Sets the default override to use for the to address.
     *
     * @param defaultToOverride
     *     the defaultToOverride to set
     */
    public void setDefaultToOverride(final String defaultToOverride)
    {
        this.defaultToOverride = defaultToOverride;
    }

    /**
     * Retrieves whether to filter the emails to synchronise based on flags set on them.
     *
     * @return the processFilterByFlagEnabled
     */
    public boolean isProcessFilterByFlagEnabled()
    {
        return this.processFilterByFlagEnabled;
    }

    /**
     * Sets whether to filter the emails to synchronise based on flags set on them.
     *
     * @param processFilterByFlagEnabled
     *     the processFilterByFlagEnabled to set
     */
    public void setProcessFilterByFlagEnabled(final boolean processFilterByFlagEnabled)
    {
        this.processFilterByFlagEnabled = processFilterByFlagEnabled;
    }

    /**
     * Retrieves the flag bit-mask by which to filter emails.
     *
     * @return the processFilterByFlagBits
     */
    public int getProcessFilterByFlagBits()
    {
        return this.processFilterByFlagBits;
    }

    /**
     * Sets the flag bit-mask by which to filter emails.
     *
     * @param processFilterByFlagBits
     *     the processFilterByFlagBits to set
     */
    public void setProcessFilterByFlagBits(final int processFilterByFlagBits)
    {
        this.processFilterByFlagBits = processFilterByFlagBits;
    }

    /**
     * Retrieves the unset flag bit-mask by which to filter emails. This allows to explicitly exclude messages that have specific flag bits
     * not set.
     *
     * @return the processFilterByUnsetFlagBits
     */
    public int getProcessFilterByUnsetFlagBits()
    {
        return this.processFilterByUnsetFlagBits;
    }

    /**
     * Sets the unset flag bit-mask by which to filter emails.
     *
     * @param processFilterByUnsetFlagBits
     *     the processFilterByUnsetFlagBits to set
     */
    public void setProcessFilterByUnsetFlagBits(final int processFilterByUnsetFlagBits)
    {
        this.processFilterByUnsetFlagBits = processFilterByUnsetFlagBits;
    }

    /**
     * Retrieves the user flag name by which to filter emails.
     *
     * @return the processFilterByFlagName
     */
    public String getProcessFilterByFlagName()
    {
        return this.processFilterByFlagName;
    }

    /**
     * Sets the user flag name by which to filter emails.
     *
     * @param processFilterByFlagName
     *     the processFilterByFlagName to set
     */
    public void setProcessFilterByFlagName(final String processFilterByFlagName)
    {
        this.processFilterByFlagName = processFilterByFlagName;
    }

    /**
     * Retrieves the unset user flag name by which to filter emails.
     *
     * @return the processFilterByUnsetFlagName
     */
    public String getProcessFilterByUnsetFlagName()
    {
        return this.processFilterByUnsetFlagName;
    }

    /**
     * Sets the unset user flag name by which to filter emails.
     *
     * @param processFilterByUnsetFlagName
     *     the processFilterByUnsetFlagName to set
     */
    public void setProcessFilterByUnsetFlagName(final String processFilterByUnsetFlagName)
    {
        this.processFilterByUnsetFlagName = processFilterByUnsetFlagName;
    }

    /**
     * Retrieves whether to mark the successfully processed emails with flags.
     *
     * @return the flagProcessedEnabled
     */
    public boolean isFlagProcessedEnabled()
    {
        return this.flagProcessedEnabled;
    }

    /**
     * Sets whether to mark the successfully processed emails with flags.
     *
     * @param flagProcessedEnabled
     *     the flagProcessedEnabled to set
     */
    public void setFlagProcessedEnabled(final boolean flagProcessedEnabled)
    {
        this.flagProcessedEnabled = flagProcessedEnabled;
    }

    /**
     * Retrieves the flag bit-mask to set when marking successfully processed emails.
     *
     * @return the flagProcessedWithBits
     */
    public int getFlagProcessedWithBits()
    {
        return this.flagProcessedWithBits;
    }

    /**
     * Sets the flag bit-mask to set when marking successfully processed emails.
     *
     * @param flagProcessedWithBits
     *     the flagProcessedWithBits to set
     */
    public void setFlagProcessedWithBits(final int flagProcessedWithBits)
    {
        this.flagProcessedWithBits = flagProcessedWithBits;
    }

    /**
     * Retrieves the flag bit-mask to unset when marking successfully processed emails.
     *
     * @return the flagProcessedWithUnsetBits
     */
    public int getFlagProcessedWithUnsetBits()
    {
        return this.flagProcessedWithUnsetBits;
    }

    /**
     * Sets the flag bit-mask to unset when marking successfully processed emails.
     *
     * @param flagProcessedWithUnsetBits
     *     the flagProcessedWithUnsetBits to set
     */
    public void setFlagProcessedWithUnsetBits(final int flagProcessedWithUnsetBits)
    {
        this.flagProcessedWithUnsetBits = flagProcessedWithUnsetBits;
    }

    /**
     * Retrieves the user flag name to set when marking successfully processed emails.
     *
     * @return the flagProcessedWithName
     */
    public String getFlagProcessedWithName()
    {
        return this.flagProcessedWithName;
    }

    /**
     * Sets the user flag name to set when marking successfully processed emails.
     *
     * @param flagProcessedWithName
     *     the flagProcessedWithName to set
     */
    public void setFlagProcessedWithName(final String flagProcessedWithName)
    {
        this.flagProcessedWithName = flagProcessedWithName;
    }

    /**
     * Retrieves the user flag name to unset when marking successfully processed emails.
     *
     * @return the flagProcessedWithUnsetName
     */
    public String getFlagProcessedWithUnsetName()
    {
        return this.flagProcessedWithUnsetName;
    }

    /**
     * Sets the user flag name to unset when marking successfully processed emails.
     *
     * @param flagProcessedWithUnsetName
     *     the flagProcessedWithUnsetName to set
     */
    public void setFlagProcessedWithUnsetName(final String flagProcessedWithUnsetName)
    {
        this.flagProcessedWithUnsetName = flagProcessedWithUnsetName;
    }

    /**
     * Retrieves whether to mark the rejected emails with flags.
     *
     * @return the flagRejectedEnabled
     */
    public boolean isFlagRejectedEnabled()
    {
        return this.flagRejectedEnabled;
    }

    /**
     * Sets whether to mark the rejected emails with flags.
     *
     * @param flagRejectedEnabled
     *     the flagRejectedEnabled to set
     */
    public void setFlagRejectedEnabled(final boolean flagRejectedEnabled)
    {
        this.flagRejectedEnabled = flagRejectedEnabled;
    }

    /**
     * Retrieves the flag bit-mask to set when marking rejected emails.
     *
     * @return the flagRejectedWithBits
     */
    public int getFlagRejectedWithBits()
    {
        return this.flagRejectedWithBits;
    }

    /**
     * Sets the flag bit-mask to set when marking rejected emails.
     *
     * @param flagRejectedWithBits
     *     the flagRejectedWithBits to set
     */
    public void setFlagRejectedWithBits(final int flagRejectedWithBits)
    {
        this.flagRejectedWithBits = flagRejectedWithBits;
    }

    /**
     * Retrieves the flag bit-mask to unset when marking rejected emails.
     *
     * @return the flagRejectedWithUnsetBits
     */
    public int getFlagRejectedWithUnsetBits()
    {
        return this.flagRejectedWithUnsetBits;
    }

    /**
     * Sets the flag bit-mask to unset when marking rejected emails.
     *
     * @param flagRejectedWithUnsetBits
     *     the flagRejectedWithUnsetBits to set
     */
    public void setFlagRejectedWithUnsetBits(final int flagRejectedWithUnsetBits)
    {
        this.flagRejectedWithUnsetBits = flagRejectedWithUnsetBits;
    }

    /**
     * Retrieves the user flag name to set when marking rejected emails.
     *
     * @return the flagRejectedWithName
     */
    public String getFlagRejectedWithName()
    {
        return this.flagRejectedWithName;
    }

    /**
     * Sets the user flag name to set when marking rejected emails.
     *
     * @param flagRejectedWithName
     *     the flagRejectedWithName to set
     */
    public void setFlagRejectedWithName(final String flagRejectedWithName)
    {
        this.flagRejectedWithName = flagRejectedWithName;
    }

    /**
     * Retrieves the user flag name to unset when marking rejected emails.
     *
     * @return the flagRejectedWithUnsetName
     */
    public String getFlagRejectedWithUnsetName()
    {
        return this.flagRejectedWithUnsetName;
    }

    /**
     * Sets the user flag name to unset when marking rejected emails.
     *
     * @param flagRejectedWithUnsetName
     *     the flagRejectedWithUnsetName to set
     */
    public void setFlagRejectedWithUnsetName(final String flagRejectedWithUnsetName)
    {
        this.flagRejectedWithUnsetName = flagRejectedWithUnsetName;
    }

    /**
     * Retrieves the logical (configuration) names for the folders to process.
     *
     * @return the folders
     */
    public Collection<String> getFolders()
    {
        return new LinkedHashSet<>(this.folders);
    }

    /**
     * Sets the logical (configuration) names for the folders to process.
     *
     * @param folders
     *     the names of the folders to process
     */
    public void setFolders(final Collection<String> folders)
    {
        this.folders.clear();
        if (folders != null)
        {
            this.folders.addAll(folders);
        }
    }

    /**
     * Retrieves the mapping of paths for logical (configuration) names of the folders to process.
     *
     * @return the pathByFolder
     */
    public Map<String, String> getPathByFolder()
    {
        return new HashMap<>(this.pathByFolder);
    }

    /**
     * Sets the mapping of paths for logical (configuration) names of the folders to process.
     *
     * @param pathByFolder
     *     the mapping of paths for folder names
     */
    public void setPathByFolder(final Map<String, String> pathByFolder)
    {
        this.pathByFolder.clear();
        if (pathByFolder != null)
        {
            this.pathByFolder.putAll(pathByFolder);
        }
    }

    /**
     * Retrieves the mapping of from overrides for logical (configuration) names of the folders to process.
     *
     * @return the fromOverrideByFolder
     */
    public Map<String, String> getFromOverrideByFolder()
    {
        return new HashMap<>(this.fromOverrideByFolder);
    }

    /**
     * Sets the mapping of from overrides for logical (configuration) names of the folders to process.
     *
     * @param fromOverrideByFolder
     *     the mapping of from override for folder names
     */
    public void setFromOverrideByFolder(final Map<String, String> fromOverrideByFolder)
    {
        this.fromOverrideByFolder.clear();
        if (fromOverrideByFolder != null)
        {
            this.fromOverrideByFolder.putAll(fromOverrideByFolder);
        }
    }

    /**
     * Retrieves the mapping of to overrides for logical (configuration) names of the folders to process.
     *
     * @return the toOverrideByFolder
     */
    public Map<String, String> getToOverrideByFolder()
    {
        return new HashMap<>(this.toOverrideByFolder);
    }

    /**
     * Sets the mapping of to overrides for logical (configuration) names of the folders to process.
     *
     * @param toOverrideByFolder
     *     the mapping of to override for folder names
     */
    public void setToOverrideByFolder(final Map<String, String> toOverrideByFolder)
    {
        this.toOverrideByFolder.clear();
        if (toOverrideByFolder != null)
        {
            this.toOverrideByFolder.putAll(toOverrideByFolder);
        }
    }

    /**
     * Retrieves the mapping of to folder paths to which processed mails should be moved for logical (configuration) names of the folders to
     * process.
     *
     * @return the moveProcessedToPathByFolder
     */
    public Map<String, String> getMoveProcessedToPathByFolder()
    {
        return new HashMap<>(this.moveProcessedToPathByFolder);
    }

    /**
     * Sets the mapping of to folder paths to which processed mails should be moved for logical (configuration) names of the folders to
     * process.
     *
     * @param moveProcessedToPathByFolder
     *     the mapping of move target paths for folder names
     */
    public void setMoveProcessedToPathByFolder(final Map<String, String> moveProcessedToPathByFolder)
    {
        this.moveProcessedToPathByFolder.clear();
        if (moveProcessedToPathByFolder != null)
        {
            this.moveProcessedToPathByFolder.putAll(moveProcessedToPathByFolder);
        }
    }

    /**
     * Retrieves the mapping of to folder paths to which rejected mails should be moved for logical (configuration) names of the folders to
     * process.
     *
     * @return the moveProcessedToPathByFolder
     */
    public Map<String, String> getMoveRejectedToPathByFolder()
    {
        return new HashMap<>(this.moveProcessedToPathByFolder);
    }

    /**
     * Sets the mapping of to folder paths to which rejected mails should be moved for logical (configuration) names of the folders to
     * process.
     *
     * @param moveRejectedToPathByFolder
     *     the mapping of move target paths for folder names
     */
    public void setMoveRejectedToPathByFolder(final Map<String, String> moveRejectedToPathByFolder)
    {
        this.moveRejectedToPathByFolder.clear();
        if (moveRejectedToPathByFolder != null)
        {
            this.moveRejectedToPathByFolder.putAll(moveRejectedToPathByFolder);
        }
    }

}
