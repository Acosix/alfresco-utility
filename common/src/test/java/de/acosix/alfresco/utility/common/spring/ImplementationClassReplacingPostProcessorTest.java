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
package de.acosix.alfresco.utility.common.spring;

import org.alfresco.util.exec.RuntimeExec;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class ImplementationClassReplacingPostProcessorTest
{

    @Test
    public void byName()
    {
        try (final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                "classpath:ImplementationClassReplacingPostProcessorTest/byName-context.xml"))
        {
            final Object testBean = context.getBean("testBean");

            Assert.assertTrue("testBean should have been specialized", testBean instanceof TestDummyBean);
        }
    }

    @Test
    public void byNameAndClass()
    {
        try (final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                "classpath:ImplementationClassReplacingPostProcessorTest/byNameAndClass-context.xml"))
        {
            final Object testBean = context.getBean("testBean");

            Assert.assertTrue("testBean should have been specialized", testBean instanceof TestDummyBean);
        }
    }

    @Test
    public void missingTargetBean()
    {
        try (final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                "classpath:ImplementationClassReplacingPostProcessorTest/missingTargetBean-context.xml"))
        {
            final Object testBean = context.getBean("testBean");

            Assert.assertFalse("testBean should should have been specialized", testBean instanceof TestDummyBean);
            Assert.assertTrue("testBean should have been an instanceof of RuntimeExec", testBean instanceof RuntimeExec);
        }
    }

    @Test
    public void missingReplacementClass()
    {
        try (final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                "classpath:ImplementationClassReplacingPostProcessorTest/missingReplacementClass-context.xml"))
        {
            final Object testBean = context.getBean("testBean");

            Assert.assertFalse("testBean should should have been specialized", testBean instanceof TestDummyBean);
            Assert.assertTrue("testBean should have been an instanceof of RuntimeExec", testBean instanceof RuntimeExec);
        }
    }

    @Test
    public void byNameAndClass_disabled()
    {
        try (final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                "classpath:ImplementationClassReplacingPostProcessorTest/byNameAndClass-disabled-context.xml"))
        {
            final Object testBean = context.getBean("testBean");

            Assert.assertFalse("testBean should not have been specialized", testBean instanceof TestDummyBean);
            Assert.assertTrue("testBean should have been an instanceof of RuntimeExec", testBean instanceof RuntimeExec);
        }
    }

    // TODO Tests against early-binding beans (AbstractLifecycleBean)
}
