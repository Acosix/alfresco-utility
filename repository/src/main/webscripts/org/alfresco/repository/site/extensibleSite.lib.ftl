<#import "site.lib.ftl" as siteLib/>
<#macro siteJSON site>
    <@siteLib.siteJSONManagers site=site roles="managers">
        <#nested />
    </@>
</#macro>