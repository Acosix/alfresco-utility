/*
 * Copyright 2016 - 2020 Acosix GmbH
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
public class SimplePermissionRemovalPatchRule implements NodePatchRule, InitializingBean
{

    private static final Logger LOGGER = LoggerFactory.getLogger(SimplePermissionRemovalPatchRule.class);

    protected PermissionService permissionService;

    protected List<String> authorities;

    protected List<String> permissions;

    protected List<Pair<String, String>> exactAuthorityPermissions;

    protected AccessStatus accessStatus;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "permissionService", this.permissionService);
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
     * @param authorities
     *            the authorities to set
     */
    public void setAuthorities(final List<String> authorities)
    {
        this.authorities = authorities;
    }

    /**
     * @param permissions
     *            the permissions to set
     */
    public void setPermissions(final List<String> permissions)
    {
        this.permissions = permissions;
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
     * @param accessStatus
     *            the accessStatus to set
     */
    public void setAccessStatus(final AccessStatus accessStatus)
    {
        this.accessStatus = accessStatus;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void apply(final NodeRef node)
    {
        this.permissionService.getAllSetPermissions(node).stream()
                // this guards against deletePermission call on nodes without a defining ACL
                .filter(AccessPermission::isSetDirectly)
                // restrict by optional accessStatus config
                .filter(ap -> this.accessStatus == null || ap.getAccessStatus() == this.accessStatus)
                // restrict by actual assignment
                .filter(ap -> {
                    boolean accept = false;

                    if (this.exactAuthorityPermissions != null)
                    {
                        accept = accept || this.exactAuthorityPermissions.contains(new Pair<>(ap.getAuthority(), ap.getPermission()));
                    }

                    if (this.authorities != null || this.permissions != null)
                    {
                        accept = accept || ((this.authorities == null || this.authorities.contains(ap.getAuthority())
                                && (this.permissions == null || this.permissions.contains(ap.getPermission()))));
                    }

                    return accept;
                }).forEach(ap -> {
                    LOGGER.debug("Deleting {} permission {} for {} on {}", ap.getAccessStatus(), ap.getPermission(), ap.getAuthority(),
                            node);
                    this.permissionService.deletePermission(node, ap.getAuthority(), ap.getPermission());
                });
    }
}
