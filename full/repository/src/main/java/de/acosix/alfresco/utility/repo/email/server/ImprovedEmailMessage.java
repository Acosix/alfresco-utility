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

import javax.mail.internet.MimeMessage;

import org.alfresco.service.cmr.email.EmailMessage;

/**
 * @author Axel Faust
 */
public interface ImprovedEmailMessage extends EmailMessage
{

    /**
     * Retrieves the entire, original RFC 822 message object
     *
     * @return the full RFC 822 message object
     */
    MimeMessage getMimeMessage();

}
