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

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.impl.StaticLoggerBinder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class SubsystemWithClassLoaderManagerTest
{

    @Rule
    public ExpectedException exRule = ExpectedException.none();

    @Test
    public void simplyStartAll()
    {
        try (final ClassPathXmlApplicationContext ctxt = new ClassPathXmlApplicationContext(
                "classpath:subsystem-with-classtloader-manager-test-context.xml"))
        {
            final SubsystemWithClassLoaderManager manager = ctxt.getBean("SubsystemWithClassLoaderManagerTest",
                    SubsystemWithClassLoaderManager.class);
            Assert.assertNotNull("manager bean was not found", manager);

            final Collection<String> instanceIds = manager.getInstanceIds();
            Assert.assertEquals("Number of subsystems does not match", 2, instanceIds.size());
            instanceIds.forEach(id -> {
                final ApplicationContext childCtxt = manager.getApplicationContext(id);

                Assert.assertNotNull("subsystem " + id + " was not started", childCtxt);
            });
        }
    }

    @Test
    public void effectiveProperties()
    {
        try (final ClassPathXmlApplicationContext ctxt = new ClassPathXmlApplicationContext(
                "classpath:subsystem-with-classtloader-manager-test-context.xml"))
        {
            final SubsystemWithClassLoaderManager manager = ctxt.getBean("SubsystemWithClassLoaderManagerTest",
                    SubsystemWithClassLoaderManager.class);
            Assert.assertNotNull("Subsystem manager bean not found", manager);

            final Properties effectivePropertiesInst1 = manager.getSubsystemEffectiveProperties("inst1");

            Assert.assertEquals("Property 1 of inst1 does not match expectation", "global-value1",
                    effectivePropertiesInst1.get("subsystem.manager.prop1"));
            Assert.assertEquals("Property 2 of inst1 does not match expectation", "value2",
                    effectivePropertiesInst1.get("subsystem.manager.prop2"));

            final Properties effectivePropertiesInst2 = manager.getSubsystemEffectiveProperties("inst2");
            Assert.assertEquals("Property 1 of inst2 does not match expectation", "extension-value1",
                    effectivePropertiesInst2.get("subsystem.manager.prop1"));
            Assert.assertEquals("Property 2 of inst2 does not match expectation", "value2",
                    effectivePropertiesInst2.get("subsystem.manager.prop2"));
        }
    }

    @Test
    public void correctPropertyPriorities()
    {
        try (final ClassPathXmlApplicationContext ctxt = new ClassPathXmlApplicationContext(
                "classpath:subsystem-with-classtloader-manager-test-context.xml"))
        {
            final SubsystemWithClassLoaderManager manager = ctxt.getBean("SubsystemWithClassLoaderManagerTest",
                    SubsystemWithClassLoaderManager.class);
            Assert.assertNotNull("manager bean was not found", manager);

            final ApplicationContext inst1Ctxt = manager.getApplicationContext("inst1");
            Assert.assertNotNull("subsystem inst1 was not started", inst1Ctxt);

            final Map<?, ?> inst1Values = inst1Ctxt.getBean("values", Map.class);

            Assert.assertEquals("default value should have been overriden by alfresco-global.properties", "global-value1",
                    inst1Values.get("prop1"));
            Assert.assertEquals("default value should have been left unchanged", "value2", inst1Values.get("prop2"));
            Assert.assertEquals("missing default value should have been provided by alfresco-global.properties", "global-value3",
                    inst1Values.get("prop3"));

            final ApplicationContext inst2Ctxt = manager.getApplicationContext("inst2");
            Assert.assertNotNull("subsystem inst2 was not started", inst2Ctxt);
            final Map<?, ?> inst2Values = inst2Ctxt.getBean("values", Map.class);

            Assert.assertEquals(
                    "default value should have been overriden by alfresco-global.properties which in turn should have been overriden by extension value",
                    "extension-value1", inst2Values.get("prop1"));
            Assert.assertEquals("default value should have been left unchanged", "value2", inst2Values.get("prop2"));
            Assert.assertEquals("missing default value should have been provided by alfresco-global.properties", "global-value3",
                    inst2Values.get("prop3"));
        }
    }

    @Test
    public void defaultAndOverrideJar() throws Exception
    {
        try (final ClassPathXmlApplicationContext ctxt = new ClassPathXmlApplicationContext(
                "classpath:subsystem-with-classtloader-manager-test-context.xml"))
        {
            final SubsystemWithClassLoaderManager manager = ctxt.getBean("SubsystemWithClassLoaderManagerTest",
                    SubsystemWithClassLoaderManager.class);
            Assert.assertNotNull("Subsystem factory bean not found", manager);

            final ApplicationContext inst1Ctxt = manager.getApplicationContext("inst1");
            Assert.assertNotNull("subsystem inst1 was not started", inst1Ctxt);

            Object staticLoggerBinder = inst1Ctxt.getBean("staticLoggerBinder");
            Class<? extends Object> staticLoggerBinderClassFromJar = staticLoggerBinder.getClass();
            Assert.assertEquals("Class name does not match", "org.slf4j.impl.StaticLoggerBinder", staticLoggerBinderClassFromJar.getName());
            Field requestedApiVersionJarField = staticLoggerBinderClassFromJar.getField("REQUESTED_API_VERSION");
            Object requestedApiVersionJar = requestedApiVersionJarField.get(null);
            Assert.assertEquals("Constant for SLF4J API version in subsystem does not match", "1.7.16", requestedApiVersionJar);

            final ApplicationContext inst2Ctxt = manager.getApplicationContext("inst2");
            Assert.assertNotNull("subsystem inst2 was not started", inst1Ctxt);

            staticLoggerBinder = inst2Ctxt.getBean("staticLoggerBinder");
            staticLoggerBinderClassFromJar = staticLoggerBinder.getClass();
            Assert.assertEquals("Class name does not match", "org.slf4j.impl.StaticLoggerBinder", staticLoggerBinderClassFromJar.getName());
            requestedApiVersionJarField = staticLoggerBinderClassFromJar.getField("REQUESTED_API_VERSION");
            requestedApiVersionJar = requestedApiVersionJarField.get(null);
            Assert.assertEquals("Constant for SLF4J API version in subsystem does not match", "1.6", requestedApiVersionJar);
        }

        this.exRule.expect(ClassNotFoundException.class);
        final Class<?> logbackConsoleAppenderClass = Class.forName("ch.qos.logback.core.ConsoleAppender");
        Assert.assertFalse("Class ch.qos.logback.core.ConsoleAppender should not have been found in global scope",
                logbackConsoleAppenderClass != null);

        final Object requestedApiVersionJar = StaticLoggerBinder.REQUESTED_API_VERSION;
        Assert.assertEquals("Constant for SLF4J API version in global scope does not match", "1.6.99", requestedApiVersionJar);
    }
}
