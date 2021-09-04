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

function findCategories(results, nodeRef, searchTerm, selectableType, maxResults)
{
    var aspect, categories, parent, rootCategories, rootNode, idx;

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

    model.parent = parent;
    model.rootNode = rootNode;
}

function findUsers(results, nodeRef, searchTerm, maxResults)
{
    var ctxt, authorityService, zone, ancestorAuthority, node, personNodes, userNameSet, collator, nameCache, ensureCached;

    ctxt = Packages.org.springframework.web.context.ContextLoader.getCurrentWebApplicationContext();
    authorityService = ctxt.getBean('AuthorityService', Packages.org.alfresco.service.cmr.security.AuthorityService);

    zone = args.zone ? String(args.zone) : null;
    ancestorAuthority = null;
    if (nodeRef)
    {
        node = search.findNode(nodeRef);
        if (node.isSubType('cm:authorityContainer'))
        {
            ancestorAuthority = String(node.properties.authorityName);
        }
    }

    // retrieves only refs, not actual nodes
    personNodes = people.getPeople(searchTerm, zone === null && ancestorAuthority === null ? maxResults : -1, 'lastName', true);
    personNodes = personNodes.map(function (ref)
    {
        return search.findNode(ref);
    });

    // zone is a cheaper filter than ancestorAuthority, so apply first to reduce cost for second
    if (zone !== null)
    {
        personNodes = personNodes.filter(function (p)
        {
            p.parentAssocs.inZone.some(function (z)
            {
                return String(z.name) === zone;
            });
        });
    }

    if (ancestorAuthority !== null)
    {
        userNameSet = authorityService.findAuthorities(Packages.org.alfresco.service.cmr.security.AuthorityType.USER, ancestorAuthority, false, null, zone);
        personNodes = personNodes.filter(function (p)
        {
            return userNameSet.contains(p.properties.userName);
        });
    }

    collator = Packages.java.text.Collator.getInstance(Packages.org.springframework.extensions.surf.util.I18NUtil.getLocale());
    nameCache = {};
    ensureCached = function (p)
    {
        if (!nameCache.hasOwnProperty(p.properties.userName))
        {
            nameCache[p.properties.userName] = String(p.properties.lastName || p.properties.userName);
            if (p.properties.lastName && p.properties.firstName)
            {
                nameCache[p.properties.userName] += ' ' + p.properties.firstName;
            }
            nameCache[p.properties.userName] = nameCache[p.properties.userName].toLowerCase();
        }
    };
    personNodes.sort(function (a, b)
    {
        ensureCached(a);
        ensureCached(b);
        return collator.compare(nameCache[a.properties.userName], nameCache[b.properties.userName]);
    });

    if (personNodes.length > maxResults) 
    {
        personNodes.splice(maxResults, personNodes.length - maxResults);
    }

    personNodes.forEach(function (p)
    {
        results.push({
            item: {
                typeShort : p.typeShort,
                isContainer: false,
                properties: {
                    userName: p.properties.userName,
                    name: (p.properties.firstName ? (p.properties.firstName + ' ') : '') + (p.properties.lastName ? (p.properties.lastName + ' ') : '') + '(' + p.properties.userName + ')',
                    jobtitle: p.properties.jobtitle || ''
                },
                displayPath: p.displayPath,
                nodeRef: '' + p.nodeRef
            },
            selectable: people.isAccountEnabled(p.properties.userName)
        });
    });
}

function findGroups(results, nodeRef, searchTerm, maxResults)
{
    var ctxt, authorityService, zone, node, ancestorAuthority, authorityNameSet, authorityNameIter, authorityName, resultGroups, collator, nameCache, ensureCached;

    ctxt = Packages.org.springframework.web.context.ContextLoader.getCurrentWebApplicationContext();
    authorityService = ctxt.getBean('AuthorityService', Packages.org.alfresco.service.cmr.security.AuthorityService);

    zone = String(args.zone || 'APP.DEFAULT');
    ancestorAuthority = null;
    if (nodeRef)
    {
        node = search.findNode(nodeRef);
        if (node.isSubType('cm:authorityContainer'))
        {
            ancestorAuthority = String(node.properties.authorityName);
        }
    }

    if (searchTerm !== null && (searchTerm.length === 0 || searchTerm === '*'))
    {
        searchTerm = null;
    }

    resultGroups = [];
    authorityNameSet = authorityService.findAuthorities(Packages.org.alfresco.service.cmr.security.AuthorityType.GROUP, ancestorAuthority, false, searchTerm, zone);
    authorityNameIter = authorityNameSet.iterator();
    while (authorityNameIter.hasNext())
    {
        authorityName = authorityNameIter.next();
        resultGroups.push(groups.getGroup(authorityName));
    }

    collator = Packages.java.text.Collator.getInstance(Packages.org.springframework.extensions.surf.util.I18NUtil.getLocale());
    nameCache = {};
    ensureCached = function (g)
    {
        if (!nameCache.hasOwnProperty(g.fullName))
        {
            nameCache[g.fullName] = String(g.displayName || g.shortName).toLowerCase();
        }
    };
    resultGroups.sort(function (a, b)
    {
        ensureCached(a);
        ensureCached(b);
        return collator.compare(nameCache[a.fullName], nameCache[b.fullName]);
    });

    if (resultGroups.length > maxResults - results.length)
    {
        resultGroups.splice(maxResults - results.length, resultGroups.length - (maxResults - results.length));
    }

    resultGroups.forEach(function (group)
    {
        results.push({
            item: {
                typeShort : group.groupNode.typeShort,
                isContainer: false,
                properties: {
                    name: group.displayName || group.shortName
                },
                displayPath: group.groupNode.displayPath,
                nodeRef: '' + group.groupNode.nodeRef
            },
            selectable: true
        });
    });
}

function findAuthorities(results, nodeRef, searchTerm, selectableType, maxResults)
{
    if (selectableType === 'cm:person')
    {
        findUsers(results, nodeRef, searchTerm, maxResults);
    }
    else if (selectableType === 'cm:authorityContainer')
    {
        findGroups(results, nodeRef, searchTerm, maxResults);
    }
    else
    {
        findGroups(results, nodeRef, searchTerm, maxResults);
        findUsers(results, nodeRef, searchTerm, maxResults);
    }
}

function main()
{
    var itemType, results, nodeRef, xPathNodes, maxResults, searchTerm, selectableType;

    model.parent = null;
    model.rootNode = companyhome;
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
            findCategories(results, nodeRef, searchTerm, selectableType, maxResults);
        }
        else if (itemType === 'authority')
        {
            findAuthorities(results, nodeRef, searchTerm, selectableType, maxResults);
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

    
    model.results = results;
}

main();