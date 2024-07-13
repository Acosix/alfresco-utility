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

import de.acosix.alfresco.utility.repo.email.EmailMessage;

/**
 * Wrapper aspect to encapsulate an IMAP-specific {@code MimeMessage} instance.
 *
 * @author Axel Faust
 */
public class ImapEmailMessage extends EmailMessage
{

    private final String folderPath;

    private final String messageId;

    private final String from;

    /**
     * Creates a new instance of this class.
     *
     * @param message
     *     the message to wrap
     * @param folderPath
     *     the path of the folder in which the message is stored
     * @param messageId
     *     the ID of the message
     * @param from
     *     the sender address
     */
    public ImapEmailMessage(final Object message, final String folderPath, final String messageId, final String from)
    {
        super(message);
        this.folderPath = folderPath;
        this.messageId = messageId;
        this.from = from;
    }

    /**
     * Retrieves the folder path in which the wrapped email is located.
     *
     * @return the folderPath
     */
    public String getFolderPath()
    {
        return this.folderPath;
    }

    /**
     * Retrieves the message ID of the wrapped email.
     *
     * @return the messageId
     */
    public String getMessageId()
    {
        return this.messageId;
    }

    /**
     * Retrieves the from message of the message.
     *
     * @return the from
     */
    public String getFrom()
    {
        return this.from;
    }

}
