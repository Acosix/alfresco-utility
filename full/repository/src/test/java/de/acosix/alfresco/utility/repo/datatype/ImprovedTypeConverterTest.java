/*
 * Copyright 2016 - 2020 Acosix GmbH
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
package de.acosix.alfresco.utility.repo.datatype;

import java.lang.reflect.Constructor;
import java.util.Locale;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.MalformedNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.DynamicNamespacePrefixResolver;
import org.alfresco.service.namespace.InvalidQNameException;
import org.alfresco.service.namespace.NamespaceException;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.PropertyConfigurator;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Axel Faust
 */
public class ImprovedTypeConverterTest
{

    @BeforeClass
    public static void beforeClass()
    {
        PropertyConfigurator.configure(
                ImprovedTypeConverterTest.class.getClassLoader().getResourceAsStream("alfresco/extension/test-log4j.properties"));
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @After
    public void tearDown() throws Exception
    {
        // reset central type converter instance
        // due to visibility restrictions on ctor we have to use reflection (will fail in Java 9)
        final Constructor<DefaultTypeConverter> ctor = DefaultTypeConverter.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        DefaultTypeConverter.INSTANCE = ctor.newInstance();
    }

    @Test
    public void defaultStringToNodeRef()
    {
        // tests Alfresco default converter
        final String validNodeRef = "workspace://SpacesStore/abcdefg";
        final NodeRef validNodeRefRes = DefaultTypeConverter.INSTANCE.convert(NodeRef.class, validNodeRef);
        Assert.assertEquals("Default String to NodeRef conversion did not convert valid NodeRef correctly",
                new NodeRef("workspace", "SpacesStore", "abcdefg"), validNodeRefRes);

        final String nullNodeRef = null;
        final NodeRef nullNodeRefRes = DefaultTypeConverter.INSTANCE.convert(NodeRef.class, nullNodeRef);
        Assert.assertNull("Default String to NodeRef conversion did not return null NodeRef for null input", nullNodeRefRes);
    }

    @Test
    public void defaultStringToNodeRef_noUUID()
    {
        // tests Alfresco default converter
        // should trigger error in StoreRef
        this.thrown.expect(AlfrescoRuntimeException.class);
        final String invalidNodeRef = "workspace://SpacesStore";
        DefaultTypeConverter.INSTANCE.convert(NodeRef.class, invalidNodeRef);
        Assert.fail("Default String to NodeRef conversion with input lacking UUID should have failed");
    }

    @Test
    public void defaultStringToNodeRef_noSlash()
    {
        // tests Alfresco default converter
        // should trigger error in NodeRef
        this.thrown.expect(MalformedNodeRefException.class);
        final String invalidNodeRef = "workspace";
        DefaultTypeConverter.INSTANCE.convert(NodeRef.class, invalidNodeRef);
        Assert.fail("Default String to NodeRef conversion with input lacking forward slash should have failed");
    }

    @Test
    public void defaultStringToNodeRef_emptyString()
    {
        // tests Alfresco default converter
        // should trigger error in NodeRef
        this.thrown.expect(MalformedNodeRefException.class);
        final String invalidNodeRef = "";
        DefaultTypeConverter.INSTANCE.convert(NodeRef.class, invalidNodeRef);
        Assert.fail("Default String to NodeRef conversion with empty String input should have failed");
    }

    @Test
    public void stringToNodeRef()
    {
        final ImprovedTypeConverterInitialiser initialiser = new ImprovedTypeConverterInitialiser();
        initialiser.setStringToNodeRefEnabled(true);
        initialiser.afterPropertiesSet();

        final String validNodeRef = "workspace://SpacesStore/abcdefg";
        final NodeRef validNodeRefRes = DefaultTypeConverter.INSTANCE.convert(NodeRef.class, validNodeRef);
        Assert.assertEquals("Improved String to NodeRef conversion did not convert valid NodeRef correctly",
                new NodeRef("workspace", "SpacesStore", "abcdefg"), validNodeRefRes);

        final String nullNodeRef = null;
        final NodeRef nullNodeRefRes = DefaultTypeConverter.INSTANCE.convert(NodeRef.class, nullNodeRef);
        Assert.assertNull("Improved String to NodeRef conversion did not return null NodeRef for null input", nullNodeRefRes);
    }

    @Test
    public void stringToNodeRef_noUUID()
    {
        final ImprovedTypeConverterInitialiser initialiser = new ImprovedTypeConverterInitialiser();
        initialiser.setStringToNodeRefEnabled(true);
        initialiser.afterPropertiesSet();

        // should trigger error in StoreRef
        this.thrown.expect(AlfrescoRuntimeException.class);
        final String invalidNodeRef = "workspace://SpacesStore";
        DefaultTypeConverter.INSTANCE.convert(NodeRef.class, invalidNodeRef);
        Assert.fail("Improved String to NodeRef conversion with input lacking UUID should have failed");
    }

    @Test
    public void stringToNodeRef_noSlash()
    {
        final ImprovedTypeConverterInitialiser initialiser = new ImprovedTypeConverterInitialiser();
        initialiser.setStringToNodeRefEnabled(true);
        initialiser.afterPropertiesSet();

        // should trigger error in NodeRef
        this.thrown.expect(MalformedNodeRefException.class);
        final String invalidNodeRef = "workspace";
        DefaultTypeConverter.INSTANCE.convert(NodeRef.class, invalidNodeRef);
        Assert.fail("Improved String to NodeRef conversion with input lacking forward slash should have failed");
    }

    @Test
    public void stringToNodeRef_emptyString()
    {
        final ImprovedTypeConverterInitialiser initialiser = new ImprovedTypeConverterInitialiser();
        initialiser.setStringToNodeRefEnabled(true);
        initialiser.afterPropertiesSet();

        final String emptyStr = "";
        final NodeRef emptyStrRes = DefaultTypeConverter.INSTANCE.convert(NodeRef.class, emptyStr);
        Assert.assertNull("Improved String to NodeRef conversion did not return null NodeRef for empty String input", emptyStrRes);

        final String whiteSpaceOnlyStr = " \t";
        final NodeRef whiteSpaceOnlyRes = DefaultTypeConverter.INSTANCE.convert(NodeRef.class, whiteSpaceOnlyStr);
        Assert.assertNull("Improved String to NodeRef conversion did not return null NodeRef for whitespace-only String input",
                whiteSpaceOnlyRes);
    }

    @Test
    public void defaultStringToQName()
    {
        final String fullQName = "{" + NamespaceService.CONTENT_MODEL_1_0_URI + "}content";
        final QName fullQNameRes = DefaultTypeConverter.INSTANCE.convert(QName.class, fullQName);
        Assert.assertEquals("Default String to QName conversion did not convert fully qualified name correctly", ContentModel.PROP_CONTENT,
                fullQNameRes);

        final String implicitNamespaceQName = "content";
        final QName implicitNamespaceQNameRes = DefaultTypeConverter.INSTANCE.convert(QName.class, implicitNamespaceQName);
        Assert.assertNotEquals("Default String to QName conversion did convert name with implicit default namespace correctly",
                ContentModel.PROP_CONTENT, implicitNamespaceQNameRes);

        final String prefixedQName = "cm:content";
        final QName prefixedQNameRes = DefaultTypeConverter.INSTANCE.convert(QName.class, prefixedQName);
        Assert.assertNotEquals("Default String to QName conversion did convert qualified name in prefix form correctly",
                ContentModel.PROP_CONTENT, prefixedQNameRes);
    }

    @Test
    public void defaultStringToQName_invalidNamespaceBegin()
    {
        this.thrown.expect(InvalidQNameException.class);
        final String invalidQName = "prefix{" + NamespaceService.CONTENT_MODEL_1_0_URI + "}content";
        DefaultTypeConverter.INSTANCE.convert(QName.class, invalidQName);
        Assert.fail("Default String to QName conversion should have failed");
    }

    @Test
    public void defaultStringToQName_missingNamespaceEnd()
    {
        this.thrown.expect(InvalidQNameException.class);
        final String invalidQName = "{" + NamespaceService.CONTENT_MODEL_1_0_URI + "content";
        DefaultTypeConverter.INSTANCE.convert(QName.class, invalidQName);
        Assert.fail("Default String to QName conversion should have failed");
    }

    @Test
    public void defaultStringToQName_missingLocalName()
    {
        this.thrown.expect(InvalidQNameException.class);
        final String invalidQName = "{" + NamespaceService.CONTENT_MODEL_1_0_URI + "}";
        DefaultTypeConverter.INSTANCE.convert(QName.class, invalidQName);
        Assert.fail("Default String to QName conversion should have failed");
    }

    @Test
    public void stringToQName()
    {
        final ImprovedTypeConverterInitialiser initialiser = new ImprovedTypeConverterInitialiser();
        initialiser.setStringToQNameEnabled(true);
        final NamespaceService prefixResolver = new DynamicNamespacePrefixResolver();
        prefixResolver.registerNamespace(NamespaceService.SYSTEM_MODEL_PREFIX, NamespaceService.SYSTEM_MODEL_1_0_URI);
        prefixResolver.registerNamespace(NamespaceService.CONTENT_MODEL_PREFIX, NamespaceService.CONTENT_MODEL_1_0_URI);
        initialiser.setNamespaceService(prefixResolver);
        initialiser.afterPropertiesSet();

        final String fullQName = "{" + NamespaceService.CONTENT_MODEL_1_0_URI + "}content";
        final QName fullQNameRes = DefaultTypeConverter.INSTANCE.convert(QName.class, fullQName);
        Assert.assertEquals("Improved String to QName conversion did not convert fully qualified name correctly", ContentModel.PROP_CONTENT,
                fullQNameRes);

        final String implicitNamespaceQName = "content";
        final QName implicitNamespaceQNameRes = DefaultTypeConverter.INSTANCE.convert(QName.class, implicitNamespaceQName);
        Assert.assertEquals("Improved String to QName conversion did not convert name with implicit default namespace correctly",
                ContentModel.PROP_CONTENT, implicitNamespaceQNameRes);

        final String prefixedQName = "cm:content";
        final QName prefixedQNameRes = DefaultTypeConverter.INSTANCE.convert(QName.class, prefixedQName);
        Assert.assertEquals("Improved String to QName conversion did not convert qualified name in prefix form correctly",
                ContentModel.PROP_CONTENT, prefixedQNameRes);
    }

    @Test
    public void stringToQName_invalidNamespaceBegin()
    {
        final ImprovedTypeConverterInitialiser initialiser = new ImprovedTypeConverterInitialiser();
        initialiser.setStringToQNameEnabled(true);
        final NamespaceService prefixResolver = new DynamicNamespacePrefixResolver();
        prefixResolver.registerNamespace(NamespaceService.SYSTEM_MODEL_PREFIX, NamespaceService.SYSTEM_MODEL_1_0_URI);
        prefixResolver.registerNamespace(NamespaceService.CONTENT_MODEL_PREFIX, NamespaceService.CONTENT_MODEL_1_0_URI);
        initialiser.setNamespaceService(prefixResolver);
        initialiser.afterPropertiesSet();

        this.thrown.expect(NamespaceException.class);
        final String invalidQName = "prefix{" + NamespaceService.CONTENT_MODEL_1_0_URI + "}content";
        DefaultTypeConverter.INSTANCE.convert(QName.class, invalidQName);
        Assert.fail("Improved String to QName conversion should have failed");
    }

    @Test
    public void stringToQName_missingNamespaceEnd()
    {
        final ImprovedTypeConverterInitialiser initialiser = new ImprovedTypeConverterInitialiser();
        initialiser.setStringToQNameEnabled(true);
        final NamespaceService prefixResolver = new DynamicNamespacePrefixResolver();
        prefixResolver.registerNamespace(NamespaceService.SYSTEM_MODEL_PREFIX, NamespaceService.SYSTEM_MODEL_1_0_URI);
        prefixResolver.registerNamespace(NamespaceService.CONTENT_MODEL_PREFIX, NamespaceService.CONTENT_MODEL_1_0_URI);
        initialiser.setNamespaceService(prefixResolver);
        initialiser.afterPropertiesSet();

        this.thrown.expect(InvalidQNameException.class);
        final String invalidQName = "{" + NamespaceService.CONTENT_MODEL_1_0_URI + "content";
        DefaultTypeConverter.INSTANCE.convert(QName.class, invalidQName);
        Assert.fail("Improved String to QName conversion should have failed");
    }

    @Test
    public void stringToQName_missingLocalName()
    {
        final ImprovedTypeConverterInitialiser initialiser = new ImprovedTypeConverterInitialiser();
        initialiser.setStringToQNameEnabled(true);
        final NamespaceService prefixResolver = new DynamicNamespacePrefixResolver();
        prefixResolver.registerNamespace(NamespaceService.SYSTEM_MODEL_PREFIX, NamespaceService.SYSTEM_MODEL_1_0_URI);
        prefixResolver.registerNamespace(NamespaceService.CONTENT_MODEL_PREFIX, NamespaceService.CONTENT_MODEL_1_0_URI);
        initialiser.setNamespaceService(prefixResolver);
        initialiser.afterPropertiesSet();

        this.thrown.expect(InvalidQNameException.class);
        final String invalidQName = "{" + NamespaceService.CONTENT_MODEL_1_0_URI + "}";
        DefaultTypeConverter.INSTANCE.convert(QName.class, invalidQName);
        Assert.fail("Improved String to QName conversion should have failed");
    }

    @Test
    public void defaultStringToLocale()
    {
        final Locale defaultLocale = Locale.getDefault();

        final String language = "de";
        final Locale languageRes = DefaultTypeConverter.INSTANCE.convert(Locale.class, language);
        Assert.assertEquals("Default String to Locale conversion did not convert language code correctly", Locale.GERMAN, languageRes);

        final String languageAndCountry = "de_DE";
        final Locale languageAndCountryRes = DefaultTypeConverter.INSTANCE.convert(Locale.class, languageAndCountry);
        Assert.assertEquals("Default String to Locale conversion did not convert language + country code correctly", Locale.GERMANY,
                languageAndCountryRes);

        final String languageCountryAndVariant = "de_DE_test";
        final Locale languageCountryAndVariantRes = DefaultTypeConverter.INSTANCE.convert(Locale.class, languageCountryAndVariant);
        Assert.assertEquals("Default String to Locale conversion did not convert language + country + variant code correctly",
                new Locale("de", "DE", "test"), languageCountryAndVariantRes);

        final Locale languageCountryAndVariantRes2 = DefaultTypeConverter.INSTANCE.convert(Locale.class, languageCountryAndVariant);
        Assert.assertFalse("Default String to Locale conversion did re-use same Locale instance as result of repeated conversion",
                languageCountryAndVariantRes == languageCountryAndVariantRes2);

        final String tooManyTokenInput = "de_DE_test_oneTooMany";
        final Locale tooManyTokenInputRes = DefaultTypeConverter.INSTANCE.convert(Locale.class, tooManyTokenInput);
        Assert.assertEquals("Default String to Locale conversion did not return default Locale for invalid input", defaultLocale,
                tooManyTokenInputRes);
    }

    @Test
    public void stringToLocale()
    {
        final ImprovedTypeConverterInitialiser initialiser = new ImprovedTypeConverterInitialiser();
        initialiser.setStringToLocaleEnabled(true);
        initialiser.afterPropertiesSet();

        final Locale defaultLocale = Locale.getDefault();

        final String language = "de";
        final Locale languageRes = DefaultTypeConverter.INSTANCE.convert(Locale.class, language);
        Assert.assertEquals("Improved String to Locale conversion did not convert language code correctly", Locale.GERMAN, languageRes);

        final String languageAndCountry = "de_DE";
        final Locale languageAndCountryRes = DefaultTypeConverter.INSTANCE.convert(Locale.class, languageAndCountry);
        Assert.assertEquals("Improved String to Locale conversion did not convert language + country code correctly", Locale.GERMANY,
                languageAndCountryRes);

        final String languageCountryAndVariant = "de_DE_test";
        final Locale languageCountryAndVariantRes = DefaultTypeConverter.INSTANCE.convert(Locale.class, languageCountryAndVariant);
        Assert.assertEquals("Improved String to Locale conversion did not convert language + country + variant code correctly",
                new Locale("de", "DE", "test"), languageCountryAndVariantRes);

        final Locale languageCountryAndVariantRes2 = DefaultTypeConverter.INSTANCE.convert(Locale.class, languageCountryAndVariant);
        Assert.assertTrue("Improved String to Locale conversion did not re-use same Locale instance as result of repeated conversion",
                languageCountryAndVariantRes == languageCountryAndVariantRes2);

        final String tooManyTokenInput = "de_DE_test_oneTooMany";
        final Locale tooManyTokenInputRes = DefaultTypeConverter.INSTANCE.convert(Locale.class, tooManyTokenInput);
        Assert.assertEquals("Improved String to Locale conversion did not return default Locale for invalid input", defaultLocale,
                tooManyTokenInputRes);
    }

    @Test
    public void stringToLocale_performanceImprovementMicroBenchmark_fullLocale()
    {
        final Logger logger = LoggerFactory.getLogger(ImprovedTypeConverterTest.class);
        logger.info("Running String to Locale performance micro-benchmark for a full locale String input");
        final int conversionRuns = 500000;

        this.runStringToLocaleMicroBenchmark(logger, conversionRuns, "de_DE_test", "fr_FR_test", "de_AT_test", "en_GB_test", "zh_CN_test",
                "en_US_test", "de-CH-test", "nl-sa-test");
    }

    @Test
    public void stringToLocale_performanceImprovementMicroBenchmark_countryLocale()
    {
        final Logger logger = LoggerFactory.getLogger(ImprovedTypeConverterTest.class);
        logger.info("Running String to Locale performance micro-benchmark for a country locale String input");
        final int conversionRuns = 500000;

        this.runStringToLocaleMicroBenchmark(logger, conversionRuns, "de", "fr", "en", "it", "zh", "nl", "pr", "es");
    }

    private void runStringToLocaleMicroBenchmark(final Logger logger, final int conversionRuns, final String... constantInput)
    {
        final int inputValues = constantInput.length;

        // warmup
        for (int i = 0; i < conversionRuns; i++)
        {
            DefaultTypeConverter.INSTANCE.convert(Locale.class, constantInput[i % inputValues]);
        }

        long nanoStart = System.nanoTime();
        for (int i = 0; i < conversionRuns; i++)
        {
            DefaultTypeConverter.INSTANCE.convert(Locale.class, constantInput[i % inputValues]);
        }
        long nanoEnd = System.nanoTime();
        final long defaultTime = nanoEnd - nanoStart;

        final ImprovedTypeConverterInitialiser initialiser = new ImprovedTypeConverterInitialiser();
        initialiser.setStringToLocaleEnabled(true);
        initialiser.afterPropertiesSet();

        // warmup
        for (int i = 0; i < conversionRuns; i++)
        {
            DefaultTypeConverter.INSTANCE.convert(Locale.class, constantInput[i % inputValues]);
        }

        nanoStart = System.nanoTime();
        for (int i = 0; i < conversionRuns; i++)
        {
            DefaultTypeConverter.INSTANCE.convert(Locale.class, constantInput[i % inputValues]);
        }
        nanoEnd = System.nanoTime();
        final long improvedTime = nanoEnd - nanoStart;

        logger.info("String to Locale performance micro-benchmark shows {} ns for default conversion and {} ns for improved conversion",
                defaultTime, improvedTime);

        Assert.assertTrue("Improved String to Locale conversion did not provide a performance improvement", improvedTime < defaultTime);

        final long delta = defaultTime - improvedTime;
        final double percentImprovement = delta * 100.0 / defaultTime;
        logger.info(
                "String to Locale performance micro-benchmark shows {} % reduction of runtime over default conversion for {} consecutive conversions spread across {} input values",
                percentImprovement, conversionRuns, inputValues);
        Assert.assertTrue("Improved String to Locale conversion did not provide a 10% reduction of runtime", percentImprovement >= 10);
        Assert.assertTrue("Improved String to Locale conversion did not provide a 20% reduction of runtime", percentImprovement >= 20);
        Assert.assertTrue("Improved String to Locale conversion did not provide a 30% reduction of runtime", percentImprovement >= 30);
    }
}
