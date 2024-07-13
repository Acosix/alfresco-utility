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

/**
 * Instances of this class encapsulate a work item of the {@link SynchJob IMAP synchronisation job}
 *
 * @author Axel Faust
 */
public class SynchWork
{

    private final ImapEmailMessage emailMessage;

    private String fromOverride;

    private String toOverride;

    private String moveProcessedPath;

    private String moveRejectedPath;

    /**
     * Creates a new instance of this class encapsulating a message to process.
     *
     * @param emailMessage
     *     the message to encapsulate
     */
    public SynchWork(final ImapEmailMessage emailMessage)
    {
        this.emailMessage = emailMessage;
    }

    /**
     * @return the emailMessage
     */
    public ImapEmailMessage getEmailMessage()
    {
        return this.emailMessage;
    }

    /**
     * @return the fromOverride
     */
    public String getFromOverride()
    {
        return this.fromOverride;
    }

    /**
     * @param fromOverride
     *     the fromOverride to set
     */
    public void setFromOverride(final String fromOverride)
    {
        this.fromOverride = fromOverride;
    }

    /**
     * @return the toOverride
     */
    public String getToOverride()
    {
        return this.toOverride;
    }

    /**
     * @param toOverride
     *     the toOverride to set
     */
    public void setToOverride(final String toOverride)
    {
        this.toOverride = toOverride;
    }

    /**
     * @return the moveProcessedPath
     */
    public String getMoveProcessedPath()
    {
        return this.moveProcessedPath;
    }

    /**
     * @param moveProcessedPath
     *     the moveProcessedPath to set
     */
    public void setMoveProcessedPath(final String moveProcessedPath)
    {
        this.moveProcessedPath = moveProcessedPath;
    }

    /**
     * @return the moveRejectedPath
     */
    public String getMoveRejectedPath()
    {
        return this.moveRejectedPath;
    }

    /**
     * @param moveRejectedPath
     *     the moveRejectedPath to set
     */
    public void setMoveRejectedPath(final String moveRejectedPath)
    {
        this.moveRejectedPath = moveRejectedPath;
    }

}
