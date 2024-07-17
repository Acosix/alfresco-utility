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
package de.acosix.alfresco.utility.repo.email.imap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.util.PropertyCheck;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.util.PropertyPlaceholderHelper;

/**
 * Instances of this class generate job definition and trigger beans for IMAP synchronisation.
 *
 * @author Axel Faust
 */
public class SynchJobBeanRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor, InitializingBean
{

    protected String propertyPrefix;

    protected Properties propertiesSource;

    protected String placeholderPrefix = PlaceholderConfigurerSupport.DEFAULT_PLACEHOLDER_PREFIX;

    protected String placeholderSuffix = PlaceholderConfigurerSupport.DEFAULT_PLACEHOLDER_SUFFIX;

    protected String valueSeparator = PlaceholderConfigurerSupport.DEFAULT_VALUE_SEPARATOR;

    protected PropertyPlaceholderHelper placeholderHelper;

    protected String accessorBeanName;

    protected String baseTriggerBeanName;

    protected String baseDetailBeanName;

    /**
     * @param propertyPrefix
     *     the propertyPrefix to set
     */
    public void setPropertyPrefix(final String propertyPrefix)
    {
        this.propertyPrefix = propertyPrefix;
    }

    /**
     * Sets the properties collection to use when emitting bean definitions.
     *
     * @param propertiesSource
     *     the propertiesSource to set
     */
    public void setPropertiesSource(final Properties propertiesSource)
    {
        this.propertiesSource = propertiesSource;
    }

    /**
     * @param placeholderPrefix
     *     the placeholderPrefix to set
     */
    public void setPlaceholderPrefix(final String placeholderPrefix)
    {
        this.placeholderPrefix = placeholderPrefix;
    }

    /**
     * @param placeholderSuffix
     *     the placeholderSuffix to set
     */
    public void setPlaceholderSuffix(final String placeholderSuffix)
    {
        this.placeholderSuffix = placeholderSuffix;
    }

    /**
     * @param valueSeparator
     *     the valueSeparator to set
     */
    public void setValueSeparator(final String valueSeparator)
    {
        this.valueSeparator = valueSeparator;
    }

    /**
     * @param accessorBeanName
     *     the accessorBeanName to set
     */
    public void setAccessorBeanName(final String accessorBeanName)
    {
        this.accessorBeanName = accessorBeanName;
    }

    /**
     * @param baseTriggerBeanName
     *     the baseTriggerBeanName to set
     */
    public void setBaseTriggerBeanName(final String baseTriggerBeanName)
    {
        this.baseTriggerBeanName = baseTriggerBeanName;
    }

    /**
     * @param baseDetailBeanName
     *     the baseDetailBeanName to set
     */
    public void setBaseDetailBeanName(final String baseDetailBeanName)
    {
        this.baseDetailBeanName = baseDetailBeanName;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "propertiesSource", this.propertiesSource);
        PropertyCheck.mandatory(this, "propertyPrefix", this.propertyPrefix);
        PropertyCheck.mandatory(this, "accessorBeanName", this.accessorBeanName);
        PropertyCheck.mandatory(this, "baseTriggerBeanName", this.baseTriggerBeanName);
        PropertyCheck.mandatory(this, "baseDetailBeanName", this.baseDetailBeanName);

