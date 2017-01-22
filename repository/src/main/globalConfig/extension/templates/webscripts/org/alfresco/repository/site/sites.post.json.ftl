<#import "extensibleSite.lib.ftl" as siteLib/>
<@siteLib.siteJSON site=site>
    <@markup id="siteCustomJSONProperties"><#escape x as jsonUtils.encodeJSONString(x)>
        "nodeRef" : "${site.node.storeType}://${site.node.storeId}/${site.node.id}",
    </#escape></@>
</@>