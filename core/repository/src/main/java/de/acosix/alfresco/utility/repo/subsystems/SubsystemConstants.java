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
package de.acosix.alfresco.utility.repo.subsystems;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * @author Axel Faust
 */
public interface SubsystemConstants
{

    String BEAN_NAME_MONITOR = "monitor";

    String BEAN_NAME_SUBSYSTEM_PROPERTIES = "subsystem-properties";

    String PROPERTY_CATEGORY = "$category";

    String PROPERTY_TYPE = "$type";

    String PROPERTY_ID = "$id";

    String PROPERTY_INSTANCE_PATH = "$instancePath";

    Collection<String> NON_UPDATEABLE_PROPERTIES = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList(PROPERTY_CATEGORY, PROPERTY_TYPE, PROPERTY_ID, PROPERTY_INSTANCE_PATH)));

    String BASE_SUBSYSTEM_CONTEXT = "classpath:alfresco/module/acosix-utility/default-subsystem-context.xml";

    String CLASSPATH_WILDCARD_PROTOCOL = "classpath*:";

    String CLASSPATH_ALFRESCO_SUBSYSTEMS = CLASSPATH_WILDCARD_PROTOCOL + "alfresco/subsystems/";

    String CLASSPATH_ALFRESCO_EXTENSION_SUBSYSTEMS = CLASSPATH_WILDCARD_PROTOCOL + "alfresco/extension/subsystems/";

    String PROPERTIES_FILE_PATTERN = "*.properties";

    String CONTEXT_FILE_PATTERN = "*-context.xml";

    String CLASSES_FOLDER_NAME = "classes";

    String JAR_FILE_LOOKUP_PATTERN = "lib/*.jar";

    String CONTEXT_ENTERPRISE_FILE_PATTERN = "*-enterprise-context.xml";

    String CLASSPATH_DELIMITER = "/";

    String SUBSYSTEM_PROPERTY_NAME_PATTERNS = "subsystem.propertyName.patterns";
}
