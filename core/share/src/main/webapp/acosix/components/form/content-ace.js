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

/* global ace: false */

(function()
{
    var Dom;

    Dom = YAHOO.util.Dom;

    YAHOO.Bubbling.fire('Acosix.formControls.Content.registerEditor', {
        name : 'ace-html',
        // just have "any" priority to be automatically preferred over tinyMCE
        priority : 100,
        mimetypes : [ 'text/html', 'application/xhtml+xml' ],
        initialiser : function Acosix_formControls_Content_aceHtml__initialiser(id, textArea, mimetype, onChange, options)
        {
            var fieldContainer, editorRootContainer, editorContainer, editor;

            fieldContainer = textArea.parentNode;
            editorRootContainer = document.createElement('div');
            Dom.addClass(editorRootContainer, 'acosix-aceRoot');
            fieldContainer.insertBefore(editorRootContainer, textArea);

            editorContainer = document.createElement('div');
            Dom.addClass(editorContainer, 'editorContainer');
            editorRootContainer.appendChild(editorContainer);

            ace.require('ace/ext/language_tools');
            ace.require('ace/ext/language-tools-spellcheck');

            editor = ace.edit(editorContainer);
            editor.setOptions({
                autoScrollEditorIntoView : false,
                showPrintMargin : (options.aceOptions || {}).showPrintMargin || false,
                theme : 'ace/theme/' + ((options.aceOptions || {}).theme || 'textmate'),
                newLineMode : ((options.aceOptions || {}).newLineMode || 'unix'),
                useWorker : false,
                wrap : (options.aceOptions || {}).wrap || false,
                foldStyle : 'manual',
                mode : 'ace/mode/html',
                enableBasicAutocompletion : true,
                enableSnippets : true,
                enableLiveAutocompletion : (options.aceOptions || {}).enableLiveAutoCompletion || false,
                value : textArea.value,
                enableLTSpellCheck : (options.aceOptions || {}).enableLTSpellCheck || null,
                ltSpellCheckUrl : (options.aceOptions || {}).ltSpellCheckUrl || null,
                ltSpellCheckUrlMode : (options.aceOptions || {}).ltSpellCheckUrlMode || null,
                ltSpellCheckUrlProxy : (options.aceOptions || {}).ltSpellCheckUrlProxy || null,
                ltSpellCheckLanguage : (options.aceOptions || {}).ltSpellCheckLanguage || null
            });

            if (options.keyboardHandler)
            {
                editor.setKeyboardHandler(options.keyboardHandler);
            }

            ace.require([ 'ace/ext/beautify', 'ace/ext/whitespace' ], function(beautify, whitespace)
            {
                var commands, idx;

                commands = beautify.commands.concat(whitespace.commands);
                for (idx = 0; idx < commands.length; idx++)
                {
                    editor.commands.addCommand(commands[idx]);
                }
            });

            editor.on('change', function()
            {
                textArea.value = editor.getValue();

                onChange();
            });

            Dom.addClass(textArea, 'hidden');

            return editor;
        }
    });
}());
