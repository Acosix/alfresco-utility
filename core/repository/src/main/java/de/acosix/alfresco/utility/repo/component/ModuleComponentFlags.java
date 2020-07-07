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
package de.acosix.alfresco.utility.repo.component;

import org.alfresco.util.transaction.TransactionSupportUtil;

/**
 * This class provides a centralised way to track module component execution flags to potentially coordinate specific behaviour of module
 * components running within the same transaction.
 *
 * @author Axel Faust
 */
public class ModuleComponentFlags
{

    private static final String TXN_TRANSACTIONAL_CHANGES_FLAG = ModuleComponentFlags.class.getName() + "-transactionalChangesFlag";

    private ModuleComponentFlags()
    {
        // static-only class
    }

    /**
     * Flags the current transaction as having had a module component run which performed transactional changes, e.g. a content bootstrap /
     * import component.
     */
    public static void flagTransactionalChanges()
    {
        TransactionSupportUtil.bindResource(TXN_TRANSACTIONAL_CHANGES_FLAG, Boolean.TRUE);
    }

    /**
     * Checks whether the transaction has had module component runs which introduced transactional changes.
     *
     * @return {@code true} if the transaction has transactional changes, {@code false} otherwise
     */
    public static boolean hasTransactionalChanges()
    {
        final Object flag = TransactionSupportUtil.getResource(TXN_TRANSACTIONAL_CHANGES_FLAG);
        final boolean hasChanges = Boolean.TRUE.equals(flag);
        return hasChanges;
    }
}
