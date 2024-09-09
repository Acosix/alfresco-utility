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
package de.acosix.alfresco.utility.repo.subetha3.email.imap;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.search.AndTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.NotTerm;
import javax.mail.search.SearchTerm;
import javax.net.SocketFactory;

import org.alfresco.service.cmr.email.EmailMessageException;
import org.alfresco.util.ParameterCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.acosix.alfresco.utility.repo.email.imap.BaseClient;
import de.acosix.alfresco.utility.repo.email.imap.Client;
import de.acosix.alfresco.utility.repo.email.imap.Config;
import de.acosix.alfresco.utility.repo.email.imap.ImapEmailMessage;
import de.acosix.alfresco.utility.repo.email.imap.MessageFilter;
import de.acosix.alfresco.utility.repo.email.server.ImprovedEmailMessage;
import de.acosix.alfresco.utility.repo.subetha3.email.server.ImprovedSubethaEmailMessage;

/**
 * Instances of this class provide the Java Mail-based implementation to interact with an IMAP account.
 *
 * @author Axel Faust
 */
public class JavaMailClient extends BaseClient
{

    // bits not exposed anywhere in javax.mail.Flags
    private static final int ANSWERED_BIT = 0x01;

    private static final int FLAGGED_BIT = 0x08;

    private static final int RECENT_BIT = 0x10;

    private static final int SEEN_BIT = 0x20;

