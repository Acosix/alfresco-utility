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
package de.acosix.alfresco.utility.repo.component;

import java.util.Collection;
import java.util.Properties;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.repo.workflow.WorkflowDeployer;
import org.alfresco.repo.workflow.activiti.ActivitiConstants;
import org.alfresco.service.cmr.workflow.WorkflowAdminService;
import org.alfresco.service.cmr.workflow.WorkflowDeployment;
import org.alfresco.service.cmr.workflow.WorkflowException;
import org.alfresco.service.cmr.workflow.WorkflowService;
import org.alfresco.util.PropertyCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

/**
 * This class provides a module component to perform workflow deployments properly based on the version progression of the extension module
 * instead of being completely decoupled from it, like the default Alfresco {@link WorkflowDeployer workflow deployer}. This component is
 * limited to Activiti workflows as this is the only type of engine support by Alfresco out-of-the-box in any recent version and no other
 * engine is known to be available via community extension modules.
 *
 * @author Axel Faust
 */
public class WorkflowDeployerModuleComponent extends AbstractModuleComponent
{

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowDeployerModuleComponent.class);

    protected Collection<Properties> workflowDefinitions;

    protected WorkflowService workflowService;

    protected WorkflowAdminService workflowAdminService;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void checkProperties()
    {
        PropertyCheck.mandatory(this, "workflowDefinitions", this.workflowDefinitions);
        PropertyCheck.mandatory(this, "workflowService", this.workflowService);
        PropertyCheck.mandatory(this, "workflowAdminService", this.workflowAdminService);
        super.checkProperties();
    }

    /**
     * @param workflowDefinitions
     *     the workflowDefinitions to set
     */
    public void setWorkflowDefinitions(final Collection<Properties> workflowDefinitions)
    {
        this.workflowDefinitions = workflowDefinitions;
    }

    /**
     * @param workflowService
     *     the workflowService to set
     */
    public void setWorkflowService(final WorkflowService workflowService)
    {
        this.workflowService = workflowService;
    }

    /**
     * @param workflowAdminService
     *     the workflowAdminService to set
     */
    public void setWorkflowAdminService(final WorkflowAdminService workflowAdminService)
    {
        this.workflowAdminService = workflowAdminService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void executeInternal() throws Throwable
    {
        for (final Properties workflowDefinition : this.workflowDefinitions)
        {
            final String location = workflowDefinition.getProperty(WorkflowDeployer.LOCATION);
            if (location == null || location.length() == 0)
            {
                throw new WorkflowException("Workflow definition location must be provided");
            }

            if (this.workflowAdminService.isEngineEnabled(ActivitiConstants.ENGINE_ID))
            {
                final String mimetype = workflowDefinition.getProperty(WorkflowDeployer.MIMETYPE);
                final ClassPathResource workflowResource = new ClassPathResource(location);

                final WorkflowDeployment deployment = this.workflowService.deployDefinition(ActivitiConstants.ENGINE_ID,
                        workflowResource.getInputStream(), mimetype, workflowResource.getFilename());

                if (deployment.getDefinition() != null)
                {
                    LOGGER.debug("Definition {} (version {}) from {} deployed with {} problems", deployment.getDefinition().getTitle(),
                            deployment.getDefinition().getVersion(), location, Integer.valueOf(deployment.getProblems().length));
                }
                else
                {
                    LOGGER.debug("Definition {} (version {}) from {} deployed with {} problems", workflowResource.getFilename(), "N/A",
                            location, Integer.valueOf(deployment.getProblems().length));
                }
            }
            else
            {
                LOGGER.debug("Definition {} not deployed as the Activiti engine is disabled", location);
            }
        }
    }
}
