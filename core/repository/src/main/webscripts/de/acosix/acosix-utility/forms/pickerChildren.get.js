/**
 * Resolve "virtual" nodeRefs, nodeRefs and xpath expressions into nodes
 *
 * @method resolveNode
 * @param reference {string} "virtual" nodeRef, nodeRef or xpath expressions
 * @return {ScriptNode|null} Node corresponding to supplied expression. Returns null if node cannot be resolved.
 */
function resolveNode(reference)
{
    return utils.resolveNodeReference(reference);
}

function main()
{
    var itemType, parent, rootNode, results, nodeRef, xPathNodes, maxResults, searchTerm, selectableType, aspect, categories, rootCategories, idx;

    parent = null;
    rootNode = companyhome;
    results = [];
    try
    {
        itemType = String(url.templateArgs.type);
        if (url.templateArgs.store_type && url.templateArgs.store_id && url.templateArgs.id)
        {
            nodeRef = url.templateArgs.store_type + '://' + url.templateArgs.store_id + '/' + url.templateArgs.id;
        }
        else if (args.xpath && String(args.xpath).length > 0)
        {
            xPathNodes = search.selectNodes(args.xpath);
            if (xPathNodes)
            {
                nodeRef = String(xPathNodes[0].nodeRef);
            }
        }
        
        maxResults = 100;
        if (args.size !== null && String(args.size).length > 0)
        {
            maxResults = parseInt(args.size, 10) || maxResults;
        }
        searchTerm = args.searchTerm && String(args.searchTerm).length > 0 ? String(args.searchTerm) : null;
        selectableType = args.selectableType ? String(args.selectableType) : null;

        if (itemType === 'category')
        {
            aspect = String(args.aspect || 'cm:generalclassifiable');
            categories = [];
        
            if (aspect === 'cm:taggable' || nodeRef === 'workspace://SpacesStore/tag:tag-root')
            {
                parent = resolveNode(nodeRef || 'workspace://SpacesStore/tag:tag-root');
                categories = classification.getRootCategories('cm:taggable', searchTerm, maxResults, 0);
            }
            else
            {
                rootCategories = classification.getRootCategories(aspect, null, 1, 0);
                if (rootCategories)
                {
                    if (args.rootNode)
                    {
                        rootNode = resolveNode(args.rootNode) || rootCategories[0].parent;
                    }
                    else
                    {
                        rootNode = rootCategories[0].parent;
                    }

                    if (nodeRef === null || nodeRef === 'alfresco://category/root')
                    {
                        parent = rootNode;
                        if (searchTerm)
                        {
                            categories = search.query({
                                language: 'fts-alfresco',
                                query: 'ANCESTOR:"' + parent.nodeRef + '" AND TYPE:"' + (selectableType || 'cm:category') + '" AND cm:name:"*' + searchTerm + '*"',
                                page: {
                                    maxItems: maxResults,
                                    skipCount: 0
                                },
                                sort: [{
                                    column: '@cm:name',
                                    ascending: true
                                }]
                            });
                        }
                        else
                        {
                            categories = classification.getRootCategories(aspect, null, maxResults, 0);
                        }
                    }
                    else
                    {
                        parent = resolveNode(nodeRef);
                        if (searchTerm)
                        {
                            categories = search.query({
                                language: 'fts-alfresco',
                                query: 'ANCESTOR:"' + parent.nodeRef + '" AND TYPE:"' + (selectableType || 'cm:category') + '" AND cm:name:"*' + searchTerm + '*"',
                                page: {
                                    maxItems: maxResults,
                                    skipCount: 0
                                },
                                sort: [{
                                    column: '@cm:name',
                                    ascending: true
                                }]
                            });
                        }
                        else
                        {
                            categories = search.query({
                                language: 'fts-alfresco',
                                query: 'PARENT:"' + parent.nodeRef + '" AND TYPE:"' + (selectableType || 'cm:category') + '"',
                                page: {
                                    maxItems: maxResults,
                                    skipCount: 0
                                },
                                sort: [{
                                    column: '@cm:name',
                                    ascending: true
                                }]
                            });
                        }
                    }
                }
            }

            for (idx = 0; idx < categories.length; idx++)
            {
                results.push({
                    item: categories[idx],
                    selectable: categories[idx].isSubType(selectableType || 'cm:category')
                });
            }
        }
        else
        {
            status.setCode(400, 'Endpoint does not support the item type ' + itemType);
        }
    }
    catch (e)
    {
        status.setCode(500, e.message);
    }

    model.parent = parent;
    model.rootNode = rootNode;
    model.results = results;
}

main();