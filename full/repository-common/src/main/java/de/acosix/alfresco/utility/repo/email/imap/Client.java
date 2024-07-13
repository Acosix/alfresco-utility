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

import java.io.Closeable;
import java.util.List;

import org.alfresco.service.cmr.email.EmailMessage;
import org.alfresco.service.cmr.email.EmailService;

import de.acosix.alfresco.utility.repo.email.server.ImprovedEmailMessage;

/**
 * Instances of this interface provide the abstraction to interact with an IMAP account.
 *
 * @author Axel Faust
 */
public interface Client extends Closeable
{

    /**
     * Count the messages stored in a specific folder.
     *
     * @param folderPath
     *     the path in which to count messages
     * @return the number of matching messages
     */
    int countMessages(String folderPath);

    /**
     * Count the messages stored in a specific folder, using a filtering based on flags.
     *
     * @param folderPath
     *     the path in which to count messages
     * @param filterFlagSetBits
     *     the bit-mask of explicitly set system flags by which to filter
     * @param filterFlagUnsetBits
     *     the bit-mask of explicitly unset system flags by which to filter
     * @param filterFlagName
     *     the name of a custom flag by which to filter
     * @param filterFlagUnsetName
     *     the name of an explicitly unset custom flag by which to filter
     * @return the number of matching messages
     */
    int countMessages(String folderPath, int filterFlagSetBits, int filterFlagUnsetBits, String filterFlagName, String filterFlagUnsetName);

    /**
     * Lists the messages stored in a specific folder.
     *
     * @param folderPath
     *     the path from which to retrieve messages
     * @return the messages
     */
    List<ImapEmailMessage> listMessages(String folderPath);

    /**
     * Lists the messages stored in a specific folder, using a filtering based on flags.
     *
     * @param folderPath
     *     the path from which to retrieve messages
     * @param filterFlagSetBits
     *     the bit-mask of explicitly set system flags by which to filter
     * @param filterFlagUnsetBits
     *     the bit-mask of explicitly unset system flags by which to filter
     * @param filterFlagName
     *     the name of a custom flag by which to filter
     * @param filterFlagUnsetName
     *     the name of an explicitly unset custom flag by which to filter
     * @return the matching messages
     */
    List<ImapEmailMessage> listMessages(String folderPath, int filterFlagSetBits, int filterFlagUnsetBits, String filterFlagName,
            String filterFlagUnsetName);

    /**
     * Flags a message with a specific set of flags. Any flags set will be additions to flags already set on the message.
     *
     * @param message
     *     the message for which to set flags
     * @param flagBits
     *     the bit-mask of system flags to set
     * @param flagUnsetBits
     *     the bit-mask of system flags to unset
     * @param flagName
     *     the name of a custom flag to set
     * @param flagUnsetName
     *     the name of a custom flag to unset
     */
    void flagMessage(ImapEmailMessage message, int flagBits, int flagUnsetBits, String flagName, String flagUnsetName);

    /**
     * Moves a message to a specific target folder.
     *
     * @param message
     *     the message to move
     * @param targetFolderPath
     *     the path of the folder to which to move the message
     */
    void moveMessage(ImapEmailMessage message, String targetFolderPath);

    /**
     * Converts the IMAP message to an instance of the ({@link ImprovedEmailMessage improved}) {@link EmailMessage Alfresco email message}
     * class processable by the {@link EmailService}.
     *
     * @param message
     *     the IMAP message
     * @return the improved email message instance
     */
    ImprovedEmailMessage toImprovedEmailMessage(ImapEmailMessage message);
}
