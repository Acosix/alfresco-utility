(function()
{
    var Dom = YAHOO.util.Dom;

    YAHOO.Bubbling
            .fire(
                    'registerAction',
                    {
                        actionName : 'onNewCustomFolderType',
                        fn : function acosix_utility_onNewCustomFolderType(record, obj)
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
                                        var activityData, folderName, folderNodeRef;

                                        folderName = response.config.dataObj[action.params.folderNameField || 'prop_cm_name'];
                                        folderNodeRef = response.json.persistedObject;

                                        activityData = {
                                            fileName : folderName,
                                            nodeRef : folderNodeRef,
                                            path : this.currentPath + (this.currentPath !== '/' ? '/' : '') + folderName
                                        };
                                        this.modules.actions.postActivity(this.options.siteId, 'folder-added', 'documentlibrary',
                                                activityData);

                                        YAHOO.Bubbling.fire('folderCreated', {
                                            name : folderName,
                                            parentNodeRef : destination
                                        });
                                        Alfresco.util.PopupManager.displayMessage({
                                            text : this.msg('message.new-folder.success', folderName)
                                        });
                                    },
                                    scope : this
                                },
                                onFailure : {
                                    fn : function acosix_utility_onNewCustomFolderType_failure(response)
                                    {
                                        var folderName;
                                        if (response)
                                        {
                                            folderName = response.config.dataObj[action.params.folderNameField || 'prop_cm_name'];
                                            Alfresco.util.PopupManager.displayMessage({
                                                text : this.msg('message.new-folder.failure', folderName)
                                            });
                                        }
                                        else
                                        {
                                            Alfresco.util.PopupManager.displayMessage({
                                                text : this.msg('message.failure')
                                            });
                                        }
                                        createFolder.widgets.cancelButton.set('disabled', false);
                                    },
                                    scope : this
                                }
                            });
                            createFolder.show();
                            return createFolder;
                        }
                    });
}());
