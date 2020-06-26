(function()
{
    var Dom, $combine, $isValueSet;

    Dom = YAHOO.util.Dom;
    $combine = Alfresco.util.combinePaths;
    $isValueSet = Alfresco.util.isValueSet;

    YAHOO.Bubbling.fire('registerAction', {
        actionName : 'onActionDeleteConfirmWithBackendErrorMessage',
        fn : function acosix_utility_onActionDeleteConfirmWithBackendErrorMessage(record)
        {
            var parentNodeRef, display;

            parentNodeRef = this.getParentNodeRef(record);
            display = {
                zIndex : this.fullscreen !== undefined
                        && (Dom.hasClass(this.id, 'alf-true-fullscreen') || Dom.hasClass(this.id, 'alf-fullscreen')) ? 1000 : 0,
                parentElement : Dom.hasClass(this.id, 'alf-true-fullscreen') ? Dom.get(this.id) : undefined
            };

            this.modules.actions.genericAction({
                success : {
                    activity : {
                        siteId : this.options.siteId,
                        activityType : record.jsNode.isContainer ? 'folder-deleted' : 'file-deleted',
                        page : 'documentlibrary',
                        activityData : {
                            fileName : record.location.file,
                            path : record.location.path,
                            nodeRef : record.jsNode.nodeRef.toString(),
                            parentNodeRef : parentNodeRef.toString()
                        }
                    },
                    event : {
                        name : record.jsNode.isContainer ? 'folderDeleted' : 'fileDeleted',
                        obj : {
                            path : $combine(record.location.path, record.location.file)
                        }
                    },
                    display : display,
                    message : this.msg('message.delete.success', record.displayName),
                    callback : {
                        fn : function acosix_utility_onActionDeleteConfirmWithBackendErrorMessage_success(response)
                        {
                            if (this.totalRecords)
                            {
                                this.totalRecords -= response.json.successCount;
                            }
                        },
                        scope : this
                    }
                },
                failure : {
                    callback : {
                        fn : function acosix_utility_onActionDeleteConfirmWithBackendErrorMessage_failure(response)
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
                                message = this.msg('message.delete.failure', record.displayName);
                            }

                            Alfresco.util.PopupManager.displayMessage({
                                text : message,
                                zIndex : display.zIndex,
                                displayTime : Alfresco.util.PopupManager.defaultDisplayMessageConfig.displayTime * 2
                            }, display.parentElement);
                        },
                        scope : this
                    }
                },
                webscript : {
                    method : Alfresco.util.Ajax.DELETE,
                    name : 'file/node/{nodeRef}',
                    params : {
                        nodeRef : record.jsNode.nodeRef.uri
                    }
                },
                wait : {
                    message : this.msg('message.multiple-delete.please-wait')
                }
            });
        }
    });

    YAHOO.Bubbling.fire('registerAction', {
        actionName : 'onActionDeleteSyncConfirmWithBackendErrorMessage',
        fn : function acosix_utility_onActionDeleteSyncConfirmWithBackendErrorMessage(record, requestDeleteRemote)
        {
            Alfresco.util.Ajax.request({
                url : Alfresco.constants.PROXY_URI + 'enterprise/sync/syncsetmembers/' + record.jsNode.nodeRef.uri
                        + '?requestDeleteRemote=' + requestDeleteRemote,
                method : Alfresco.util.Ajax.DELETE,
                successCallback : {
                    fn : function acosix_utility_onActionDeleteSyncConfirmWithBackendErrorMessage_onCloudUnsyncSuccess()
                    {
                        YAHOO.Bubbling.fire('metadataRefresh');
                        Alfresco.util.PopupManager.displayMessage({
                            text : this.msg('message.unsync.success')
                        });
                        this.onActionDeleteConfirmWithBackendErrorMessage.call(this, record);
                    },
                    scope : this
                },
                failureMessage : this.msg('message.unsync.failure')
            });
        }
    });

    YAHOO.Bubbling
            .fire(
                    'registerAction',
                    {
                        actionName : 'onActionDeleteWithBackendErrorMessage',
                        fn : function acosix_utility_onActionDeleteWithBackendErrorMessage(record)
                        {
                            var content, isCloud, isDirectSSMN, promptText, promptHtml, zIndex, parent, scope, buttons;

                            content = record.jsNode.isContainer ? 'folder' : 'document';
                            isCloud = this.options.syncMode === 'CLOUD';
                            isDirectSSMN = record.jsNode.hasAspect('synch:syncSetMemberNode') && $isValueSet(record.jsNode.properties) ? record.jsNode.properties['sync:directSync'] === 'true'
                                    : false;

                            promptHtml = '';
                            if (!isCloud && isDirectSSMN)
                            {
                                promptHtml = '<div><input type="checkbox" id="requestDeleteRemote" class="requestDeleteRemote-checkBox"><span class="requestDeleteRemote-text">'
                                        + this.msg('sync.remove.' + content + '.from.cloud', record.displayName) + '</span></div>';
                            }
                            promptText = this.msg('message.confirm.delete', record.displayName);

                            zIndex = 0;
                            if (this.fullscreen !== undefined && (this.fullscreen.isWindowOnly || Dom.hasClass(this.id, 'alf-fullscreen')))
                            {
                                zIndex = 1000;
                            }

                            if (Dom.hasClass(this.id, 'alf-true-fullscreen'))
                            {
                                parent = Dom.get(this.id);
                            }

                            scope = this;
                            buttons = [ {
                                text : this.msg('button.delete'),
                                handler : function acosix_utility_onActionDeleteWithBackendErrorMessage_deleteHandler()
                                {
                                    var requestDeleteRemote;

                                    requestDeleteRemote = isCloud ? false : Dom.getAttribute('requestDeleteRemote', 'checked');
                                    this.destroy();

                                    if (isDirectSSMN)
                                    {
                                        scope.onActionDeleteSyncConfirmWithBackendErrorMessage.call(scope, record, requestDeleteRemote);
                                    }
                                    else
                                    {
                                        scope.onActionDeleteConfirmWithBackendErrorMessage.call(scope, record);
                                    }
                                }
                            }, {
                                text : this.msg('button.cancel'),
                                handler : function acosix_utility_onActionDeleteWithBackendErrorMessage_cancelHandler()
                                {
                                    this.destroy();
                                },
                                isDefault : true
                            } ];

                            Alfresco.util.PopupManager.displayPrompt({
                                title : this.msg('actions.' + content + '.delete'),

                                text : promptText + promptHtml,
                                noEscape : true,
                                buttons : buttons,
                                zIndex : zIndex
                            }, parent);
                        }
                    });

}());
