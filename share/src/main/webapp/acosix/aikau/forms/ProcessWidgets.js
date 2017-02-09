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

/**
 * This module extends the default [ProcessWidgets]{@link module:alfresco/core/ProcessWidgets} to add form layout functionality required to
 * properly manage nested form elements without forcing use of [ControlRow]{@link module:alfresco/core/ControlRow} or [ControlColumn]{@link module:alfresco/core/ControlColumn}.
 * 
 * @module acosix/forms/ProcessWidgets
 * @extends module:alfresco/core/ProcessWidgets
 * @mixes module:alfresco/forms/LayoutMixin
 */
define([ 'dojo/_base/declare', 'alfresco/core/ProcessWidgets', 'alfresco/forms/LayoutMixin' ], function(declare, ProcessWidgets,
        LayoutMixin)
{
    return declare([ ProcessWidgets, LayoutMixin ], {
        cssRequirements : [ {
            cssFile : './css/BaseFormControlEnhancements.css'
        } ]
    });
});
