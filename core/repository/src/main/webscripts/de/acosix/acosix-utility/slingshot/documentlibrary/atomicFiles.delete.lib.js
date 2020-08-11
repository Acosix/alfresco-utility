/**
 * Entrypoint required by action.lib.js
 * 
 * @method runAction
 * @param p_params
 *            {object} Object literal containing files array
 * @return {object|null} object representation of action results
 */
/* exported runAction */
function runAction(p_params)
{
    var files, results, linkNodes, nodes, file, nodeRef, node, i;

    files = p_params.files;
    if (!files || files.length === 0)
    {
        status.setCode(status.STATUS_BAD_REQUEST, 'No files.');
        return;
    }

    results = [];
    linkNodes = [];
    nodes = [];
    for (file in files)
    {
        if (files.hasOwnProperty(file))
        {
            try
            {
                nodeRef = files[file];
                node = search.findNode(nodeRef);

                if (node === null)
                {
                    results.push({
                        id : file,
                        nodeRef : nodeRef,
                        action : 'deleteFile',
                        success : false
                    });
                    continue;
                }
                else if (node.isLinkToDocument || node.isLinkToContainer)
                {
                    linkNodes.push(node);
                }
                else
                {
                    nodes.push(node);
                }
            }
            catch (e)
            {
                status.setCode(status.STATUS_INTERNAL_SERVER_ERROR, e.toString());
                return;
            }
        }
    }

    nodes = linkNodes.concat(nodes);

    for (i = 0; i < nodes.length; i++)
    {
        results.push({
            id : nodes[i].name,
            nodeRef : nodes[i].nodeRef.toString(),
            action : 'deleteFile',
            type : nodes[i].isContainer ? 'folder' : 'document',
            success : nodes[i].remove(true)
        });
    }

    return results;
}
