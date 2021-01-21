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
package de.acosix.alfresco.utility.repo.policy;

import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

/**
 *
 * @author Axel Faust
 */
public class NOOPBehaviourFilter implements BehaviourFilter
{

    /**
     * {@inheritDoc}
     */
    @Override
    public void enableBehaviours(final NodeRef nodeRef)
    {
        // NO-OP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disableAllBehaviours()
    {
        // NO-OP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enableAllBehaviours()
    {
        // NO-OP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disableBehaviour()
    {
        // NO-OP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disableBehaviour(final QName className)
    {
        // NO-OP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disableBehaviour(final NodeRef nodeRef, final QName className)
    {
        // NO-OP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disableBehaviour(final NodeRef nodeRef)
    {
        // NO-OP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enableBehaviour()
    {
        // NO-OP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enableBehaviour(final QName className)
    {
        // NO-OP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enableBehaviour(final NodeRef nodeRef, final QName className)
    {
        // NO-OP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enableBehaviour(final NodeRef nodeRef)
    {
        // NO-OP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled()
    {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled(final QName className)
    {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled(final NodeRef nodeRef, final QName className)
    {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled(final NodeRef nodeRef)
    {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isActivated()
    {
        return true;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void disableBehaviour(final QName className, final boolean includeSubClasses)
    {
        // NO-OP
    }

}