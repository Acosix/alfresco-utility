/*
 * Copyright 2016 - 2024 Acosix GmbH
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

ace
        .define(
                'ace/ext/language-tools-spellcheck',
                [ 'require' ],
                function(require)
                {
                    var $html, Range, activeMarkers, Editor, queuedSpellChecks;

                    $html = Alfresco.util.encodeHTML;

                    Range = require('../range').Range;
                    activeMarkers = [];

                    function buildAnnotatedTextFromSGMLLike(text)
                    {
                        var annotatedTextFragments, pos, max, tagStart, tagEnd;

                        annotatedTextFragments = [];
                        max = text.length;
                        pos = 0;

                        while (pos < max)
                        {
                            tagStart = text.indexOf('<', pos);
                            if (tagStart !== -1)
                            {
                                if (tagStart !== pos)
                                {
                                    annotatedTextFragments.push({
                                        text : text.substring(pos, tagStart).replace(/\n/g, ' ')
                                    });
                                }
                                if (text.indexOf('<![CDATA[', tagStart) === tagStart)
                                {
                                    annotatedTextFragments.push({
                                        markup : '<![CDATA['
                                    });
                                    tagEnd = text.indexOf(']]>', tagStart);
                                    // consider all in CDATA as text - can be any kind of content
                                    annotatedTextFragments.push({
                                        text : text.substring(tagStart + 9, tagEnd).replace(/\n/g, ' ')
                                    });
                                    annotatedTextFragments.push({
                                        markup : ']]>'
                                    });
                                    pos = tagEnd + 3;
                                }
                                else if (text.indexOf('<!--', tagStart) === tagStart)
                                {
                                    tagEnd = text.indexOf('-->', tagStart);
                                    // consider all in comment as markup - can be anything, but definitely not actual "content"
                                    annotatedTextFragments.push({
                                        markup : text.substring(tagStart, tagEnd + 3)
                                    });
                                    pos = tagEnd + 3;
                                }
                                else
                                {
                                    tagEnd = text.indexOf('>', tagStart);
                                    annotatedTextFragments.push({
                                        markup : text.substring(tagStart, tagEnd + 1)
                                    });
                                    pos = tagEnd + 1;
                                }
                            }
                            else
                            {
                                // rest is all non-tag content (invalid in SGML, but possible during editing)
                                annotatedTextFragments.push({
                                    text : text.substring(pos).replace(/\n/g, ' ')
                                });
                                pos = max;
                            }
                        }

                        return annotatedTextFragments;
                    }

                    function performSpellCheck(queuedCheck)
                    {
                        var url, fullText, dataObj, idx;

                        url = queuedCheck.editor.$ltSpellCheckUrl;
                        if (queuedCheck.editor.$ltSpellCheckUrlMode === 'proxyRelative')
                        {
                            url = Alfresco.constants.PROXY_URI_RELATIVE.replace(/alfresco\/$/, queuedCheck.editor.$ltSpellCheckUrlProxy
                                    + '/')
                                    + url;
                        }
                        else if (queuedCheck.editor.$ltSpellCheckUrlMode === 'serviceRelative')
                        {
                            url = Alfresco.constants.URL_SERVICECONTEXT + url;
                        }

                        fullText = queuedCheck.editorSession.getValue();
                        dataObj = {
                            language : queuedCheck.editor.$ltSpellCheckLanguage || Alfresco.constants.JS_LOCALE,
                            enabledOnly : 'false'
                        };

                        if (queuedCheck.editorSession.getMode().$id === 'ace/mode/html'
                                || queuedCheck.editorSession.getMode().$id === 'ace/mode/xml')
                        {
                            dataObj.data = JSON.stringify({
                                annotation : buildAnnotatedTextFromSGMLLike(fullText)
                            });
                        }
                        else
                        {
                            dataObj.text = fullText;
                        }

                        queuedCheck.inProgress = true;
                        Alfresco.util.Ajax
                                .request({
                                    url : url,
                                    dataObj : dataObj,
                                    method : Alfresco.util.Ajax.POST,
                                    requestContentType : Alfresco.util.Ajax.FORM,
                                    responseContentType : Alfresco.util.Ajax.JSON,
                                    successCallback : {
                                        fn : function(response)
                                        {
                                            var idx, textRows, rowIdx, columnIdx, overallPositionIdx, matches, annotations, match, startRow, startOffset, endRow, endOffset, toSkip, annotationText, replacementIdx;

                                            for (idx = 0; idx < activeMarkers.length; idx++)
                                            {
                                                queuedCheck.editorSession.removeMarker(activeMarkers[idx]);
                                            }
                                            activeMarkers = [];

                                            textRows = fullText.split(/\n/);
                                            rowIdx = 0;
                                            columnIdx = 0;
                                            overallPositionIdx = 0;

                                            matches = response.json.matches;
                                            annotations = [];

                                            for (idx = 0; idx < matches.length; idx++)
                                            {
                                                match = matches[idx];

                                                // as an editor for code / rich text, we are bound to end up with many repeated whitespaces
                                                // so we ignore this kind of reported issue
                                                if (match.rule.issueType === 'whitespace')
                                                {
                                                    continue;
                                                }

                                                while (overallPositionIdx < match.offset && rowIdx < textRows.length)
                                                {
                                                    toSkip = match.offset - overallPositionIdx;
                                                    if (toSkip >= textRows[rowIdx].length - columnIdx)
                                                    {
                                                        overallPositionIdx += textRows[rowIdx].length - columnIdx;
                                                        rowIdx++;
                                                        columnIdx = 0;
                                                        // one position for line break
                                                        overallPositionIdx++;
                                                    }
                                                    else
                                                    {
                                                        columnIdx += toSkip;
                                                        overallPositionIdx += toSkip;
                                                    }
                                                }

                                                if (overallPositionIdx === match.offset)
                                                {
                                                    startRow = rowIdx;
                                                    startOffset = columnIdx;
                                                }

                                                while (overallPositionIdx < match.offset + match.length && rowIdx < textRows.length)
                                                {
                                                    toSkip = (match.offset + match.length) - overallPositionIdx;
                                                    if (toSkip > textRows[rowIdx].length - columnIdx)
                                                    {
                                                        overallPositionIdx += textRows[rowIdx].length - columnIdx;
                                                        rowIdx++;
                                                        columnIdx = 0;
                                                        // one position for line break
                                                        overallPositionIdx++;
                                                    }
                                                    else
                                                    {
                                                        columnIdx += toSkip;
                                                        overallPositionIdx += toSkip;
                                                    }
                                                }

                                                if (overallPositionIdx === match.offset + match.length)
                                                {
                                                    endRow = rowIdx;
                                                    endOffset = columnIdx;
                                                }

                                                if (rowIdx < textRows.length && columnIdx <= textRows[rowIdx].length)
                                                {
                                                    activeMarkers.push(queuedCheck.editorSession.addMarker(new Range(startRow, startOffset,
                                                            endRow, endOffset), 'spellcheck-error', 'text', false));

                                                    if (match.shortMessage)
                                                    {
                                                        annotationText = match.shortMessage;
                                                        if (match.rule.issueType === 'misspelling')
                                                        {
                                                            annotationText += ': ';
                                                            for (replacementIdx = 0; replacementIdx < match.replacements.length; replacementIdx++)
                                                            {
                                                                if (replacementIdx !== 0)
                                                                {
                                                                    annotationText += ', ';
                                                                }
                                                                annotationText += '"';
                                                                annotationText += match.replacements[replacementIdx].value;
                                                                annotationText += '"';
                                                            }
                                                        }

                                                        annotations.push({
                                                            row : startRow,
                                                            type : 'info',
                                                            text : annotationText
                                                        });
                                                    }
                                                }
                                            }
                                            queuedCheck.editorSession.setAnnotations(annotations);
                                        },
                                        scope : this
                                    },
                                    failureCallback : {
                                        fn : function(response)
                                        {
                                            var message, excMessageStart;

                                            if (response.json)
                                            {
                                                message = response.json.message;
                                                if (message.search(':') !== -1)
                                                {
                                                    excMessageStart = message.search(':') + 1;
                                                    message = message.substr(excMessageStart, message.length - 1);
                                                }
                                            }
                                            else
                                            {
                                                message = Alfresco.util.message('acosix.utility.language-tools.spellcheck.unknownError',
                                                        null, {
                                                            0 : response.serverResponse.status
                                                        });
                                            }

                                            Alfresco.util.PopupManager.displayMessage({
                                                title : Alfresco.util.message('message.failure'),
                                                text : $html(message)
                                            });
                                        },
                                        scope : this
                                    }
                                });

                        for (idx = 0; idx < queuedSpellChecks.length; idx++)
                        {
                            if (queuedSpellChecks[idx] === queuedCheck)
                            {
                                queuedSpellChecks.splice(idx, 1);
                                break;
                            }
                        }
                    }

                    queuedSpellChecks = [];
                    function onEditorChange(delta, editor)
                    {
                        var queued, idx;

                        if (editor.$enableLTSpellCheck && editor.$ltSpellCheckUrl && editor.$ltSpellCheckUrlMode)
                        {
                            if (editor.$ltSpellCheckUrlMode !== 'proxyRelative' || editor.$ltSpellCheckUrlProxy)
                            {
                                queued = null;

                                for (idx = 0; idx < queuedSpellChecks.length; idx++)
                                {
                                    if (queuedSpellChecks[idx].editor === editor && !queuedSpellChecks[idx].inProgress)
                                    {
                                        queued = queuedSpellChecks[idx];
                                        break;
                                    }
                                }

                                if (!queued)
                                {
                                    queued = {
                                        editor : editor,
                                        editorSession : editor.getSession(),
                                        inProgress : false,
                                        timeout : null
                                    };
                                    queuedSpellChecks.push(queued);
                                }
                                else
                                {
                                    clearTimeout(queued.timeout);
                                }

                                queued.timeout = setTimeout(performSpellCheck, Math.max(250, editor.$ltSpellCheckDelay), queued);
                            }
                        }
                    }

                    Editor = require('../editor').Editor;
                    require('../config').defineOptions(Editor.prototype, 'editor', {
                        enableLTSpellCheck : {
                            set : function(val)
                            {
                                if (val === true || val === 'true')
                                {
                                    this.on('change', onEditorChange);
                                }
                                else
                                {
                                    this.off('change', onEditorChange);
                                }
                            },
                            values : [ false, true ],
                            initialValue : false
                        },
                        ltSpellCheckUrl : {
                            initialValue : null
                        },
                        ltSpellCheckUrlMode : {
                            values : [ 'absolute', 'serviceRelative', 'proxyRelative' ],
                            initialValue : null
                        },
                        ltSpellCheckUrlProxy : {
                            initialValue : null
                        },
                        ltSpellCheckDelay : {
                            initialValue : 500
                        },
                        ltSpellCheckLanguage : {
                            initialValue : null
                        }
                    });
                });
