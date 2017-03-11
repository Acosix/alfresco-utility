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
package de.acosix.alfresco.utility.repo.subsystems;

import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class SubsystemWithClassLoaderFactoryTest
{

    @Test
    public void effectiveProperties()
    {
        try (final ClassPathXmlApplicationContext ctxt = new ClassPathXmlApplicationContext(
                "classpath:subsystem-with-classloader-factory-test-context.xml"))
        {
            final SubsystemWithClassLoaderFactory factory = ctxt.getBean("SubsystemWithClassLoaderFactoryTest",
                    SubsystemWithClassLoaderFactory.class);
            Assert.assertNotNull("Subsystem factory bean not found", factory);

            final Properties effectiveProperties = factory.getSubsystemEffectiveProperties();

            Assert.assertEquals("Global value does not match", "extension-value1", effectiveProperties.get("subsystem.manager.prop1"));
            Assert.assertEquals("Default value does not match", "value2", effectiveProperties.get("subsystem.manager.prop2"));
            Assert.assertEquals("Extension value does not match", "global-value3", effectiveProperties.get("subsystem.manager.prop3"));
        }
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void beanDefinitionsAndPlaceholderResolution()
    {
        try (final ClassPathXmlApplicationContext ctxt = new ClassPathXmlApplicationContext(
                "classpath:subsystem-with-classloader-factory-test-context.xml"))
        {
            final SubsystemWithClassLoaderFactory factory = ctxt.getBean("SubsystemWithClassLoaderFactoryTest",
                    SubsystemWithClassLoaderFactory.class);
            Assert.assertNotNull("Subsystem factory bean not found", factory);

            final ApplicationContext applicationContext = factory.getApplicationContext();

            final Map defaultMap = applicationContext.getBean("defaultMap", Map.class);
            Assert.assertEquals("Default map size does not match", 2, defaultMap.size());
            Assert.assertEquals("Value placeholder for prop1 was not replaced with configured value in defaultMap", "extension-value1",
                    defaultMap.get("prop1"));
            Assert.assertEquals("Value placeholder for prop2 was not replaced with configured value in defaultMap", "value2",
                    defaultMap.get("prop2"));

            final Map modifiableMap = applicationContext.getBean("modifiableMap", Map.class);
            Assert.assertEquals("Modifiable map size does not match", 3, modifiableMap.size());
            Assert.assertEquals("Value placeholder for prop1 was not replaced with configured value in modifiableMap", "extension-value1",
                    modifiableMap.get("prop1"));
            Assert.assertEquals("Value placeholder for prop2 was not replaced with configured value in modifiableMap", "value2",
                    modifiableMap.get("prop2"));
            Assert.assertEquals("Value placeholder for prop3 was not replaced with configured value in modifiableMap", "global-value3",
                    modifiableMap.get("prop3"));
        }
    }
}
