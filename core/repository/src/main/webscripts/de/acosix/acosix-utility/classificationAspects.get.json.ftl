<#compress><#escape x as jsonUtils.encodeJSONString(x)>
{
    "aspects" : [<#list aspects as aspect>"${aspect}"<#if aspect_has_next>,</#if></#list>]
}
</#escape></#compress>