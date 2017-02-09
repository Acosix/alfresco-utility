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

import java.util.List;
import java.util.Map;

/**
 * Instances of this class are used as dummy beans for unit tests.
 *
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class TestDummyBean
{

    private List<String> stringList;

    private List<Integer> integerList;

    private List<Boolean> booleanList;

    private List<Object> beanList;

    private Map<String, String> stringMap;

    private Map<String, Integer> integerMap;

    private Map<String, Boolean> booleanMap;

    private Map<String, Object> beanMap;

    private String stringValue;

    private Integer integerValue;

    private Boolean booleanValue;

    private Object beanReference;

    /**
     * @return the stringList
     */
    public List<String> getStringList()
    {
        return this.stringList;
    }

    /**
     * @param stringList
     *            the stringList to set
     */
    public void setStringList(final List<String> stringList)
    {
        this.stringList = stringList;
    }

    /**
     * @return the integerList
     */
    public List<Integer> getIntegerList()
    {
        return this.integerList;
    }

    /**
     * @param integerList
     *            the integerList to set
     */
    public void setIntegerList(final List<Integer> integerList)
    {
        this.integerList = integerList;
    }

    /**
     * @return the booleanList
     */
    public List<Boolean> getBooleanList()
    {
        return this.booleanList;
    }

    /**
     * @param booleanList
     *            the booleanList to set
     */
    public void setBooleanList(final List<Boolean> booleanList)
    {
        this.booleanList = booleanList;
    }

    /**
     * @return the beanList
     */
    public List<Object> getBeanList()
    {
        return this.beanList;
    }

    /**
     * @param beanList
     *            the beanList to set
     */
    public void setBeanList(final List<Object> beanList)
    {
        this.beanList = beanList;
    }

    /**
     * @return the stringMap
     */
    public Map<String, String> getStringMap()
    {
        return this.stringMap;
    }

    /**
     * @param stringMap
     *            the stringMap to set
     */
    public void setStringMap(final Map<String, String> stringMap)
    {
        this.stringMap = stringMap;
    }

    /**
     * @return the integerMap
     */
    public Map<String, Integer> getIntegerMap()
    {
        return this.integerMap;
    }

    /**
     * @param integerMap
     *            the integerMap to set
     */
    public void setIntegerMap(final Map<String, Integer> integerMap)
    {
        this.integerMap = integerMap;
    }

    /**
     * @return the booleanMap
     */
    public Map<String, Boolean> getBooleanMap()
    {
        return this.booleanMap;
    }

    /**
     * @param booleanMap
     *            the booleanMap to set
     */
    public void setBooleanMap(final Map<String, Boolean> booleanMap)
    {
        this.booleanMap = booleanMap;
    }

    /**
     * @return the beanMap
     */
    public Map<String, Object> getBeanMap()
    {
        return this.beanMap;
    }

    /**
     * @param beanMap
     *            the beanMap to set
     */
    public void setBeanMap(final Map<String, Object> beanMap)
    {
        this.beanMap = beanMap;
    }

    /**
     * @return the stringValue
     */
    public String getStringValue()
    {
        return this.stringValue;
    }

    /**
     * @param stringValue
     *            the stringValue to set
     */
    public void setStringValue(final String stringValue)
    {
        this.stringValue = stringValue;
    }

    /**
     * @return the integerValue
     */
    public Integer getIntegerValue()
    {
        return this.integerValue;
    }

    /**
     * @param integerValue
     *            the integerValue to set
     */
    public void setIntegerValue(final Integer integerValue)
    {
        this.integerValue = integerValue;
    }

    /**
     * @return the booleanValue
     */
    public Boolean getBooleanValue()
    {
        return this.booleanValue;
    }

    /**
     * @param booleanValue
     *            the booleanValue to set
     */
    public void setBooleanValue(final Boolean booleanValue)
    {
        this.booleanValue = booleanValue;
    }

    /**
     * @return the beanReference
     */
    public Object getBeanReference()
    {
        return this.beanReference;
    }

    /**
     * @param beanReference
     *            the beanReference to set
     */
    public void setBeanReference(final Object beanReference)
    {
        this.beanReference = beanReference;
    }

}
