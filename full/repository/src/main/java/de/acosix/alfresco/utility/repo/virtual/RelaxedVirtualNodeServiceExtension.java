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
package de.acosix.alfresco.utility.repo.virtual;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.db.traitextender.NodeServiceTrait;
import org.alfresco.repo.virtual.ActualEnvironment;
import org.alfresco.repo.virtual.VirtualContentModel;
import org.alfresco.repo.virtual.VirtualizationException;
import org.alfresco.repo.virtual.bundle.VirtualNodeServiceExtension;
import org.alfresco.repo.virtual.ref.NodeProtocol;
import org.alfresco.repo.virtual.ref.Reference;
import org.alfresco.repo.virtual.store.VirtualStore;
import org.alfresco.repo.virtual.template.FilingData;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.PropertyCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Axel Faust
 */
public class RelaxedVirtualNodeServiceExtension extends VirtualNodeServiceExtension implements InitializingBean
{

    private static final Logger LOGGER = LoggerFactory.getLogger(RelaxedVirtualNodeServiceExtension.class);

    protected DictionaryService dictionaryService;

    // copied from base class due to visibility restrictions
    protected VirtualStore smartStore;

    protected ActualEnvironment environment;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "dictionaryService", this.dictionaryService);
        PropertyCheck.mandatory(this, "smartStore", this.smartStore);
        PropertyCheck.mandatory(this, "environment", this.environment);
    }

    /**
     * @param dictionaryService
     *            the dictionaryService to set
     */
    public void setDictionaryService(final DictionaryService dictionaryService)
    {
        this.dictionaryService = dictionaryService;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void setSmartStore(final VirtualStore smartStore)
    {
        super.setSmartStore(smartStore);
        this.smartStore = smartStore;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void setEnvironment(final ActualEnvironment environment)
    {
        super.setEnvironment(environment);
        this.environment = environment;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public ChildAssociationRef createNode(final NodeRef parentRef, final QName assocTypeQName, final QName assocQName,
            final QName nodeTypeQName, final Map<QName, Serializable> properties)
    {
        final Reference targetReference = Reference.fromNodeRef(parentRef);

        ChildAssociationRef result;
        if (targetReference != null && !this.smartStore.canMaterialize(targetReference))
        {
            LOGGER.debug("Creating node of type {} in virtual target {}", nodeTypeQName, targetReference);
            result = this.createNodeInVirtual(targetReference, assocTypeQName, assocQName, nodeTypeQName, properties);
        }
        else
        {
            final QName materialAssocQName = this.materializeAssocQName(assocQName);
            final NodeRef actualTarget = targetReference != null ? this.smartStore.materialize(targetReference) : parentRef;
            result = this.getTrait().createNode(actualTarget, assocTypeQName, materialAssocQName, nodeTypeQName, properties);
        }
        return result;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public ChildAssociationRef moveNode(final NodeRef nodeToMoveRef, final NodeRef newParentRef, final QName assocTypeQName,
            final QName assocQName)
    {
        final Reference nodeReference = Reference.fromNodeRef(nodeToMoveRef);
        final Reference targetReference = Reference.fromNodeRef(newParentRef);

        if (nodeReference != null && !this.smartStore.canMaterialize(nodeReference))
        {
            LOGGER.debug("Rejecting node move of virtual source {} into {}", nodeToMoveRef, newParentRef);
            throw new UnsupportedOperationException("Unsuported operation for virtual source");
        }

        ChildAssociationRef result;
        if (targetReference != null && !this.smartStore.canMaterialize(targetReference))
        {
            final NodeRef actualNodeToMove = nodeReference != null ? this.smartStore.materialize(nodeReference) : nodeToMoveRef;
            LOGGER.debug("Moving {} (actual ref {}) into virtual target {}", nodeToMoveRef, actualNodeToMove, newParentRef);
            result = this.moveNodeInVirtual(actualNodeToMove, targetReference, assocTypeQName, assocQName);
        }
        else
        {
            final NodeRef actualNodeToMove = nodeReference != null ? this.smartStore.materialize(nodeReference) : nodeToMoveRef;
            final NodeRef actualTarget = targetReference != null ? this.smartStore.materialize(targetReference) : newParentRef;
            result = this.getTrait().moveNode(actualNodeToMove, actualTarget, assocTypeQName, assocQName);
        }
        return result;
    }

    /**
     * Creates a new node in a virtual parent.
     *
     * @param parentReference
     *            the reference to the virtual parent
     * @param assocTypeQName
     *            the qualified name of the association type to use
     * @param assocQName
     *            the qualified name of the association
     * @param nodeTypeQName
     *            the qualified name of the type of node to create
     * @param properties
     *            the map of property values to set during creation
     * @return the reference to the new (virtualised) child association
     */
    protected ChildAssociationRef createNodeInVirtual(final Reference parentReference, final QName assocTypeQName, final QName assocQName,
            final QName nodeTypeQName, final Map<QName, Serializable> properties)
    {
        final NodeRef parentRef = parentReference.toNodeRef();

        try
        {
            final FilingData filingData = this.smartStore.createFilingData(parentReference, assocTypeQName, assocQName, nodeTypeQName,
                    properties);

            final NodeRef childParentNodeRef = filingData.getFilingNodeRef();

            if (childParentNodeRef != null)
            {
                final Map<QName, Serializable> filingDataProperties = filingData.getProperties();
                QName changedAssocQName = assocQName;
                if (filingDataProperties.containsKey(ContentModel.PROP_NAME))
                {
                    final String fileName = (String) filingDataProperties.get(ContentModel.PROP_NAME);
                    final String changedFileName = this.handleExistingFile(childParentNodeRef, fileName);
                    if (!changedFileName.equals(fileName))
                    {
                        filingDataProperties.put(ContentModel.PROP_NAME, changedFileName);
                        final QName filingDataAssocQName = filingData.getAssocQName();
                        changedAssocQName = QName.createQName(filingDataAssocQName.getNamespaceURI(),
                                QName.createValidLocalName(changedFileName));
                    }
                }

                final NodeServiceTrait theTrait = this.getTrait();

                QName filingNodeType = filingData.getNodeTypeQName();
                if (this.dictionaryService.isSubClass(filingNodeType, nodeTypeQName))
                {
                    filingNodeType = nodeTypeQName;
                }
                else
                {
                    LOGGER.debug("Rejecting node creation in {} since requested node type {} is incompatible with filing node type {}",
                            parentRef, nodeTypeQName, filingNodeType);
                    throw new InvalidNodeRefException("Can not create node of type in virtual parent with incompatible filing node type.",
                            parentRef);
                }

                final ChildAssociationRef actualChildAssocRef = theTrait.createNode(childParentNodeRef, filingData.getAssocTypeQName(),
                        changedAssocQName == null ? filingData.getAssocQName() : changedAssocQName, filingNodeType, filingDataProperties);

                final Reference nodeProtocolChildRef = NodeProtocol.newReference(actualChildAssocRef.getChildRef(), parentReference);
                final QName vChildAssocQName = QName.createQNameWithValidLocalName(VirtualContentModel.VIRTUAL_CONTENT_MODEL_1_0_URI,
                        actualChildAssocRef.getQName().getLocalName());
                final ChildAssociationRef childAssocRef = new ChildAssociationRef(actualChildAssocRef.getTypeQName(), parentRef,
                        vChildAssocQName, nodeProtocolChildRef.toNodeRef());

                // aspects may already have been auto-applied via createNode based on filing properties - this leaves only marker aspects
                final Set<QName> aspects = filingData.getAspects();
                aspects.stream().filter(aspect -> !theTrait.hasAspect(actualChildAssocRef.getChildRef(), aspect)).forEach(aspect -> {
                    theTrait.addAspect(actualChildAssocRef.getChildRef(), aspect, Collections.emptyMap());
                });

                return childAssocRef;
            }
            else
            {
                LOGGER.debug("Rejecting node creation in {} due to lack of filing target", parentRef);
                throw new InvalidNodeRefException("Can not create node in virtual parent without filing context.", parentRef);
            }
        }
        catch (final VirtualizationException e)
        {
            LOGGER.error("Failed to create node into virtual context", e);
            throw new InvalidNodeRefException("Could not create node in virtual context.", parentRef, e);
        }
    }

    /**
     * Moves a node into a virtual target parent.
     *
     * @param actualNodeToMove
     *            the reference to the node to move
     * @param parentReference
     *            the reference to the virtual target parent
     * @param assocTypeQName
     *            the qualified name of the association type to use
     * @param assocQName
     *            the qualified name of the association
     * @return the reference to the new (virtualised) child association
     */
    protected ChildAssociationRef moveNodeInVirtual(final NodeRef actualNodeToMove, final Reference parentReference,
            final QName assocTypeQName, final QName assocQName)
    {
        final NodeServiceTrait theTrait = this.getTrait();
        final NodeRef parentRef = parentReference.toNodeRef();
        final QName nodeTypeQName = theTrait.getType(actualNodeToMove);
        try
        {
            final FilingData filingData = this.smartStore.createFilingData(parentReference, assocTypeQName, assocQName, nodeTypeQName,
                    Collections.emptyMap());

            final NodeRef filingNodeRef = filingData.getFilingNodeRef();
            final QName filingNodeTypeQName = filingData.getNodeTypeQName();
            final QName filingAssocTypeQName = filingData.getAssocTypeQName();
            final QName filingAssocQName = filingData.getAssocQName();

            final Map<QName, Serializable> filingDataProperties = filingData.getProperties();
            final Set<QName> filingAspects = filingData.getAspects();

            if (filingNodeRef == null && filingDataProperties.isEmpty() && filingAspects.isEmpty()
                    && nodeTypeQName.equals(filingData.getNodeTypeQName()))
            {
                LOGGER.debug("Rejecting node move to {} due to lack of filing path or specialising classification details", parentRef);
                throw new InvalidNodeRefException("Can not move node to virtual parent without filing rule(s).", parentRef);
            }

            if (filingNodeRef != null)
            {
                final ChildAssociationRef oldChildAssocRef = theTrait.getPrimaryParent(actualNodeToMove);

                // perform physical move only if necessary
                if (!filingNodeRef.equals(oldChildAssocRef.getParentRef()) || !filingAssocTypeQName.equals(oldChildAssocRef.getTypeQName())
                        || !filingAssocQName.equals(oldChildAssocRef.getQName()))
                {
                    LOGGER.debug("Moving node {} into filing target {}", actualNodeToMove, filingNodeRef);
                    theTrait.moveNode(actualNodeToMove, filingNodeRef, filingAssocTypeQName, filingAssocQName);
                }
            }

            if (this.dictionaryService.isSubClass(filingNodeTypeQName, nodeTypeQName))
            {
                theTrait.setType(actualNodeToMove, filingNodeTypeQName);
            }
            else
            {
                LOGGER.debug("Rejecting node move into {} since current node type {} is incompatible with filing node type {}", parentRef,
                        nodeTypeQName, filingNodeTypeQName);
                throw new InvalidNodeRefException("Can not move node into virtual parent with incompatible filing node type.", parentRef);
            }

            LOGGER.debug("Applying filing properties {} to {}", filingData, actualNodeToMove);
            theTrait.addProperties(actualNodeToMove, filingDataProperties);

            // aspects may already have been auto-applied via addProperties - this leaves only marker aspects
            LOGGER.debug("Applying filing aspects {} to {}", filingAspects, actualNodeToMove);
            filingAspects.stream().filter(aspect -> !theTrait.hasAspect(actualNodeToMove, aspect)).forEach(aspect -> {
                theTrait.addAspect(actualNodeToMove, aspect, Collections.emptyMap());
            });

            final Reference nodeProtocolChildRef = NodeProtocol.newReference(actualNodeToMove, parentReference);
            final QName vChildAssocQName = QName.createQNameWithValidLocalName(VirtualContentModel.VIRTUAL_CONTENT_MODEL_1_0_URI,
                    filingAssocQName.getLocalName());
            final ChildAssociationRef childAssocRef = new ChildAssociationRef(filingAssocTypeQName, parentRef, vChildAssocQName,
                    nodeProtocolChildRef.toNodeRef());

            return childAssocRef;
        }
        catch (final VirtualizationException e)
        {
            LOGGER.error("Failed to move node into virtual context", e);
            throw new InvalidNodeRefException("Could not move node into virtual context.", parentRef, e);
        }
    }

    // copied from base class due to accessibility restrictions
    protected QName materializeAssocQName(final QName assocQName)
    {
        // Version nodes have too long assocQNames so we try
        // to detect references with "material" protocols in order to
        // replace the assocQNames with material correspondents.
        try
        {
            final String lName = assocQName.getLocalName();
            NodeRef nrAssocQName = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, lName);
            if (Reference.fromNodeRef(nrAssocQName) != null)
            {
                nrAssocQName = this.smartStore.materializeIfPossible(nrAssocQName);
                final QName materialAssocQName = QName.createQName(assocQName.getNamespaceURI(), nrAssocQName.getId());
                return materialAssocQName;
            }
            else
            {
                return assocQName;
            }
        }
        catch (final VirtualizationException e)
        {
            // We assume it can not be put through the
            // isReference-virtualize-materialize.
            LOGGER.debug("Defaulting on materializeAssocQName due to error.", e);
            return assocQName;
        }
    }

    // copied from base class due to accessibility restrictions
    protected String handleExistingFile(final NodeRef parentNodeRef, final String fileName)
    {
        String resultFileName = fileName;

        final NodeServiceTrait actualNodeService = this.getTrait();
        NodeRef existingFile = actualNodeService.getChildByName(parentNodeRef, ContentModel.ASSOC_CONTAINS, fileName);

        if (existingFile != null)
        {
            int counter = 1;
            int dotIndex;
            String tmpFilename = "";
            final String dot = ".";
            final String hyphen = "-";

            while (existingFile != null)
            {
                final int beforeCounter = fileName.lastIndexOf(hyphen);
                dotIndex = fileName.lastIndexOf(dot);
                if (dotIndex == 0)
                {
                    // File didn't have a proper 'name' instead it had just a suffix and started with a ".", create "1.txt"
                    tmpFilename = counter + fileName;
                }
                else if (dotIndex > 0)
                {
                    if (beforeCounter > 0 && beforeCounter < dotIndex)
                    {
                        // does file have counter in it's name or it just contains -1
                        final String originalFileName = fileName.substring(0, beforeCounter) + fileName.substring(dotIndex);
                        final boolean doesOriginalFileExist = actualNodeService.getChildByName(parentNodeRef, ContentModel.ASSOC_CONTAINS,
                                originalFileName) != null;
                        if (doesOriginalFileExist)
                        {
                            final String counterStr = fileName.substring(beforeCounter + 1, dotIndex);
                            try
                            {
                                final int parseInt = DefaultTypeConverter.INSTANCE.intValue(counterStr);
                                counter = parseInt + 1;
                                resultFileName = fileName.substring(0, beforeCounter) + fileName.substring(dotIndex);
                                dotIndex = fileName.lastIndexOf(dot);
                            }
                            catch (final NumberFormatException ex)
                            {
                                // "-" is not before counter
                            }

                        }
                    }
                    tmpFilename = fileName.substring(0, dotIndex) + hyphen + counter + fileName.substring(dotIndex);
                }
                else
                {
                    // Filename didn't contain a dot at all, create "filename-1"
                    tmpFilename = fileName + hyphen + counter;
                }
                existingFile = actualNodeService.getChildByName(parentNodeRef, ContentModel.ASSOC_CONTAINS, tmpFilename);
                counter++;
            }
            resultFileName = tmpFilename;
        }

        return resultFileName;
    }
}
