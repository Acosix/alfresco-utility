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
package de.acosix.alfresco.utility.repo.web.scripts;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.alfresco.repo.web.scripts.ExtensibilityContainer;
import org.alfresco.repo.web.scripts.RepositoryContainer;
import org.alfresco.repo.web.scripts.TenantRepositoryContainer;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.config.ConfigImpl;
import org.springframework.extensions.config.ConfigSection;
import org.springframework.extensions.config.ConfigService;
import org.springframework.extensions.config.evaluator.Evaluator;
import org.springframework.extensions.config.xml.XMLConfigService;
import org.springframework.extensions.config.xml.elementreader.ConfigElementReader;
import org.springframework.extensions.surf.extensibility.BasicExtensionModule;
import org.springframework.extensions.surf.extensibility.ExtensibilityModel;
import org.springframework.extensions.surf.extensibility.HandlesExtensibility;
import org.springframework.extensions.surf.extensibility.WebScriptExtensibilityModuleHandler;
import org.springframework.extensions.surf.extensibility.impl.ExtensibilityModelImpl;
import org.springframework.extensions.surf.extensibility.impl.MarkupDirective;
import org.springframework.extensions.webscripts.Authenticator;
import org.springframework.extensions.webscripts.ExtendedScriptConfigModel;
import org.springframework.extensions.webscripts.ExtendedTemplateConfigModel;
import org.springframework.extensions.webscripts.ScriptConfigModel;
import org.springframework.extensions.webscripts.TemplateConfigModel;
import org.springframework.extensions.webscripts.WebScriptPropertyResourceBundle;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

