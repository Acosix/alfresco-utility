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
 * Wrapper aspect to encapsulate an {@code InternetAddress} instance.
 * 
 * @author Axel Faust
 */
public class EmailAddress
{

    private final Object address;

    /**
     * Creates a new instance of this class.
     * 
     * @param address
     *     the address to wrap
     */
    public EmailAddress(Object address)
    {
        this.address = address;
    }

    /**
     * Retrieves the address in the required type if compatible.
     * 
     * @param <T>
     *     the type of the wrapped address
     * @param baseClass
     *     the class denoting the type
     * @return the correctly typed address wrapped in this instance
     * throws {@link ClassCastException} if the wrapped address is incompatible
     */
    public <T> T getWrappedAddress(Class<T> baseClass)
    {
        return baseClass.cast(this.address);
    }
}