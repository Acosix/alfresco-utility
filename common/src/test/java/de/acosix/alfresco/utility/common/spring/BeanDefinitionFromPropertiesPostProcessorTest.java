/*
 * Copyright 2016 Acosix GmbH
 *
 * Licensed under the Eclipse Public License (EPL), Version 1.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package de.acosix.alfresco.utility.common.spring;

import org.alfresco.util.exec.RuntimeExec;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
@RunWith(BlockJUnit4ClassRunner.class)
public class BeanDefinitionFromPropertiesPostProcessorTest
{

    @Test
    public void simpleBean()
    {
        final ApplicationContext context = new ClassPathXmlApplicationContext(
                "classpath:BeanDefinitionFromPropertiesPostProcessorTest-simpleBean-context.xml");
        final Object simpleBean = context.getBean("beanTypeX.simpleBean");

        // TODO Switch with other simple bean class that provides a simple property that can be set AND get
        Assert.assertTrue("beanTypeX.simpleBean is not of expected type", simpleBean instanceof RuntimeExec);
        Assert.assertArrayEquals("command is not of expected array value", new String[] { "dummy", "cmd" },
                ((RuntimeExec) simpleBean).getCommand());
    }

    // TODO Test constructs with all types of properties
}
