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
package de.acosix.alfresco.utility.repo.action;

/**
 * Wrapper aspect to encapsulate a {@code MimeMessage} instance.
 * 
 * @author Axel Faust
 */
public class EmailMessage
{

    private final Object message;

    /**
     * Creates a new instance of this class.
     * 
     * @param message
     *     the message to wrap
     */
    public EmailMessage(Object message)
    {
        this.message = message;
    }

    /**
     * Retrieves the email message in the required type if compatible.
     * 
     * @param <T>
     *     the type of the wrapped message
     * @param baseClass
     *     the class denoting the type
     * @return the correctly typed message wrapped in this instance
     * throws {@link ClassCastException} if the wrapped message is incompatible
     */
    public <T> T getWrappedMessage(Class<T> baseClass)
    {
        return baseClass.cast(this.message);
    }
}