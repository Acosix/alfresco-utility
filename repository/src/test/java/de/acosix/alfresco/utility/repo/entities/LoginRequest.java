/*
 * Copyright 2016, 2017 Acosix GmbH
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
package de.acosix.alfresco.utility.repo.entities;

/**
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class LoginRequest
{

    private String username;

    private String password;

    /**
     * @return the username
     */
    public String getUsername()
    {
        return this.username;
    }

    /**
     * @param username
     *            the username to set
     */
    public void setUsername(final String username)
    {
        this.username = username;
    }

    /**
     * @return the password
     */
    public String getPassword()
    {
        return this.password;
    }

    /**
     * @param password
     *            the password to set
     */
    public void setPassword(final String password)
    {
        this.password = password;
    }

}
