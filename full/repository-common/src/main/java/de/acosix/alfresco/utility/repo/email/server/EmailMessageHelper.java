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
package de.acosix.alfresco.utility.repo.email.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;

import de.acosix.alfresco.utility.repo.email.EmailMessage;

/**
 *
 * @author Axel Faust
 */
public interface EmailMessageHelper
{

    interface EmailMessagePart
    {

        /**
         * Retrieves the file name for the message part.
         *
         * @param decoded
         *     {@code true} if the file name should be fully decoded, {@code false} if it should be returned as-is
         * @return the file name or {@code null} if the part does not have an associated file name
         */
        String getFileName(boolean decoded);

        /**
         * Retrieves the content type of the message part.
         *
         * @return the content type
         */
        String getContentType();

        /**
         * Retrieves the disposition of this message part.
         *
         * @return the disposition
         */
        String getDisposition();

        /**
         * Checks whether the {@link #getDisposition() disposition} of this message part denotes an attachment.
         *
         * @return {@code true} if this message part is an attachment, {@code false} otherwise
         */
        boolean isAttachmentDisposition();

        /**
         * Checks whether the message part is a multipart and may contain one or more body parts.
         *
         * @return {@code true} if the message part may contain one or more body parts, {@code false} otherwise
         */
        boolean isMultipart();

        /**
         * Processes the body parts contained in this message part.
         *
         * @param bodyPartConsumer
         *     the consumer to handle each body part
         */
        void processBodyParts(Consumer<EmailMessagePart> bodyPartConsumer);

        /**
         * Retrieves an input stream to the raw content of the message part.
         *
         * @return the input stream
         */
        InputStream getInputStream();
    }

    /**
     * Writes the raw content of the email to an output stream.
     *
     * @param message
     *     the email message
     * @param os
     *     the output stream
     * @throws IOException
     *     if an error occurs during the write operation
     */
    void writeTo(EmailMessage message, OutputStream os) throws IOException;

    /**
     * Retrieves the root message part of the email message.
     *
     * @param message
     *     the email message
     * @return the root message part
     */
    EmailMessagePart getMessagePart(EmailMessage message);

}