    private static final int SUPPORTED_DIRECT_BITS = ANSWERED_BIT | FLAGGED_BIT | RECENT_BIT | SEEN_BIT;

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaMailClient.class);

    private final Store store;

    private final Map<String, Folder> openFoldersByPath = new ConcurrentHashMap<>();

    /**
     * Opens an IMAP client for the specified IMAP configuration.
     *
     * @param imapConfig
     *     the configuration to use
     * @param connections
     *     the number of connections to support
     * @param socketFactory
     *     the socket factory to use for SSL connections
     * @return the IMAP client instance
     */
    public static Client open(final Config imapConfig, final int connections, final SocketFactory socketFactory)
    {
        final Properties props = prepareParameters(imapConfig, connections, socketFactory);
        final String protocol = imapConfig.getProtocol().toLowerCase(Locale.ENGLISH);

        final Session instance = Session.getInstance(props);
        instance.setDebug(imapConfig.isDebug());
        try
        {
            final Store store = instance.getStore(protocol);

            if (usesOAuth(imapConfig) && hasRequiredOAuthParameters(imapConfig))
            {
                final String accessToken = obtainOAuthAccessToken(imapConfig, socketFactory);
                store.connect(imapConfig.getUser(), accessToken);
            }
            else
            {
                store.connect(imapConfig.getUser(), imapConfig.getPassword());
            }
            return new JavaMailClient(store);
        }
        catch (final MessagingException e)
        {
            LOGGER.error("Failed to open Java Mail IMAP client", e);
            throw new EmailMessageException("Failed to open Java Mail IMAP client");
        }
    }

    /**
     * Creates a new instance of this class.
     *
     * @param store
     *     the store representing the IMAP account
     */
    private JavaMailClient(final Store store)
    {
        this.store = store;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException
    {
        MessagingException lastThrown = null;
        for (final Folder folder : this.openFoldersByPath.values())
        {
            try
            {
                folder.close();
            }
            catch (final MessagingException e)
            {
                LOGGER.debug("Failure closing folder", e);
                lastThrown = e;
            }
        }
        try
        {
            this.store.close();
        }
        catch (final MessagingException e)
        {
            LOGGER.debug("Failure closing store", e);
            lastThrown = e;
        }

        if (lastThrown != null)
        {
            throw new IOException("One or more errors occurred during closure of IMAP client", lastThrown);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int countMessages(final String folderPath)
    {
        ParameterCheck.mandatoryString("folderPath", folderPath);

        final Folder folder = this.openFoldersByPath.computeIfAbsent(folderPath, this::openFolder);
        try
        {
            return folder.getMessageCount();
        }
        catch (final MessagingException e)
        {
            LOGGER.error("Failed to count messages", e);
            throw new EmailMessageException("Failed to count messages");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int countMessages(final String folderPath, final MessageFilter messageFilter)
    {
        ParameterCheck.mandatoryString("folderPath", folderPath);
        ParameterCheck.mandatory("messageFilter", messageFilter);

        final Folder folder = this.openFoldersByPath.computeIfAbsent(folderPath, this::openFolder);

        final SearchTerm searchTerm = this.buildSearchTerm(messageFilter);

        try
        {
            final Message[] messages = folder.search(searchTerm);
            return messages.length;
        }
        catch (final MessagingException e)
        {
            LOGGER.error("Failed to count messages", e);
            throw new EmailMessageException("Failed to count messages");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ImapEmailMessage> listMessages(final String folderPath)
    {
        ParameterCheck.mandatoryString("folderPath", folderPath);

        final Folder folder = this.openFoldersByPath.computeIfAbsent(folderPath, this::openFolder);

        try
        {
            final Message[] messages = folder.getMessages();
            return this.convertMessages(folderPath, messages);
        }
        catch (final MessagingException e)
        {
            LOGGER.error("Failed to retrieve messages", e);
            throw new EmailMessageException("Failed to retrieve messages");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ImapEmailMessage> listMessages(final String folderPath, final MessageFilter messageFilter)
    {
        ParameterCheck.mandatoryString("folderPath", folderPath);
        ParameterCheck.mandatory("messageFilter", messageFilter);

        final Folder folder = this.openFoldersByPath.computeIfAbsent(folderPath, this::openFolder);

        final SearchTerm searchTerm = this.buildSearchTerm(messageFilter);

        try
        {
            final Message[] messages = folder.search(searchTerm);
            return this.convertMessages(folderPath, messages);
        }
        catch (final MessagingException e)
        {
            LOGGER.error("Failed to retrieve messages", e);
            throw new EmailMessageException("Failed to retrieve messages");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flagMessage(final ImapEmailMessage message, final int flagBits, final int flagUnsetBits, final Set<String> flagNames,
            final Set<String> flagUnsetNames)
    {
        ParameterCheck.mandatory("message", message);

        final IMAPMessage baseMessage = message.getWrappedMessage(IMAPMessage.class);

        if ((flagBits & SUPPORTED_DIRECT_BITS) != flagBits)
        {
            throw new EmailMessageException("Some system flags to set are not valid/supported");
        }
        if ((flagUnsetBits & SUPPORTED_DIRECT_BITS) != flagUnsetBits)
        {
            throw new EmailMessageException("Some system flags to unset are not valid/supported");
        }

        if (flagBits != 0 || (flagNames != null && !flagNames.isEmpty()))
        {
            final Flags flags = this.buildFlags(flagBits, flagNames);
            try
            {
                baseMessage.setFlags(flags, true);
            }
            catch (final MessagingException e)
            {
                LOGGER.error("Failed to flag message", e);
                throw new EmailMessageException("Failed to flag message");
            }
        }

        if (flagUnsetBits != 0 || (flagUnsetNames != null && !flagUnsetNames.isEmpty()))
        {
            final Flags flags = this.buildFlags(flagUnsetBits, flagUnsetNames);
            try
            {
                baseMessage.setFlags(flags, false);
            }
            catch (final MessagingException e)
            {
                LOGGER.error("Failed to flag message", e);
                throw new EmailMessageException("Failed to flag message");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void moveMessage(final ImapEmailMessage message, final String targetFolderPath)
    {
        ParameterCheck.mandatory("message", message);
        ParameterCheck.mandatoryString("targetFolderPath", targetFolderPath);

        final IMAPMessage baseMessage = message.getWrappedMessage(IMAPMessage.class);

        final Folder sourceFolder = this.openFoldersByPath.computeIfAbsent(message.getFolderPath(), this::openFolder);
        final Folder targetFolder = this.openFoldersByPath.computeIfAbsent(targetFolderPath, this::openFolder);

        try
        {
            ((IMAPFolder) sourceFolder).moveMessages(new Message[] { baseMessage }, targetFolder);
        }
        catch (final MessagingException e)
        {
            LOGGER.error("Failed to move message", e);
            throw new EmailMessageException("Failed to move message");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImprovedEmailMessage toImprovedEmailMessage(final ImapEmailMessage message)
    {
        ParameterCheck.mandatory("message", message);

        final IMAPMessage baseMessage = message.getWrappedMessage(IMAPMessage.class);
        return new ImprovedSubethaEmailMessage(baseMessage);
    }

    private Folder openFolder(final String folderPath)
    {
        final String[] pathElements = folderPath.split("/");

        Folder folder;
        try
        {
            folder = this.store.getFolder(pathElements[0]);
            for (int idx = 1; idx < pathElements.length; idx++)
            {
                folder = folder.getFolder(pathElements[idx]);
            }

            folder.open(Folder.READ_WRITE);
        }
        catch (final MessagingException e)
        {
            LOGGER.warn("Failed to resolve folder", e);
            throw new EmailMessageException("Failed to resolve folder " + folderPath);
        }

        return folder;
    }

    private SearchTerm buildSearchTerm(final MessageFilter messageFilter)
    {
        final Flags flags = this.buildFlags(messageFilter.getFlagSetBits(), messageFilter.getFlagSetNames());
        final Flags unsetFlags = this.buildFlags(messageFilter.getFlagUnsetBits(), messageFilter.getFlagUnsetNames());
        SearchTerm searchTerm = new AndTerm(new FlagTerm(flags, true), new FlagTerm(unsetFlags, false));

        final Set<String> allowedFromAddressPatterns = messageFilter.getAllowedFromAddressPatterns();
        final Set<String> blockedFromAddressPatterns = messageFilter.getBlockedFromAddressPatterns();
        if (allowedFromAddressPatterns != null && !allowedFromAddressPatterns.isEmpty())
        {
            searchTerm = new AndTerm(searchTerm, new FromAddressesTerm(allowedFromAddressPatterns));
        }
        if (blockedFromAddressPatterns != null && !blockedFromAddressPatterns.isEmpty())
        {
            searchTerm = new AndTerm(searchTerm, new NotTerm(new FromAddressesTerm(blockedFromAddressPatterns)));
        }

        return searchTerm;
    }

    private Flags buildFlags(final int flagBits, final Set<String> flagNames)
    {
        final Flags flags = new Flags();

        if ((flagBits & ANSWERED_BIT) == ANSWERED_BIT)
        {
            flags.add(Flag.ANSWERED);
        }
        if ((flagBits & FLAGGED_BIT) == FLAGGED_BIT)
        {
            flags.add(Flag.FLAGGED);
        }
        if ((flagBits & RECENT_BIT) == RECENT_BIT)
        {
            flags.add(Flag.RECENT);
        }
        if ((flagBits & SEEN_BIT) == SEEN_BIT)
        {
            flags.add(Flag.SEEN);
        }
        if (flagNames != null && !flagNames.isEmpty())
        {
            flags.add(Flag.USER);
            for (final String flagName : flagNames)
            {
                flags.add(flagName);
            }
        }
        return flags;
    }

    private List<ImapEmailMessage> convertMessages(final String folderPath, final Message[] baseMessages) throws MessagingException
    {
        final List<ImapEmailMessage> messages = new ArrayList<>(baseMessages.length);
        for (final Message message : baseMessages)
        {
            final String messageId = ((IMAPMessage) message).getMessageID();
            String from = null;
            final Address[] fromAddresses = message.getFrom();
            if (fromAddresses != null && fromAddresses.length > 0 && fromAddresses[0] instanceof InternetAddress)
            {
                from = ((InternetAddress) fromAddresses[0]).getAddress();
            }
            final ImapEmailMessage imapMessage = new ImapEmailMessage(message, folderPath, messageId, from);
            messages.add(imapMessage);
        }

        return messages;
    }
}
