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
package de.acosix.alfresco.utility.repo.component;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.util.Pair;
import org.alfresco.util.PropertyCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Axel Faust
 */
public class SimplePermissionSettingPatchRule implements NodePatchRule, InitializingBean
{

    private static final Logger LOGGER = LoggerFactory.getLogger(SimplePermissionSettingPatchRule.class);

    protected PermissionService permissionService;

    protected List<Pair<String, String>> exactAuthorityPermissions;

    protected boolean allowed = true;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "permissionService", this.permissionService);
        PropertyCheck.mandatory(this, "exactAuthorityPermissions", this.exactAuthorityPermissions);

        this.exactAuthorityPermissions.forEach(exact -> {
            PropertyCheck.mandatory(exact, "authority", exact.getFirst());
            PropertyCheck.mandatory(exact, "permission", exact.getSecond());
        });
    }

    /**
     * @param permissionService
     *            the permissionService to set
     */
    public void setPermissionService(final PermissionService permissionService)
    {
        this.permissionService = permissionService;
    }

    /**
     * @param exactAuthorityPermissions
     *            the exactAuthorityPermissions to set
     */
    public void setExactAuthorityPermissions(final List<Pair<String, String>> exactAuthorityPermissions)
    {
        this.exactAuthorityPermissions = exactAuthorityPermissions;
    }

    /**
     * @param allowed
     *            the allowed to set
     */
    public void setAllowed(final boolean allowed)
    {
        this.allowed = allowed;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void apply(final NodeRef node)
    {
        this.exactAuthorityPermissions.stream().filter(exact -> !this.isSetPermission(node, exact.getFirst(), exact.getSecond()))
                .forEach(exact -> {
                    LOGGER.debug("Setting authority/permission pair {} to allowed={} on {}", exact, this.allowed, node);
                    this.permissionService.setPermission(node, exact.getFirst(), exact.getSecond(), this.allowed);
                });
    }

    protected boolean isSetPermission(final NodeRef nodeRef, final String authority, final String permission)
    {
        final boolean hasPermission = this.permissionService.getAllSetPermissions(nodeRef).stream().filter(AccessPermission::isSetDirectly)
                .filter(ap -> this.allowed ? AccessStatus.ALLOWED == ap.getAccessStatus() : AccessStatus.DENIED == ap.getAccessStatus())
                .filter(ap -> permission.equals(ap.getPermission())).anyMatch(ap -> authority.equals(ap.getAuthority()));
        return hasPermission;
    }
}
