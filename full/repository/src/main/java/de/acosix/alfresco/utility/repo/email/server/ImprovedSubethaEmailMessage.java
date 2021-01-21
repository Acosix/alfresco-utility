/*
 * Copyright 2016 - 2021 Acosix GmbH
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

import java.io.InputStream;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.alfresco.email.server.impl.subetha.SubethaEmailMessage;
import org.alfresco.service.cmr.email.EmailMessageException;

/**
 * @author Axel Faust
 */
public class ImprovedSubethaEmailMessage extends SubethaEmailMessage implements ImprovedEmailMessage
{

    private static final long serialVersionUID = 2811314941511064704L;

    private static final String ERR_FAILED_TO_CREATE_MIME_MESSAGE = "email.server.err.failed_to_create_mime_message";

    protected transient MimeMessage mimeMessage;

    protected ImprovedSubethaEmailMessage()
    {
        super();
    }

    public ImprovedSubethaEmailMessage(final MimeMessage mimeMessage)
    {
        super(mimeMessage);
        this.mimeMessage = mimeMessage;
    }

    public ImprovedSubethaEmailMessage(final InputStream dataInputStream)
    {
        this(toMimeMessage(dataInputStream));
    }

    protected static MimeMessage toMimeMessage(final InputStream dataInputStream)
    {
        try
        {
            return new MimeMessage(Session.getDefaultInstance(System.getProperties()), dataInputStream);
        }
        catch (final MessagingException e)
        {
            throw new EmailMessageException(ERR_FAILED_TO_CREATE_MIME_MESSAGE, e.getMessage());
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public MimeMessage getMimeMessage()
    {
        return this.mimeMessage;
    }
}
