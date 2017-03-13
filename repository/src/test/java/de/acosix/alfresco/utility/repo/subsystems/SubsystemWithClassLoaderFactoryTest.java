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
public class SubsystemWithClassLoaderFactoryTest
{

    @Rule
    public ExpectedException exRule = ExpectedException.none();

    @Test
    public void effectiveProperties()
    {
        try (final ClassPathXmlApplicationContext ctxt = new ClassPathXmlApplicationContext(
                "classpath:subsystem-with-classloader-factory-test-simple-context.xml"))
        {
            final SubsystemWithClassLoaderFactory factory = ctxt.getBean("SubsystemWithClassLoaderFactoryTest-simple",
                    SubsystemWithClassLoaderFactory.class);
            Assert.assertNotNull("Subsystem factory bean not found", factory);

            final Properties effectiveProperties = factory.getSubsystemEffectiveProperties();

            Assert.assertEquals("Global value does not match", "extension-value1", effectiveProperties.get("subsystem.manager.prop1"));
            Assert.assertEquals("Default value does not match", "value2", effectiveProperties.get("subsystem.manager.prop2"));
            Assert.assertEquals("Extension value does not match", "global-value3", effectiveProperties.get("subsystem.manager.prop3"));
        }
    }

    @Test
    public void isolatedProperties()
    {
        try (final ClassPathXmlApplicationContext ctxt = new ClassPathXmlApplicationContext(
                "classpath:subsystem-with-classloader-factory-test-simple-context.xml"))
        {
            final SubsystemWithClassLoaderFactory factory = ctxt.getBean("SubsystemWithClassLoaderFactoryTest-simple",
                    SubsystemWithClassLoaderFactory.class);
            Assert.assertNotNull("Subsystem factory bean not found", factory);

            final ApplicationContext innerCtxt = factory.getApplicationContext();
            final Properties isolatedProperties = innerCtxt.getBean("isolatedProperties", Properties.class);

            Assert.assertEquals("Default value does not match", "value1", isolatedProperties.get("subsystem.isolated.prop1"));
            Assert.assertEquals("Extension value does not match", "extension-value2", isolatedProperties.get("subsystem.isolated.prop2"));
        }
    }

    @Test
    public void defaultJarInSubsystem() throws Exception
    {
        try (final ClassPathXmlApplicationContext ctxt = new ClassPathXmlApplicationContext(
                "classpath:subsystem-with-classloader-factory-test-jar-context.xml"))
        {
            final SubsystemWithClassLoaderFactory factory = ctxt.getBean("SubsystemWithClassLoaderFactoryTest-jar",
                    SubsystemWithClassLoaderFactory.class);
            Assert.assertNotNull("Subsystem factory bean not found", factory);

            final ApplicationContext innerCtxt = factory.getApplicationContext();
            final Object logbackConsoleAppender = innerCtxt.getBean("logback-console-appender");

            Assert.assertEquals("Class name does not match", "ch.qos.logback.core.ConsoleAppender",
                    logbackConsoleAppender.getClass().getName());

            final Object staticLoggerBinder = innerCtxt.getBean("staticLoggerBinder");
            final Class<? extends Object> staticLoggerBinderClassFromJar = staticLoggerBinder.getClass();
            Assert.assertEquals("Class name does not match", "org.slf4j.impl.StaticLoggerBinder", staticLoggerBinderClassFromJar.getName());
            final Field requestedApiVersionJarField = staticLoggerBinderClassFromJar.getField("REQUESTED_API_VERSION");
            final Object requestedApiVersionJar = requestedApiVersionJarField.get(null);
            Assert.assertEquals("Constant for SLF4J API version in subsystem does not match", "1.6", requestedApiVersionJar);
        }

        this.exRule.expect(ClassNotFoundException.class);
        final Class<?> logbackConsoleAppenderClass = Class.forName("ch.qos.logback.core.ConsoleAppender");
        Assert.assertFalse("Class ch.qos.logback.core.ConsoleAppender should not have been found in global scope",
                logbackConsoleAppenderClass != null);

        final Object requestedApiVersionJar = StaticLoggerBinder.REQUESTED_API_VERSION;
        Assert.assertEquals("Constant for SLF4J API version in global scope does not match", "1.6.99", requestedApiVersionJar);
    }

    @Test
    public void overrideJarInSubsystem() throws Exception
    {
        try (final ClassPathXmlApplicationContext ctxt = new ClassPathXmlApplicationContext(
                "classpath:subsystem-with-classloader-factory-test-jarWithOverride-context.xml"))
        {
            final SubsystemWithClassLoaderFactory factory = ctxt.getBean("SubsystemWithClassLoaderFactoryTest-jarWithOverride",
                    SubsystemWithClassLoaderFactory.class);
            Assert.assertNotNull("Subsystem factory bean not found", factory);

            final ApplicationContext innerCtxt = factory.getApplicationContext();
            final Object staticLoggerBinder = innerCtxt.getBean("staticLoggerBinder");
            final Class<? extends Object> staticLoggerBinderClassFromJar = staticLoggerBinder.getClass();
            Assert.assertEquals("Class name does not match", "org.slf4j.impl.StaticLoggerBinder", staticLoggerBinderClassFromJar.getName());
            final Field requestedApiVersionJarField = staticLoggerBinderClassFromJar.getField("REQUESTED_API_VERSION");
            final Object requestedApiVersionJar = requestedApiVersionJarField.get(null);
            Assert.assertEquals("Constant for SLF4J API version in subsystem does not match", "1.7.16", requestedApiVersionJar);
        }

        this.exRule.expect(ClassNotFoundException.class);
        final Class<?> logbackConsoleAppenderClass = Class.forName("ch.qos.logback.core.ConsoleAppender");
        Assert.assertFalse("Class ch.qos.logback.core.ConsoleAppender should not have been found in global scope",
                logbackConsoleAppenderClass != null);

        final Object requestedApiVersionJar = StaticLoggerBinder.REQUESTED_API_VERSION;
        Assert.assertEquals("Constant for SLF4J API version in global scope does not match", "1.6.99", requestedApiVersionJar);
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void beanDefinitionsAndPlaceholderResolution()
    {
        try (final ClassPathXmlApplicationContext ctxt = new ClassPathXmlApplicationContext(
                "classpath:subsystem-with-classloader-factory-test-simple-context.xml"))
        {
            final SubsystemWithClassLoaderFactory factory = ctxt.getBean("SubsystemWithClassLoaderFactoryTest-simple",
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
