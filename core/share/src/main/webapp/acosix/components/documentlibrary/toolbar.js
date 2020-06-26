(function()
{
    var Dom = YAHOO.util.Dom;

    // we could register via YAHOO.Bubbling.fire('registerAction', {})
    // but that would also register with document list / detail pages, which is not what we want
    if (Alfresco.DocListToolbar)
    {
        Alfresco.DocListToolbar.prototype.onToolbarActionDeleteConfirmWithBackendErrorMessage = function acosix_utility_onToolbarActionDeleteConfirmWithBackendErrorMessage(
                records, atomic)
        {
            var recordArr, i, ii;

            recordArr = [];
            for (i = 0, ii = records.length; i < ii; i++)
            {
                recordArr.push(records[i].jsNode.nodeRef.nodeRef);
            }

            this.modules.actions.genericAction({
                success : {
                    callback : {
                        fn : function acosix_utility_onToolbarActionDeleteConfirmWithBackendErrorMessage_success(data)
                        {
                            var result, successFileCount, successFolderCount, successCount, activityData;

                            if (!data.json.overallSuccess)
                            {
                                Alfresco.util.PopupManager.displayMessage({
                                    text : this.msg('message.multiple-delete.failure')
                                });
                                return;
                            }

                            this.modules.docList.totalRecords -= data.json.totalResults;
                            YAHOO.Bubbling.fire('filesDeleted');

                            successFileCount = 0;
                            successFolderCount = 0;
                            for (i = 0, ii = data.json.totalResults; i < ii; i++)
                            {
                                result = data.json.results[i];

                                if (result.success)
                                {
                                    if (result.type === 'folder')
                                    {
                                        successFolderCount++;
                                    }
                                    else
                                    {
                                        successFileCount++;
                                    }

                                    YAHOO.Bubbling.fire(result.type === 'folder' ? 'folderDeleted' : 'fileDeleted', {
                                        multiple : true,
                                        nodeRef : result.nodeRef
                                    });
                                }
                            }

                            successCount = successFolderCount + successFileCount;
                            if (Alfresco.util.isValueSet(this.options.siteId))
                            {
                                if (successCount > 0)
                                {
                                    if (successCount < this.options.groupActivitiesAt)
                                    {
                                        for (i = 0; i < successCount; i++)
                                        {
                                            activityData = {
                                                fileName : data.json.results[i].id,
                                                nodeRef : data.json.results[i].nodeRef,
                                                path : this.currentPath,
                                                parentNodeRef : this.doclistMetadata.parent.nodeRef
                                            };

                                            if (data.json.results[i].type === 'folder')
                                            {
                                                this.modules.actions.postActivity(this.options.siteId, 'folder-deleted', 'documentlibrary',
                                                        activityData);
                                            }
                                            else
                                            {
                                                this.modules.actions.postActivity(this.options.siteId, 'file-deleted', 'documentlibrary',
                                                        activityData);
                                            }
                                        }
                                    }
                                    else
                                    {
                                        if (successFileCount > 0)
                                        {
                                            this.modules.actions.postActivity(this.options.siteId, 'files-deleted', 'documentlibrary', {
                                                fileCount : successFileCount,
                                                path : this.currentPath,
                                                parentNodeRef : this.doclistMetadata.parent.nodeRef
                                            });
                                        }

                                        if (successFolderCount > 0)
                                        {
                                            this.modules.actions.postActivity(this.options.siteId, 'folders-deleted', 'documentlibrary', {
                                                fileCount : successFolderCount,
                                                path : this.currentPath,
                                                parentNodeRef : this.doclistMetadata.parent.nodeRef
                                            });
                                        }
                                    }
                                }
                            }

                            Alfresco.util.PopupManager.displayMessage({
                                text : this.msg('message.multiple-delete.success', successCount)
                            });
                        },
                        scope : this
                    }
                },
                failure : {
                    callback : {
                        fn : function acosix_utility_onToolbarActionDeleteConfirmWithBackendErrorMessage_failure(response)
                        {
                            var message, expRes;

                            if (response.json && response.json.message)
                            {
                                message = response.json.message;
                                expRes = /^([^:]+Exception: \d+ )(.+)$/.exec(message);
                                if (expRes)
                                {
                                    message = expRes[2];
                                }
                            }
                            else
                            {
                                message = this.msg('message.multiple-delete.failure');
                            }

                            Alfresco.util.PopupManager.displayMessage({
                                text : message,
                                displayTime : Alfresco.util.PopupManager.defaultDisplayMessageConfig.displayTime * 2
                            });
                        },
                        scope : this
                    }
                },
                webscript : {
                    method : Alfresco.util.Ajax.DELETE,
                    name : atomic ? 'atomicFiles' : 'files'
                },
                wait : {
                    message : this.msg('message.multiple-delete.please-wait')
                },
                config : {
                    requestContentType : Alfresco.util.Ajax.JSON,
                    dataObj : {
                        nodeRefs : recordArr
                    }
                }
            });
        };

        Alfresco.DocListToolbar.prototype.onToolbarActionDeleteWithBackendErrorMessage = function acosix_utility_onToolbarActionDeleteWithBackendErrorMessage(
                records, atomic)
        {
            var fileNameHtml, i, j, scope;

            if (typeof records.length === 'undefined')
            {
                records = [ records ];
            }

            fileNameHtml = [];
            for (i = 0, j = records.length; i < j; i++)
            {
                fileNameHtml.push('<span class="' + (records[i].jsNode.isContainer ? 'folder' : 'document') + '">'
                        + Alfresco.util.encodeHTML(records[i].displayName) + '</span>');
            }

            scope = this;
            Alfresco.util.PopupManager.displayPrompt({
                title : this.msg('title.multiple-delete.confirm'),
                text : '<div class="toolbar-file-list">' + fileNameHtml.join('') + '</div>',
                noEscape : true,
                modal : true,
                buttons : [ {
                    text : this.msg('button.delete'),
                    handler : function acosix_utility_onToolbarActionDeleteWithBackendErrorMessage_deleteHandler()
                    {
                        this.destroy();
                        scope.onToolbarActionDeleteConfirmWithBackendErrorMessage.call(scope, records, atomic);
                    }
                }, {
                    text : this.msg('button.cancel'),
                    handler : function acosix_utility_onToolbarActionDeleteWithBackendErrorMessage_cancelHandler()
                    {
                        this.destroy();
                    },
                    isDefault : true
                } ]
            });
        };

        Alfresco.DocListToolbar.prototype.onToolbarActionAtomicDeleteWithBackendErrorMessage = function acosix_utility_onToolbarActionAtomicDeleteWithBackendErrorMessage(
                records)
        {
            this.onToolbarActionDeleteWithBackendErrorMessage(records, true);
        };

        Alfresco.DocListToolbar.prototype.onNewCustomFolderType = function acosix_utility_onNewCustomFolderType(record, obj)
        {
            var destination, action, doBeforeDialogShow, templateUrl, createFolder;

            destination = record.nodeRef;

            if (obj)
            {
                if (obj.params)
                {
                    action = obj;
                }
                else
                {
                    action = this.getAction(record, obj);
                }
            }
            else
            {
                action = {
                    params : {
                        type : 'cm:folder'
                    }
                };
            }

            doBeforeDialogShow = function DLTB_onNewFolder_doBeforeDialogShow(p_form, p_dialog)
            {
                Dom.get(p_dialog.id + '-dialogTitle').innerHTML = this.msg('label.new-folder.title');
                Dom.get(p_dialog.id + '-dialogHeader').innerHTML = this.msg('label.new-folder.header');
            };

            templateUrl = YAHOO.lang
                    .substitute(
                            Alfresco.constants.URL_SERVICECONTEXT
                                    + 'components/form?itemKind={itemKind}&itemId={itemId}&destination={destination}&mode={mode}&submitType={submitType}&formId={formId}&showCancelButton=true',
                            {
                                itemKind : 'type',
                                itemId : action.params.type || 'cm:folder',
                                destination : destination,
                                mode : 'create',
                                submitType : 'json',
                                formId : 'doclib-common'
                            });

            createFolder = new Alfresco.module.SimpleDialog(this.id + '-createFolder');

            createFolder.setOptions({
                width : '33em',
                templateUrl : templateUrl,
                actionUrl : null,
                destroyOnHide : true,
                doBeforeDialogShow : {
                    fn : doBeforeDialogShow,
                    scope : this
                },
                onSuccess : {
                    fn : function acosix_utility_onNewCustomFolderType_success(response)
                    {
                        var folderName;

                        folderName = response.config.dataObj[action.params.folderNameField || 'prop_cm_name'];

                        if (folderName)
                        {
                            this.modules.actions.postActivity(this.options.siteId, 'folder-added', 'documentlibrary', {
                                fileName : folderName,
                                nodeRef : response.json.persistedObject,
                                path : this.currentPath + (this.currentPath !== '/' ? '/' : '') + folderName
                            });

                            YAHOO.Bubbling.fire('folderCreated', {
                                name : folderName,
                                parentNodeRef : destination
                            });
                        }

                        Alfresco.util.PopupManager.displayMessage({
                            text : this.msg('message.new-folder.success', folderName)
                        });
                    },
                    scope : this
                },
                onFailure : {
                    fn : function acosix_utility_onNewCustomFolderType_failure(response)
                    {
                        var message, expRes;

                        if (response.json && response.json.message)
                        {
                            message = response.json.message;
                            expRes = /^([^:]+Exception: \d+ )(.+)$/.exec(message);
                            if (expRes)
                            {
                                message = expRes[2];
                            }
                        }
                        else
                        {
                            if (response)
                            {
                                message = this.msg('message.new-folder.failure', response.config.dataObj[action.params.folderNameField
                                        || 'prop_cm_name']);
                            }
                            else
                            {
                                message = this.msg('message.failure');
                            }
                        }

                        Alfresco.util.PopupManager.displayMessage({
                            text : message,
                            displayTime : Alfresco.util.PopupManager.defaultDisplayMessageConfig.displayTime * 2
                        });

                        createFolder.widgets.cancelButton.set('disabled', false);
                    },
                    scope : this
                }
            });
            createFolder.show();
            return createFolder;
        };
    }
}());