        this.placeholderHelper = new PropertyPlaceholderHelper(this.placeholderPrefix, this.placeholderSuffix, this.valueSeparator, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) throws BeansException
    {
        // NO-OP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postProcessBeanDefinitionRegistry(final BeanDefinitionRegistry registry) throws BeansException
    {
        final String enabledStr = this.getResolvedProperty(".job.enabled", "false", false);
        if (Boolean.parseBoolean(enabledStr))
        {
            final ManagedList<RuntimeBeanReference> triggerReferences = new ManagedList<>();

            final String configNames = this.getResolvedProperty(".configNames", "", false);
            final String[] configNamesArr = configNames.split(",");
            for (final String configName : configNamesArr)
            {
                if (!configName.trim().isEmpty())
                {
                    final String triggerBeanName = this.setupJob(registry, configName);
                    triggerReferences.add(new RuntimeBeanReference(triggerBeanName));
                }
            }

            final BeanDefinition accessorDef = registry.getBeanDefinition(this.accessorBeanName);
            accessorDef.getPropertyValues().addPropertyValue("triggersGen", triggerReferences);
        }
    }

    protected String setupJob(final BeanDefinitionRegistry registry, final String configName)
    {
        final String beanNamePrefix = this.propertyPrefix + "." + configName;
        final String detailBeanName = beanNamePrefix + ".jobDetail";

        final Config config = this.readImapConfig(configName);

        final GenericBeanDefinition detailDef = new GenericBeanDefinition();
        detailDef.setParentName(this.baseDetailBeanName);
        final ManagedMap<Object, Object> managedMap = new ManagedMap<>();
        managedMap.setMergeEnabled(true);
        managedMap.put("imapConfig", config);
        managedMap.put("configName", configName);
        detailDef.getPropertyValues().addPropertyValue("jobDataAsMap", managedMap);
        registry.registerBeanDefinition(detailBeanName, detailDef);

        final GenericBeanDefinition triggerDef = new GenericBeanDefinition();
        triggerDef.setParentName(this.baseTriggerBeanName);
        final String cron = this.getResolvedProperty(this.propertyPrefix, ".config." + configName + ".cron", "0 0 * * * ? 2099", false);
        triggerDef.getPropertyValues().addPropertyValue("cronExpression", cron);
        triggerDef.getPropertyValues().addPropertyValue("jobDetail", new RuntimeBeanReference(detailBeanName));

        final String triggerBeanName = beanNamePrefix + ".jobTrigger";
        registry.registerBeanDefinition(triggerBeanName, triggerDef);

        return triggerBeanName;
    }

    protected Config readImapConfig(final String configName)
    {
        final String prefix = this.propertyPrefix + ".config." + configName;

        final Config config = new Config();

        config.setProtocol(this.getResolvedProperty(prefix, ".protocol", "imap", true));
        config.setHost(this.getResolvedProperty(prefix, ".host", null, true));
        config.setPort(Integer.parseInt(this.getResolvedProperty(prefix, ".port", "143", true)));

        config.setUser(this.getResolvedProperty(prefix, ".user", null, true));
        // optional in case of XOAUTH2
        config.setPassword(this.getResolvedProperty(prefix, ".password", null, false));
        config.setAuthMechanisms(this.getResolvedProperty(prefix, ".auth.mechanisms", null, false));
        config.setSaslMechanisms(this.getResolvedProperty(prefix, ".sasl.mechanisms", null, false));
        config.setSaslAuthorizationId(this.getResolvedProperty(prefix, ".sasl.authorizationId", null, false));
        config.setSaslRealm(this.getResolvedProperty(prefix, ".sasl.realm", null, false));

        config.setOauthTokenUrl(this.getResolvedProperty(prefix, ".oauth.url", null, false));
        config.setOauthClientId(this.getResolvedProperty(prefix, ".oauth.client", null, false));
        config.setOauthClientSecret(this.getResolvedProperty(prefix, ".oauth.secret", null, false));
        config.setOauthScope(this.getResolvedProperty(prefix, ".oauth.scope", null, false));

        config.setStartTlsEnabled(Boolean.parseBoolean(this.getResolvedProperty(prefix, ".starttls.enabled", "true", false)));
        config.setStartTlsRequired(Boolean.parseBoolean(this.getResolvedProperty(prefix, ".starttls.required", "false", false)));
        config.setConnectionTimeout(Integer.parseInt(this.getResolvedProperty(prefix, ".connectionTimeout", "10000", false)));
        config.setReadTimeout(Integer.parseInt(this.getResolvedProperty(prefix, ".readTimeout", "10000", false)));
        config.setWriteTimeout(Integer.parseInt(this.getResolvedProperty(prefix, ".writeTimeout", "10000", false)));
        config.setCompressionEnabled(Boolean.parseBoolean(this.getResolvedProperty(prefix, ".compression.enabled", "true", false)));
        config.setCompressionLevel(Integer.parseInt(this.getResolvedProperty(prefix, ".compression.level", "5", false)));
        config.setCompressionStrategy(Integer.parseInt(this.getResolvedProperty(prefix, ".compression.strategy", "0", false)));

        config.setDefaultFromOverride(this.getResolvedProperty(prefix, ".default.from", null, false));
        config.setDefaultToOverride(this.getResolvedProperty(prefix, ".default.to", null, false));

        config.setDebug(Boolean.parseBoolean(this.getResolvedProperty(prefix, ".debug", "false", false)));

        config.setProcessFilterByFlagEnabled(Boolean.parseBoolean(this.getResolvedProperty(prefix, ".filter.flag.enabled", "true", false)));
        config.setProcessFilterByFlagBits(Integer.parseInt(this.getResolvedProperty(prefix, ".filter.flag.bits.set", "0", false)));
        config.setProcessFilterByUnsetFlagBits(Integer.parseInt(this.getResolvedProperty(prefix, ".filter.flag.bits.unset", "8", false)));
        config.setProcessFilterByFlagName(this.getResolvedProperty(prefix, ".filter.flag.name.set", null, false));
        config.setProcessFilterByUnsetFlagName(this.getResolvedProperty(prefix, ".filter.flag.name.unset", null, false));

        config.setFlagProcessedEnabled(Boolean.parseBoolean(this.getResolvedProperty(prefix, ".processed.flag.enabled", "true", false)));
        config.setFlagProcessedWithBits(Integer.parseInt(this.getResolvedProperty(prefix, ".processed.flag.bits.set", "8", false)));
        config.setFlagProcessedWithUnsetBits(Integer.parseInt(this.getResolvedProperty(prefix, ".processed.flag.bits.unset", "0", false)));
        config.setFlagProcessedWithName(this.getResolvedProperty(prefix, ".processed.flag.name.set", null, false));
        config.setFlagProcessedWithUnsetName(this.getResolvedProperty(prefix, ".processed.flag.name.unset", null, false));

        config.setFlagRejectedEnabled(Boolean.parseBoolean(this.getResolvedProperty(prefix, ".rejected.flag.enabled", "false", false)));
        config.setFlagRejectedWithBits(Integer.parseInt(this.getResolvedProperty(prefix, ".rejected.flag.bits.set", "0", false)));
        config.setFlagRejectedWithUnsetBits(Integer.parseInt(this.getResolvedProperty(prefix, ".rejected.flag.bits.unset", "0", false)));
        config.setFlagRejectedWithName(this.getResolvedProperty(prefix, ".rejected.flag.name.set", null, false));
        config.setFlagRejectedWithUnsetName(this.getResolvedProperty(prefix, ".rejected.flag.name.unset", null, false));

        this.readFoldersConfig(prefix, config);

        return config;
    }

    private void readFoldersConfig(final String prefix, final Config config)
    {
        final String folders = this.getResolvedProperty(prefix, ".process.folders", null, false);
        if (folders != null && !folders.trim().isEmpty())
        {
            final String[] foldersArr = folders.split(",");
            final List<String> nonEmptyFolderNames = Arrays.asList(foldersArr).stream().map(String::trim).filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            if (nonEmptyFolderNames.isEmpty())
            {
                throw new AlfrescoRuntimeException("No folders to process configured");
            }

            config.setFolders(nonEmptyFolderNames);

            final Map<String, String> pathByFolder = new HashMap<>();
            final Map<String, String> toOverrideByFolder = new HashMap<>();
            final Map<String, String> fromOverrideByFolder = new HashMap<>();
            final Map<String, String> moveProcessedTargetByFolder = new HashMap<>();
            final Map<String, String> moveRejectedTargetByFolder = new HashMap<>();

            for (final String folder : nonEmptyFolderNames)
            {
                final String folderPrefix = prefix + ".process.folder." + folder;

                final String path = this.getResolvedProperty(folderPrefix, ".path", null, false);
                if (path == null || path.trim().isEmpty())
                {
                    throw new AlfrescoRuntimeException("Missing path for folder " + folder);
                }
                pathByFolder.put(folder, path);

                final String from = this.getResolvedProperty(folderPrefix, ".from", null, false);
                if (from != null)
                {
                    fromOverrideByFolder.put(folder, from);
                }

                final String to = this.getResolvedProperty(folderPrefix, ".to", null, false);
                if (to != null)
                {
                    toOverrideByFolder.put(folder, to);
                }

                final String moveProcessedTarget = this.getResolvedProperty(folderPrefix, ".move.processed", null, false);
                if (moveProcessedTarget != null)
                {
                    moveProcessedTargetByFolder.put(folder, moveProcessedTarget);
                }

                final String moveRejectedTarget = this.getResolvedProperty(folderPrefix, ".move.rejected", null, false);
                if (moveRejectedTarget != null)
                {
                    moveRejectedTargetByFolder.put(folder, moveRejectedTarget);
                }
            }

            config.setPathByFolder(pathByFolder);
            config.setFromOverrideByFolder(fromOverrideByFolder);
            config.setToOverrideByFolder(toOverrideByFolder);
            config.setMoveProcessedToPathByFolder(moveProcessedTargetByFolder);
            config.setMoveRejectedToPathByFolder(moveRejectedTargetByFolder);
        }
        else
        {
            throw new AlfrescoRuntimeException("No folders to process configured");
        }
    }

    protected String getResolvedProperty(final String key, final String defaultValue, final boolean mandatory)
    {
        return this.getResolvedProperty(this.propertyPrefix, key, defaultValue, mandatory);
    }

    protected String getResolvedProperty(final String prefix, final String key, final String defaultValue, final boolean mandatory)
    {
        String str = this.propertiesSource.getProperty(prefix + key, defaultValue);
        str = str != null ? this.placeholderHelper.replacePlaceholders(str, this.propertiesSource) : null;
        if (mandatory && (str == null || str.isEmpty()))
        {
            throw new AlfrescoRuntimeException(prefix + key + " is missing a configuration value");
        }
        return str;
    }
}
