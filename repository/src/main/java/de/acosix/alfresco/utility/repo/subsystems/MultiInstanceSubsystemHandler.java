/*
 * Copyright 2017 Acosix GmbH
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
package de.acosix.alfresco.utility.repo.subsystems;

import java.util.Properties;

import org.alfresco.repo.management.subsystems.ChildApplicationContextManager;
import org.alfresco.repo.management.subsystems.PropertyBackedBeanState;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

/**
 * Instances of this interface are responsible for handling subsystems that may consist of multiple instances with different configurations
 * and potentially being active concurrently.
 *
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public interface MultiInstanceSubsystemHandler extends ChildApplicationContextManager
{

    /**
     * Determines the instance ID for a specific child application context from within all the child application contexts managed by this
     * instance.
     *
     * @param childApplicationContext
     *            the child application context
     * @return the ID of the child application instance or {@code null} if none of the currently active child application contexts match the
     *         provided one
     */
    String determineInstanceId(ApplicationContext childApplicationContext);

    /**
     * Looks up the default subsystem configuration properties files for the provided subsystem instance.
     *
     * @param instanceId
     *            the ID of the subsystem instance for which to look up the default configuration properties files
     * @return the list of default configuration properties files
     */
    Resource[] getSubsystemDefaultPropertiesResources(String instanceId);

    /**
     * Looks up the extension subsystem configuration properties files for the provided subsystem instance.
     *
     * @param instanceId
     *            the ID of the subsystem instance for which to look up the extension configuration properties files
     * @return the list of extension configuration properties files
     */
    Resource[] getSubsystemExtensionPropertiesResources(String instanceId);

    /**
     * Retrieves a view of the currently applied configuration properties within a specific subsystem instance. This will reflect the state
     * of any properties configured via {@code *.properties} files as well as any {@link PropertyBackedBeanState runtime configuration
     * state}.
     *
     * @param instanceId
     *            the ID of the subsystem instance for which to look up the effective configuration properties
     * @return the effective configuration properties - may be empty if the subystem was not be properly initialised / started
     */
    Properties getSubsystemEffectiveProperties(String instanceId);
}
