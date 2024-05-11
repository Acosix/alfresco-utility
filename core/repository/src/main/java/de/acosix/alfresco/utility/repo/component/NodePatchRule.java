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

import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Instances of this interface encapsulate a complex patch rule to apply to a node being patched by {@link NodesPatchModuleComponent}.
 *
 * @author Axel Faust
 */
public interface NodePatchRule
{

    /**
     * Applies this rule on the specified node.
     *
     * @param node
     *            the node on which to apply the patch rule
     */
    void apply(NodeRef node);
}