/**
 * This class is basically a copy of the {@link ExtensibilityContainer} capable of handling
 * <a href="https://www.alfresco.com/blogs/developer/2012/05/23/webscript-extensibility-on-the-alfresco-repository/">Repository-tier web
 * script extensibility</a> to use {@link TenantRepositoryContainer} as the base which has become the default container with Alfresco .
 *
 * An improvement request has been filed with Alfresco as <a href="https://issues.alfresco.com/jira/browse/ALF-21794">ALF-21794</a>
 *
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
public class TenantExtensibilityContainer extends TenantRepositoryContainer implements HandlesExtensibility
{

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantExtensibilityContainer.class);

    /**
     *
     * This keeps track of whether or not the {@link ExtensibilityModel} for the current thread has been used. The
     * thread local value will only be set to <code>true</code> if the <code>getCurrentExtensibilityModel</code> method
     * is called.
     *
     */
    protected final ThreadLocal<Boolean> modelUsed = new ThreadLocal<>();

    /**
     *
     * Maintains a list of all the {@link ExtensibilityModel} instances being used across all the
     * available threads.
     *
     */
    protected final ThreadLocal<ExtensibilityModel> extensibilityModel = new ThreadLocal<>();

    /**
     *
     * A thread-safe cache of extended {@link ResourceBundle} instances for the current request.
     *
     */
    protected final ThreadLocal<Map<String, WebScriptPropertyResourceBundle>> extendedBundleCache = new ThreadLocal<>();

    /**
     *
     * A {@link ThreadLocal} reference to the file currently being processed in the model.
     */
    protected final ThreadLocal<String> fileBeingProcessed = new ThreadLocal<>();

    /**
     *
     * The list of {@link org.springframework.extensions.surf.types.ExtensionModule} instances that have been evaluated as applicable to
     * this RequestContext. This is set to <code>null</code> when during instantiation and is only
     * properly set the first time the <code>getEvaluatedModules</code> method is invoked. This ensures
     * that module evaluation only occurs once per request.
     *
     */
    protected final ThreadLocal<List<BasicExtensionModule>> evaluatedModules = new ThreadLocal<>();

    /**
     *
     * This is a local {@link ConfigImpl} instance that will only be used when extension modules are employed. It will
     * initially be populated with the default "static" global configuration taken from the {@link ConfigService} associated
     * with this {@link org.springframework.extensions.surf.RequestContext} but then updated to include global configuration provided by
     * extension modules that
     * have been evaluated to be applied to the current request.
     *
     */
    protected ThreadLocal<ConfigImpl> globalConfig = new ThreadLocal<>();

    /**
     *
     * This map represents {@link ConfigSection} instances mapped by area. It will only be used when extension modules are
     * employed. It will initially be populated with the default "static" configuration taken from the {@link ConfigService} associated
     * with this {@link org.springframework.extensions.surf.RequestContext} but then updated to include configuration provided by extension
     * modules that have been evaluated
     * to be applied to the current request.
     *
     */
    protected ThreadLocal<Map<String, List<ConfigSection>>> sectionsByArea = new ThreadLocal<>();

    /**
     *
     * A list of {@link ConfigSection} instances that are only applicable to the current request. It will only be used when extension
     * modules are
     * employed. It will initially be populated with the default "static" configuration taken from the {@link ConfigService} associated
     * with this {@link org.springframework.extensions.surf.RequestContext} but then updated to include configuration provided by extension
     * modules that have been evaluated
     * to be applied to the current request.
     *
     */
    protected ThreadLocal<List<ConfigSection>> sections = new ThreadLocal<>();

    /**
     *
     * A {@link WebScriptExtensibilityModuleHandler} is required for retrieving information on what
     * {@link BasicExtensionModule} instances have been configured and the extension files that need
     * to be processed. This variable should be set thorugh the Spring application context configuration.
     *
     */
    protected WebScriptExtensibilityModuleHandler extensibilityModuleHandler = null;

    /**
     *
     * Sets the {@link WebScriptExtensibilityModuleHandler} for this {@link org.springframework.extensions.webscripts.Container}.
     *
     *
     * @param extensibilityModuleHandler
     *            WebScriptExtensibilityModuleHandler
     */
    public void setExtensibilityModuleHandler(final WebScriptExtensibilityModuleHandler extensibilityModuleHandler)
    {
        this.extensibilityModuleHandler = extensibilityModuleHandler;
    }

    /**
     *
     * Returns the path of the file currently being processed in the model by the current thread.
     * This information is primarily provided for the purposes of generating debug information.
     *
     *
     * @return The path of the file currently being processed.
     */
    @Override
    public String getFileBeingProcessed()
    {
        return this.fileBeingProcessed.get();
    }

    /**
     *
     * Sets the path of the file currently being processed in the model by the current thread.
     * This information should be collected to assist with providing debug information.
     *
     *
     * @param file
     *            The path of the file currently being processed.
     */
    @Override
    public void setFileBeingProcessed(final String file)
    {
        this.fileBeingProcessed.set(file);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isExtensibilitySuppressed()
    {
        return false;
    }

    /**
     *
     * Opens a new {@link ExtensibilityModel}, defers execution to the extended {@link RepositoryContainer} and
     * then closes the {@link ExtensibilityModel}.
     *
     */
    @Override
    public void executeScript(final WebScriptRequest scriptReq, final WebScriptResponse scriptRes, final Authenticator auth)
            throws IOException
    {
        final ExtensibilityModel extModel = this.openExtensibilityModel();
        try
        {
            super.executeScript(scriptReq, scriptRes, auth);
        }
        finally
        {
            // It's only necessary to close the model if it's actually been used. Not all WebScripts will make use of the
            // model. An example of this would be the StreamContent WebScript. It is important not to attempt to close
            // an unused model since the WebScript executed may have already flushed the response if it has overridden
            // the default .execute() method.
            if (this.modelUsed.get())
            {
                try
                {
                    this.closeExtensibilityModel(extModel, scriptRes.getWriter());
                }
                catch (final IOException e)
                {
                    LOGGER.error("An error occurred getting the Writer when closing an ExtensibilityModel", e);
                }
            }
        }
    }

    /**
     *
     * Creates a new {@link ExtensibilityModel} and sets it on the current thread
     */
    @Override
    public ExtensibilityModel openExtensibilityModel()
    {
        LOGGER.debug("Opening for thread: {}", Thread.currentThread().getName());
        this.extendedBundleCache.set(new HashMap<String, WebScriptPropertyResourceBundle>());

        final ExtensibilityModel model = new ExtensibilityModelImpl(null, this);
        this.extensibilityModel.set(model);
        this.modelUsed.set(Boolean.FALSE);

        return model;
    }

    /**
     * Flushes the {@link ExtensibilityModel} provided and sets its parent as the current {@link ExtensibilityModel}
     * for the current thread.
     */
    @Override
    public void closeExtensibilityModel(final ExtensibilityModel model, final Writer out)
    {
        LOGGER.debug("Closing for thread: {}", Thread.currentThread().getName());
        model.flushModel(out);

        this.extendedBundleCache.remove();
        this.evaluatedModules.remove();
        this.fileBeingProcessed.remove();
        this.globalConfig.remove();
        this.sections.remove();
        this.sectionsByArea.remove();

        this.extensibilityModel.remove();
        this.modelUsed.set(Boolean.FALSE);
    }

    /**
     * Returns the {@link ExtensibilityModel} for the current thread.
     */
    @Override
    public ExtensibilityModel getCurrentExtensibilityModel()
    {
        LOGGER.debug("Getting current for thread: {}", Thread.currentThread().getName());
        this.modelUsed.set(Boolean.TRUE);
        return this.extensibilityModel.get();
    }

    /**
     * This method is implemented to perform no action as it is not necessary for a standalone WebScript
     * container to add dependencies for processing.
     */
    @Override
    public void updateExtendingModuleDependencies(final String pathBeingProcessed, final Map<String, Object> model)
    {
        // NOT REQUIRED FOR STANDALONE WEBSCRIPT CONTAINER
    }

    /**
     * Checks the cache to see if it has cached an extended bundle (that is a basic {@link ResourceBundle} that
     * has had extension modules applied to it. Extended bundles can only be safely cached once per request as the modules
     * applied can vary for each request.
     *
     * @param webScriptId
     *            The id of the WebScript to retrieve the extended bundle for.
     * @return A cached bundle or <code>null</code> if the bundle has not previously been cached.
     */
    @Override
    public ResourceBundle getCachedExtendedBundle(final String webScriptId)
    {
        ResourceBundle cachedExtendedBundle = null;
        final Map<String, WebScriptPropertyResourceBundle> threadLocal = this.extendedBundleCache.get();
        if (threadLocal != null)
        {
            cachedExtendedBundle = this.extendedBundleCache.get().get(webScriptId);
        }
        return cachedExtendedBundle;
    }

    /**
     * Adds a new extended bundle to the cache. An extended bundle is a WebScript {@link ResourceBundle} that has had
     * {@link ResourceBundle} instances merged into it from extension modules that have been applied. These can only be cached
     * for the lifetime of the request as different modules may be applied to the same WebScript for different requests.
     *
     * @param webScriptId
     *            The id of the WebScript to cache the extended bundle against.
     * @param extensionBundle
     *            The extended bundle to cache.
     */
    @Override
    public void addExtensionBundleToCache(final String webScriptId, final WebScriptPropertyResourceBundle extensionBundle)
    {
        Map<String, WebScriptPropertyResourceBundle> threadLocal = this.extendedBundleCache.get();
        if (threadLocal == null)
        {
            // This should never be the case because when a new model is opened this value should be reset
            // but we will double-check to avoid the potential of NPEs...
            threadLocal = new HashMap<>();
            this.extendedBundleCache.set(threadLocal);
        }
        threadLocal.put(webScriptId, extensionBundle);
    }

    /**
     *
     * Retrieves an files for the evaluated modules that are extending the WebScript files being processed.
     *
     */
    @Override
    public List<String> getExtendingModuleFiles(final String pathBeingProcessed)
    {
        final List<String> extendingModuleFiles = new ArrayList<>();
        for (final BasicExtensionModule module : this.getEvaluatedModules())
        {
            extendingModuleFiles.addAll(this.extensibilityModuleHandler.getExtendingModuleFiles(module, pathBeingProcessed));
        }
        return extendingModuleFiles;
    }

    /**
     * Retrieve the list of {@link org.springframework.extensions.surf.types.ExtensionModule} instances that have been evaluated as
     * applicable
     * for the current request. If this list has not yet been populated then use the
     * {@link org.springframework.extensions.surf.extensibility.ExtensibilityModuleHandler}
     * configured in the Spring application context to evaluate them.
     *
     * @return A list of {@link org.springframework.extensions.surf.types.ExtensionModule} instances that are applicable to the current
     *         request.
     */
    public List<BasicExtensionModule> getEvaluatedModules()
    {
        List<BasicExtensionModule> evaluatedModules = this.evaluatedModules.get();
        if (evaluatedModules == null)
        {
            if (this.extensibilityModuleHandler == null)
            {
                if (LOGGER.isErrorEnabled())
                {
                    LOGGER.error(
                            "No 'extensibilityModuleHandler' has been configured for this request context. Extensions cannot be processed");
                }
                evaluatedModules = new ArrayList<>();
            }
            else
            {
                evaluatedModules = this.extensibilityModuleHandler.getExtensionModules();
            }
            this.evaluatedModules.set(evaluatedModules);
        }
        return evaluatedModules;
    }

    /**
     * Creates a new {@link ExtendedScriptConfigModel} instance using the local configuration generated for this request.
     * If configuration for the request will be generated if it does not yet exist. It is likely that this method will be
     * called multiple times within the context of a single request and although the configuration containers will always
     * be the same a new {@link ExtendedScriptConfigModel} instance will always be created as the the supplied <code>xmlConfig</code>
     * string could be different for each call (because each WebScript invoked in the request will supply different
     * configuration.
     */
    @Override
    public ScriptConfigModel getExtendedScriptConfigModel(final String xmlConfig)
    {
        if (this.globalConfig.get() == null && this.sectionsByArea.get() == null && this.sections.get() == null)
        {
            this.getConfigExtensions();
        }
        return new ExtendedScriptConfigModel(this.getConfigService(), xmlConfig, this.globalConfig.get(), this.sectionsByArea.get(),
                this.sections.get());
    }

    /**
     * Creates a new {@link TemplateConfigModel} instance using the local configuration generated for this request.
     * If configuration for the request will be generated if it does not yet exist. It is likely that this method will be
     * called multiple times within the context of a single request and although the configuration containers will always
     * be the same a new {@link TemplateConfigModel} instance will always be created as the the supplied <code>xmlConfig</code>
     * string could be different for each call (because each WebScript invoked in the request will supply different
     * configuration.
     */
    @Override
    public TemplateConfigModel getExtendedTemplateConfigModel(final String xmlConfig)
    {
        if (this.globalConfig.get() == null && this.sectionsByArea.get() == null && this.sections.get() == null)
        {
            this.getConfigExtensions();
        }
        return new ExtendedTemplateConfigModel(this.getConfigService(), xmlConfig, this.globalConfig.get(), this.sectionsByArea.get(),
                this.sections.get());
    }

    /**
     * Adds the <code>&lt;@markup&gt;</code> directive to the container which allows FreeMarker templates to be extended.
     */
    @Override
    public void addExtensibilityDirectives(final Map<String, Object> freeMarkerModel, final ExtensibilityModel extModel)
    {
        final MarkupDirective mud = new MarkupDirective("markup", extModel);
        freeMarkerModel.put("markup", mud);
    }

    /**
     * Creates and populates the request specific configuration container objects (<code>globalConfig</code>, <code>sectionsByArea</code>
     * and
     * <code>sections</code> with a combination of the default static configuration (taken from files accessed by the {@link ConfigService})
     * and
     * dynamic configuration taken from extension modules evaluated for the current request.
     */
    protected void getConfigExtensions()
    {
        // Extended configuration is only possible if config service is an XMLConfigService...
        // ...also, it's only necessary to populate the configuration containers if they have not already been populated. This test should
        // also be carried out by the two methods ("getExtendedTemplateConfigModel" & "getExtendedTemplateConfigModel") to prevent
        // duplication of effort... but in case other methods attempt to access it we will make these additional tests.
        if (this.getConfigService() instanceof XMLConfigService && this.globalConfig.get() == null && this.sectionsByArea.get() == null
                && this.sections.get() == null)
        {
            // Cast the config service for ease of access
            final XMLConfigService xmlConfigService = (XMLConfigService) this.getConfigService();

            // Get the current configuration from the ConfigService - we don't want to permanently pollute
            // the standard configuration with additions from the modules...
            this.globalConfig.set(new ConfigImpl((ConfigImpl) xmlConfigService.getGlobalConfig())); // Make a copy of the current global
                                                                                                    // config

            // Initialise these with the config service values...
            this.sectionsByArea.set(new HashMap<>(xmlConfigService.getSectionsByArea()));
            this.sections.set(new ArrayList<>(xmlConfigService.getSections()));

            // Check to see if there are any modules that we need to apply...
            final List<BasicExtensionModule> evaluatedModules = this.getEvaluatedModules();
            if (evaluatedModules != null && !evaluatedModules.isEmpty())
            {
                for (final BasicExtensionModule currModule : evaluatedModules)
                {
                    for (final Element currentConfigElement : currModule.getConfigurations())
                    {
                        // Set up containers for our request specific configuration - this will contain data taken from the evaluated
                        // modules...
                        final Map<String, ConfigElementReader> parsedElementReaders = new HashMap<>();
                        final Map<String, Evaluator> parsedEvaluators = new HashMap<>();
                        final List<ConfigSection> parsedConfigSections = new ArrayList<>();

                        // Parse and process the parses configuration...
                        final String currentArea = xmlConfigService.parseFragment(currentConfigElement, parsedElementReaders,
                                parsedEvaluators, parsedConfigSections);
                        for (final Map.Entry<String, Evaluator> entry : parsedEvaluators.entrySet())
                        {
                            // add the evaluators to the config service
                            parsedEvaluators.put(entry.getKey(), entry.getValue());
                        }

                        for (final Map.Entry<String, ConfigElementReader> entry : parsedElementReaders.entrySet())
                        {
                            // add the element readers to the config service
                            parsedElementReaders.put(entry.getKey(), entry.getValue());
                        }

                        for (final ConfigSection section : parsedConfigSections)
                        {
                            // Update local configuration with our updated data...
                            xmlConfigService.addConfigSection(section, currentArea, this.globalConfig.get(), this.sectionsByArea.get(),
                                    this.sections.get());
                        }
                    }
                }
            }
        }
    }
}
