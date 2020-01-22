(function()
{
    var Dom, Event, $combine;

    Dom = YAHOO.util.Dom;
    Event = YAHOO.util.Event;
    $combine = Alfresco.util.combinePaths;

    if (Alfresco.CategoryManager)
    {
        // this completely replaces the default - we can't adapt it post-construction, so need to adapt it in copied code
        Alfresco.CategoryManager.prototype._buildTree = function()
        {
            var tree, root, idx, labelKey;

            this.insituEditors = [];

            tree = new YAHOO.widget.TreeView(this.id + '-category-manager');
            this.widgets.treeview = tree;

            YAHOO.widget.TreeView.FOCUS_CLASS_NAME = '';

            tree.setDynamicLoad(this.fnLoadNodeData);

            root = tree.getRoot();

            if (this.options.classifications && this.options.classifications.length > 0)
            {
                for (idx = 0; idx < this.options.classifications.length; idx++)
                {
                    labelKey = 'aspect.' + this.options.classifications[idx].replace(':', '_');
                    this._buildTreeNode({
                        name : Alfresco.util.message(labelKey, this.name),
                        path : '/',
                        classification : this.options.classifications[idx],
                        nodeRef : ''
                    }, root, false);
                }
            }
            else
            {
                // default behaviour
                this._buildTreeNode({
                    name : Alfresco.util.message('node.root', this.name),
                    path : '/',
                    nodeRef : ''
                }, root, true);
            }

            tree.subscribe('clickEvent', this.onNodeClicked, this, true);
            tree.subscribe('expandComplete', this.onExpandComplete, this, true);
            tree.subscribe('dblClickEvent', tree.onEventEditNode);

            tree._onKeyDownEvent = function DLT__onKeyDownEvent()
            {
                // Disable the key down events for the tree so that the cursor behaves as it should when editing the node text
            };

            tree.render();
        };

        Alfresco.CategoryManager.prototype._defaultBuildTreeNode = Alfresco.CategoryManager.prototype._buildTreeNode;
        Alfresco.CategoryManager.prototype._buildTreeNode = function(data, parent, expanded)
        {
            var node, classification;

            node = this._defaultBuildTreeNode(data, parent, expanded);
            classification = data.classification || parent.data.classification;

            if (classification)
            {
                // copy / adapt data
                node.data.classification = classification;
                if (node.data.path.indexOf(classification) !== 0)
                {
                    node.data.path = classification + '#' + data.path;
                }
                if (data.nodeRef !== null && this.insituEditors.length > 0)
                {
                    // last-added insitu editor is for the current tree node
                    this.insituEditors[this.insituEditors.length - 1].params.classification = classification;
                }
            }

            return node;
        };

        Alfresco.CategoryManager.prototype._defaultBuildTreeNodeUrl = Alfresco.CategoryManager.prototype._buildTreeNodeUrl;
        Alfresco.CategoryManager.prototype._buildTreeNodeUrl = function(path)
        {
            var nodeRef, aspect, uri;

            if (path.indexOf('#') === -1)
            {
                uri = this._defaultBuildTreeNodeUrl(path);
            }
            else
            {
                nodeRef = new Alfresco.util.NodeRef(this.options.nodeRef);

                aspect = path.substr(0, path.indexOf('#'));
                path = path.substr(path.indexOf('#') + 1);

                uri = 'slingshot/doclib/categorynode/node/' + $combine(encodeURI(nodeRef.uri), Alfresco.util.encodeURIPath(path));
                uri = Alfresco.constants.PROXY_URI + uri + '?perms=false&children=true&aspect=' + aspect;
            }

            return uri;
        };

        // this completely replaces the default - default contains lambda which performs XHR call we need to adapt
        Alfresco.widget.InsituEditorIconAdd.prototype.onIconClick = function(e, obj)
        {
            if (obj.disabled)
            {
                return;
            }
            Event.stopEvent(e);

            if (Alfresco.logger.isDebugEnabled())
            {
                Alfresco.logger.debug('onIconClick', e);
            }

            Alfresco.util.PopupManager.getUserInput({
                title : Alfresco.util.message('tool.category-manager.add-category'),
                text : Alfresco.util.message('tool.category-manager.label.category-name'),
                input : 'text',
                callback : {
                    fn : function promptCallback(newNodeName)
                    {
                        var url, config;

                        url = this._buildAddNodeUrl(this.params.nodeRef);

                        config = {
                            method : 'POST',
                            url : url,
                            successCallback : {
                                fn : function(response)
                                {
                                    var treeNode = this.params.treeNode;
                                    // Only sort children if it has any already loaded
                                    if (treeNode.hasChildren())
                                    {
                                        this.params.component._sortNodeChildren(treeNode);
                                    }
                                    treeNode.toggle();
                                    treeNode.refresh();
                                    treeNode.toggle();

                                    if (response.json.message)
                                    {
                                        Alfresco.util.PopupManager.displayMessage({
                                            text : response.json.message
                                        });
                                    }
                                },
                                scope : this
                            },
                            failureCallback : {
                                fn : function()
                                {
                                    Alfresco.util.PopupManager.displayMessage({
                                        text : Alfresco.util.message('tool.category-manager.add-category.failure')
                                    });
                                },
                                scope : this
                            },
                            dataObj : {
                                name : newNodeName
                            }
                        };

                        if (this.params.classification)
                        {
                            config.dataObj.aspect = this.params.classification;
                        }

                        Alfresco.util.Ajax.jsonRequest(config);
                    },
                    obj : {},
                    scope : obj
                }
            });

            var elements = Dom.getElementsByClassName('yui-button', 'span', 'userInput');
            Dom.addClass(elements[0], 'alf-primary-button');
        };
    }

}());
