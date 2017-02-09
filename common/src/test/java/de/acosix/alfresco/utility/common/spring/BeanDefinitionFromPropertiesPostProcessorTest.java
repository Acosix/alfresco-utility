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

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.internal.ArrayComparisonFailure;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
@RunWith(BlockJUnit4ClassRunner.class)
public class BeanDefinitionFromPropertiesPostProcessorTest
{

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void singleton()
    {
        try (final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                "classpath:BeanDefinitionFromPropertiesPostProcessorTest/singleton-context.xml"))
        {
            final TestDummyBean simpleBean = context.getBean("beanTypeX.simpleBean", TestDummyBean.class);

            final TestDummyBean refBean1 = context.getBean("beanTypeX.refBean1", TestDummyBean.class);
            final TestDummyBean refBean2 = context.getBean("beanTypeX.refBean2", TestDummyBean.class);
            final TestDummyBean refBean3 = context.getBean("beanTypeX.refBean3", TestDummyBean.class);

            this.verifyBean(simpleBean, refBean1, refBean2, refBean3);
        }
    }

    @Test
    public void prototype()
    {
        try (final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                "classpath:BeanDefinitionFromPropertiesPostProcessorTest/proto-context.xml"))
        {
            final TestDummyBean instance1 = context.getBean("beanTypeX.simpleBean.proto", TestDummyBean.class);
            final TestDummyBean instance2 = context.getBean("beanTypeX.simpleBean.proto", TestDummyBean.class);

            final TestDummyBean refBean1 = context.getBean("beanTypeX.refBean1", TestDummyBean.class);
            final TestDummyBean refBean2 = context.getBean("beanTypeX.refBean2", TestDummyBean.class);
            final TestDummyBean refBean3 = context.getBean("beanTypeX.refBean3", TestDummyBean.class);

            Assert.assertFalse("instance1 is not a different object than instance 2", instance1 == instance2);

            this.verifyBean(instance1, refBean1, refBean2, refBean3);
            this.verifyBean(instance2, refBean1, refBean2, refBean3);
        }
    }

    @Test
    public void abstractParent()
    {
        try (final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                "classpath:BeanDefinitionFromPropertiesPostProcessorTest/abstractParent-context.xml"))
        {
            final TestDummyBean concreteInstance = context.getBean("beanTypeX.simpleBean.withParent", TestDummyBean.class);

            final TestDummyBean refBean1 = context.getBean("beanTypeX.refBean1", TestDummyBean.class);
            final TestDummyBean refBean2 = context.getBean("beanTypeX.refBean2", TestDummyBean.class);
            final TestDummyBean refBean3 = context.getBean("beanTypeX.refBean3", TestDummyBean.class);

            this.verifyBean(concreteInstance, refBean1, refBean2, refBean3);
        }
    }

    @Test
    public void renameBean()
    {
        try (final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                "classpath:BeanDefinitionFromPropertiesPostProcessorTest/renameBean-context.xml"))
        {
            Assert.assertFalse("beanTypeX.bean1 should have been renamed", context.containsBean("beanTypeX.bean1"));
            Assert.assertTrue("beanTypeX.bean2 should not have been renamed", context.containsBean("beanTypeX.bean2"));

            final TestDummyBean simpleBean = context.getBean("beanTypeY.bean1", TestDummyBean.class);

            final TestDummyBean refBean1 = context.getBean("beanTypeX.refBean1", TestDummyBean.class);
            final TestDummyBean refBean2 = context.getBean("beanTypeX.refBean2", TestDummyBean.class);
            final TestDummyBean refBean3 = context.getBean("beanTypeX.refBean3", TestDummyBean.class);

            this.verifyBean(simpleBean, refBean1, refBean2, refBean3);
        }
    }

    @Test
    public void removeBean()
    {
        try (final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                "classpath:BeanDefinitionFromPropertiesPostProcessorTest/removeBean-context.xml"))
        {
            Assert.assertFalse("beanTypeX.bean1 should have been removed", context.containsBean("beanTypeX.bean1"));
            Assert.assertTrue("beanTypeX.bean2 should not have been removed", context.containsBean("beanTypeX.bean2"));
        }
    }

    // TODO Test constructs with override / removal of predefined beans

    protected void verifyBean(final TestDummyBean beanToVerify, final TestDummyBean refBean1, final TestDummyBean refBean2,
            final TestDummyBean refBean3) throws ArrayComparisonFailure
    {
        Assert.assertArrayEquals("stringList does not contain expected values", new String[] { "value 1", "value 2" },
                beanToVerify.getStringList().toArray(new String[0]));
        Assert.assertArrayEquals("numberList does not contain expected values", new Integer[] { Integer.valueOf(1), Integer.valueOf(2) },
                beanToVerify.getIntegerList().toArray(new Integer[0]));
        Assert.assertArrayEquals("booleanList does not contain expected values", new Boolean[] { Boolean.TRUE, Boolean.FALSE },
                beanToVerify.getBooleanList().toArray(new Boolean[0]));
        Assert.assertArrayEquals("beanList does not contain expected bean", new Object[] { refBean1, refBean2 },
                beanToVerify.getBeanList().toArray(new Object[0]));

        Assert.assertTrue("stringMap does not contain expected keys",
                beanToVerify.getStringMap().keySet().containsAll(Arrays.asList("strkey1", "strkey2")));
        Assert.assertEquals("stringMap[strkey1] does match expected value", "value 1", beanToVerify.getStringMap().get("strkey1"));
        Assert.assertEquals("stringMap[strkey2] does match expected value", "value 2", beanToVerify.getStringMap().get("strkey2"));
        Assert.assertTrue("integerMap does not contain expected keys",
                beanToVerify.getIntegerMap().keySet().containsAll(Arrays.asList("intkey1", "intkey2")));
        Assert.assertEquals("integerMap[intkey1] does match expected value", Integer.valueOf(1),
                beanToVerify.getIntegerMap().get("intkey1"));
        Assert.assertEquals("integerMap[intkey2] does match expected value", Integer.valueOf(2),
                beanToVerify.getIntegerMap().get("intkey2"));
        Assert.assertTrue("booleanMap does not contain expected keys",
                beanToVerify.getBooleanMap().keySet().containsAll(Arrays.asList("boolkey1", "boolkey2")));
        Assert.assertEquals("booleanMap[boolkey1] does match expected value", Boolean.TRUE, beanToVerify.getBooleanMap().get("boolkey1"));
        Assert.assertEquals("booleanMap[boolkey2] does match expected value", Boolean.FALSE, beanToVerify.getBooleanMap().get("boolkey2"));
        Assert.assertTrue("beanMap does not contain expected keys",
                beanToVerify.getBeanMap().keySet().containsAll(Arrays.asList("beankey1", "beankey2")));
        Assert.assertSame("beanMap[beankey1] does match expected bean", refBean1, beanToVerify.getBeanMap().get("beankey1"));
        Assert.assertSame("beanMap[beankey2] does match expected bean", refBean2, beanToVerify.getBeanMap().get("beankey2"));

        Assert.assertEquals("stringValue does not match expected value", "dummy", beanToVerify.getStringValue());
        Assert.assertEquals("numberValue does not match expected value", Integer.valueOf(3), beanToVerify.getIntegerValue());
        Assert.assertEquals("booleanValue does not match expected value", Boolean.TRUE, beanToVerify.getBooleanValue());
        Assert.assertSame("beanReference does not match expected bean", refBean3, beanToVerify.getBeanReference());
    }
}
