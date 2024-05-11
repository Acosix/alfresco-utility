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
package de.acosix.alfresco.utility.repo.model;

import org.alfresco.service.namespace.QName;

/**
 * @author Axel Faust
 */
public interface EmailModel
{

    String NAMESPACE = "http://acosix.de/model/utility/email/1.0";

    String PREFIX = "aco6ue";

    QName ASPECT_MAIL_TARGET_FOLDER_SETTINGS = QName.createQName(NAMESPACE, "mailTargetFolderSettings");

    QName PROP_EXTRACT_ATTACHMENTS = QName.createQName(NAMESPACE, "extractAttachments");

    QName PROP_EXTRACT_ATTACHMENTS_AS_DIRECT_CHILDREN = QName.createQName(NAMESPACE, "extractAttachmentsAsDirectChildren");

    QName PROP_OVERWRITE_DUPLICATES = QName.createQName(NAMESPACE, "overwriteDuplicates");

    QName ASPECT_MAIL_WITH_ATTACHMENTS = QName.createQName(NAMESPACE, "mailWithAttachments");

    QName ASPECT_MAIL_WITH_EXTRACTED_ATTACHMENTS = QName.createQName(NAMESPACE, "mailWithExtractedAttachments");

    QName ASSOC_ATTACHMENTS = QName.createQName(NAMESPACE, "attachments");

}
