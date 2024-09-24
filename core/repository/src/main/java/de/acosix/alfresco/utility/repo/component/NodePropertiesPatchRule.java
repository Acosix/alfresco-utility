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

import java.io.Serializable;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;

/**
 * Instances of this interface encapsulate a simple node properties patch rule to apply to a node being patched by
 * {@link NodesPatchModuleComponent}.
 *
 * @author Axel Faust
 */
public interface NodePropertiesPatchRule
{

    /**
     * Applies this rule on the specified node. This operation should not itself perform any modification on the node in question and will
     * be run outside of any special context the {@link NodesPatchModuleComponent} may create to avoid inadvertent triggering of behaviours.
     *
     * @param node
     *            the node on which to apply the patch rule
     * @param currentProperties
     *            the (unmodifiable) map of properties of the node before the application of the rule (and any other properties patch rules)
     * @return the property updates to apply as part of an aggregated call to {@link NodeService#addProperties(NodeRef, Map) addProperties}
     *         - any mapping of a property QName to {@code null} will result in that property to be removed from the node
     */
    Map<QName, Serializable> apply(NodeRef node, Map<QName, Serializable> currentProperties);
}
