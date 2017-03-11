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

import org.alfresco.repo.management.subsystems.ApplicationContextFactory;
import org.alfresco.repo.management.subsystems.PropertyBackedBeanState;
import org.springframework.core.io.Resource;

/**
 * Instances of this interface are responsible for handling a simple subsystem instance.
 *
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public interface SingleInstanceSubsystemHandler extends ApplicationContextFactory
{

    /**
     * Looks up the default subsystem configuration properties files for the this subsystem instance.
     *
     * @return the list of default configuration properties files
     */
    Resource[] getSubsystemDefaultPropertiesResources();

    /**
     * Looks up the extension subsystem configuration properties files for the this subsystem instance.
     *
     * @return the list of extension configuration properties files
     */
    Resource[] getSubsystemExtensionPropertiesResources();

    /**
     * Retrieves a view of the currently applied configuration properties within this subsystem instance. This will reflect the state
     * of any properties configured via {@code *.properties} files as well as any {@link PropertyBackedBeanState runtime configuration
     * state}.
     *
     * @return the effective configuration properties
     */
    Properties getSubsystemEffectiveProperties();
}
