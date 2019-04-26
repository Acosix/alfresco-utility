/*
 * Copyright 2016 - 2019 Acosix GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* global tinyMCE: false */

/* jshint -W003 */
if (typeof Acosix === 'undefined' || !Acosix)
{
    var Acosix = {};
}
/* jshint +W003 */

(function()
{
    var Dom, $isValueSet, $arrayContains, $arrayIndex, editorRegistry;

    Dom = YAHOO.util.Dom;
    $isValueSet = Alfresco.util.isValueSet;
    $arrayContains = Alfresco.util.arrayContains;
    $arrayIndex = Alfresco.util.arrayIndex;

    editorRegistry = {

        editorsByMimetype : {},

        onRegisterEditor : function Acosix_formControls_Content_editorRegistry__onRegisterEditor(layer, args)
        {
            var obj = args[1], editor, idx, mimetype;

            if (obj && $isValueSet(obj.name) && $isValueSet(obj.initialiser) && typeof obj.initialiser === 'function'
                    && ($isValueSet(obj.mimetypes) || $isValueSet(obj.mimetype)))
            {
                editor = {
                    name : obj.name,
                    initialiser : obj.initialiser
                };
                if ($isValueSet(obj.priority))
                {
                    editor.priority = obj.priority;
                }

                if (obj.mimetype)
                {
                    this.editorsByMimetype[obj.mimetype] = this.editorsByMimetype[obj.mimetype] || [];
                    this.editorsByMimetype[obj.mimetype].push(editor);
                }
                else
                {
                    for (idx = 0; idx < obj.mimetypes.length; idx++)
                    {
                        mimetype = obj.mimetypes[idx];
                        this.editorsByMimetype[mimetype] = this.editorsByMimetype[mimetype] || [];
                        this.editorsByMimetype[mimetype].push(editor);
                    }
                }
            }
        },

        getEditor : function Acosix_formControls_Content_editorRegistry__getEditor(mimetype, preferredEditors, forbiddenEditors)
        {
            var eligibleEditors, selectedEditor, idx;

            selectedEditor = null;

            if (this.editorsByMimetype[mimetype])
            {
                // copy
                eligibleEditors = this.editorsByMimetype[mimetype].concat([]);

                if (forbiddenEditors)
                {
                    for (idx = 0; idx < eligibleEditors.length; idx++)
                    {
                        if ($arrayContains(forbiddenEditors, eligibleEditors[idx].name))
                        {
                            eligibleEditors.splice(idx--, 1);
                        }
                    }
                }

                eligibleEditors.sort(function(e1, e2)
                {
                    var order = 0, pIdx1, pIdx2;

                    if (preferredEditors)
                    {
                        pIdx1 = $arrayIndex(preferredEditors, e1.name);
                        pIdx2 = $arrayIndex(preferredEditors, e2.name);

                        if (pIdx1 !== -1 && (pIdx1 < pIdx2 || pIdx2 === -1))
                        {
                            order = -1;
                        }
                        else if (pIdx2 !== -1 && (pIdx2 < pIdx1 || pIdx1 === -1))
                        {
                            order = 1;
                        }
                    }

                    if (order === 0)
                    {
                        if (e1.priority === e2.priority)
                        {
                            order = e1.name.localeCompare(e2.name);
                        }
                        else if ($isValueSet(e1.priority) && (!$isValueSet(e2.priority) || e1.priority < e2.priority))
                        {
                            order = -1;
                        }
                        else if ($isValueSet(e2.priority) && (!$isValueSet(e1.priority) || e2.priority < e1.priority))
                        {
                            order = 1;
                        }
                    }

                    return order;
                });

                if (eligibleEditors.length > 0)
                {
                    selectedEditor = eligibleEditors[0];
                }
            }

            return selectedEditor;
        }
    };

    YAHOO.Bubbling.on('Acosix.formControls.Content.registerEditor', editorRegistry.onRegisterEditor, editorRegistry);

    YAHOO.Bubbling.fire('Acosix.formControls.Content.registerEditor', {
        name : 'tinyMCE',
        mimetypes : [ 'text/html', 'application/xhtml+xml' ],
        initialiser : function Acosix_formControls_Content_tinyMCE__initialiser(id, textArea, mimetype, onChange, options)
        {
            var editor, handleChange;

            // identical to Alfresco.RichTextControl._renderTinyMCEEditor
            editor = new Alfresco.util.RichEditor('tinyMCE', id, options.editorParameters);

            if (!options.currentValue || options.currentValue.indexOf('mimetype=text/html') !== -1)
            {
                editor.getEditor().settings.forced_root_block = 'p';
            }
            // render and register event handler
            editor.render();

            handleChange = function()
            {
                if (editor.isDirty())
                {
                    editor.save();
                }

                onChange();
            };

            // Make sure we persist the dom content from the editor in to the hidden textarea when appropriate
            editor.getEditor().on('BeforeSetContent', handleChange);

            // register the listener to add saving of the editor contents before form is submitted
            YAHOO.Bubbling.on('formBeforeSubmit', handleChange);

            // MNT-10232: Description is displayed with tags
            if (id.indexOf('_prop_cm_') > 0 && id.indexOf('_prop_cm_content') === -1)
            {
                editor.getEditor().on('SaveContent', function(e)
                {
                    var content;

                    e.format = 'text';
                    content = tinyMCE.activeEditor.getBody().textContent;
                    if (content === undefined)
                    {
                        content = tinyMCE.activeEditor.getBody().innerText;
                    }
                    e.content = content;
                });
            }

            return editor;
        }
    });

    Acosix.formControls = Acosix.formControls || {};

    Acosix.formControls.Content = function Acosix_formControls_Content__constructor(htmlId, name, components)
    {
        var componentName = name || 'Acosix.formControls.Content';

        return Acosix.formControls.Content.superclass.constructor.call(this, componentName, htmlId, (components || []));
    };

    YAHOO.extend(Acosix.formControls.Content, Alfresco.component.Base, {
        options : {

            currentValue : '',

            disabled : false,

            mandatory : false,

            // only for TinyMCE fallback support
            editorParameters : null,

            formMode : 'edit',

            nodeRef : null,

            mimeType : null,

            forceEditor : false,

            forceContent : false,

            preferredEditors : null,

            forbiddenEditors : null,
        },

        editor : null,

        onReady : function Acosix_formControls_Content__onReady()
        {
            var mimetype;

            mimetype = this._determineMimeType();

            if (mimetype !== null)
            {
                if (this._isSupportedMimetype(mimetype))
                {
                    if (this.options.formMode === 'create')
                    {
                        // render immediately
                        if (!this.options.disabled)
                        {
                            this._renderEditor(mimetype);
                        }
                    }
                    else
                    {
                        this._populateContent({
                            successCallback : {
                                fn : function()
                                {
                                    if (!this.options.disabled)
                                    {
                                        this._renderEditor(mimetype);
                                    }
                                },
                                scope : this
                            }
                        });
                    }
                }
                else
                {
                    this._hideField();

                    if (Alfresco.logger.isDebugEnabled())
                    {
                        Alfresco.logger.debug('Hidden field \'' + this.id + '\' as the content for the mimetype can not be displayed');
                    }
                }
            }
            else if (this.options.forceEditor)
            {
                this._populateContent();
            }
            else
            {
                this._hideField();

                if (Alfresco.logger.isDebugEnabled())
                {
                    Alfresco.logger.debug('Hidden field \'' + this.id + '\' as the mimetype is unknown');
                }
            }
        },

        _determineMimeType : function Acosix_formControls_Content__determineMimeType()
        {
            var result, mtBegIdx, mtEndIdx;

            result = null;
            // copied from Alfresco.ContentControl
            if (this.options.currentValue.indexOf('contentUrl=') === 0 && this.options.currentValue.indexOf('mimetype=') !== -1)
            {
                // extract the mimetype from the content url
                mtBegIdx = this.options.currentValue.indexOf('mimetype=') + 9;
                mtEndIdx = this.options.currentValue.indexOf('|', mtBegIdx);
                result = this.options.currentValue.substring(mtBegIdx, mtEndIdx);
            }

            // if the content url did not contain the mimetype examine the mimeType parameter
            if (this.options.mimeType !== null && this.options.mimeType.length > 0)
            {
                result = this.options.mimeType;
            }

            if (Alfresco.logger.isDebugEnabled())
            {
                Alfresco.logger.debug('Determined mimetype: ' + result);
            }

            return result;
        },

        _isSupportedMimetype : function Acosix_formControls_Content__isSupportedMimetype(mimetype)
        {
            var supported;

            supported = /^text\/.+$/.test(mimetype)
                    || editorRegistry.getEditor(mimetype, this.options.preferredEditors, this.options.forbiddenEditors) !== null;

            return supported;
        },

        _populateContent : function Acosix_formControls_Content__populateContent(callback)
        {
            var onSuccess, onFailure, nodeRefUrl;

            if (this.options.nodeRef !== null && this.options.nodeRef.length > 0)
            {
                if (Alfresco.logger.isDebugEnabled())
                {
                    Alfresco.logger.debug('Retrieving content for field \'' + this.id + '\' using nodeRef: ' + this.options.nodeRef);
                }

                // success handler, show the content
                onSuccess = function Acosix_formControls_Content__populateContent_onSuccess(response)
                {
                    Dom.get(this.id).value = response.serverResponse.responseText;

                    // if a callback was provided, execute it
                    if (callback && callback.successCallback)
                    {
                        if (Alfresco.logger.isDebugEnabled())
                        {
                            Alfresco.logger.debug('calling callback');
                        }

                        callback.successCallback.fn.call(callback.successCallback.scope, response);
                    }
                };

                // failure handler, display alert
                onFailure = function Acosix_formControls_Content__populateContent_onFailure()
                {
                    // hide the whole field so incorrect content does not get re-submitted
                    this._hideField();

                    if (Alfresco.logger.isDebugEnabled())
                    {
                        Alfresco.logger.debug('Hidden field \'' + this.id + '\' as content retrieval failed');
                    }
                };

                // attempt to retrieve content
                nodeRefUrl = this.options.nodeRef.replace('://', '/');
                Alfresco.util.Ajax.request({
                    url : Alfresco.constants.PROXY_URI + 'api/node/content/' + nodeRefUrl,
                    method : 'GET',
                    successCallback : {
                        fn : onSuccess,
                        scope : this
                    },
                    failureCallback : {
                        fn : onFailure,
                        scope : this
                    }
                });
            }
            else if (this.options.formMode !== 'create')
            {
                this._hideField();

                if (Alfresco.logger.isDebugEnabled())
                {
                    Alfresco.logger.debug('Hidden field \'' + this.id + '\' as the nodeRef parameter is missing');
                }
            }
        },

        _renderEditor : function Acosix_formControls_Content__renderEditor(mimetype)
        {
            var editor, _this;

            editor = editorRegistry.getEditor(mimetype, this.options.preferredEditors, this.options.forbiddenEditors);
            if (editor !== null)
            {
                _this = this;
                this.editor = editor.initialiser(this.id, Dom.get(this.id), mimetype, function()
                {
                    _this._handleContentChange();
                }, this.options);
            }
            // unless content is a text-variant, we can't display without special editors
            else if (!(/^text\/.+$/.test(mimetype)))
            {
                this._hideField();

                if (Alfresco.logger.isDebugEnabled())
                {
                    Alfresco.logger.debug('Hidden field \'' + this.id + '\' as the content for the mimetype can not be displayed');
                }
            }
        },

        _handleContentChange : function Acosix_formControls_Content__handleContentChange()
        {
            // inform the forms runtime if this field is mandatory
            if (this.options.mandatory)
            {
                YAHOO.Bubbling.fire('mandatoryControlValueUpdated', this);
            }
        },

        _hideField : function Acosix_formControls_Content__hideField()
        {
            if (!this.options.forceContent)
            {
                // change the name of the textarea so it is not submitted as new content!
                Dom.get(this.id).name = '-';
            }

            // hide the whole field
            Dom.get(this.id + '-field').style.display = 'none';
        }
    });
}());
