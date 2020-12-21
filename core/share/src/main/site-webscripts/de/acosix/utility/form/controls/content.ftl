<#include "/org/alfresco/components/form/controls/common/editorparams.inc.ftl" />

<#if field.control.params.rows??>
    <#assign rows=field.control.params.rows />
<#else>
    <#assign rows=12 />
</#if>
<#if field.control.params.columns??>
    <#assign columns=field.control.params.columns />
<#else>
    <#assign columns=60 />
</#if>

<#assign jsDisabled=(form.capabilities?? && form.capabilities.javascript?? && form.capabilities.javascript == false) />

<#macro renderEditorOptions field><#compress>
    <#assign lastGroupKey = "" />
    <#if field.control.params??>
        <#list field.control.params?keys?sort as key>
            <#if key?matches("[a-z][a-zA-Z0-9]+Options\\.[a-z][a-zA-Z0-9]+")>
                <#assign groupKey = key?substring(0, key?index_of(".")) />
                <#assign optionKey = key?substring(key?index_of(".") + 1) />
                <#if groupKey != lastGroupKey>
                    <#if lastGroupKey != "">
                    }
                    </#if>
                    , ${groupKey?js_string}: {
                    <#assign lastGroupKey = groupKey />
                <#else>
                ,
                </#if>
                ${optionKey?js_string}: "${field.control.params[key]?js_string}"
            </#if>
        </#list>
        <#if lastGroupKey != "">
        }
        </#if>
    </#if>
</#compress></#macro>

<#if form.mode != "view">
<div class="form-field" id="${fieldHtmlId}-field">
    <#if jsDisabled == false>
        <script type="text/javascript">//<![CDATA[
            (function() {
                new Acosix.formControls.Content("${fieldHtmlId}").setOptions( {
                    <#if form.mode == "view" || (field.disabled && !(field.control.params.forceEditable?? && field.control.params.forceEditable == "true"))>disabled: true,</#if>
                    fieldName : "${field.name?js_string}",
                    currentValue: "${field.value?js_string}",
                    mandatory: ${field.mandatory?string},
                    formMode: "${form.mode}",
                    <#if context.properties.nodeRef??>
                        nodeRef: "${context.properties.nodeRef?js_string}",
                    <#elseif form.mode == "edit" && args.itemId??>
                        nodeRef: "${args.itemId?js_string}",
                    <#else>
                        nodeRef: "",
                    </#if>
                    mimeType: "${(context.properties.mimeType!"")?js_string}",
                    <#if field.control.params.forceEditor??>forceEditor: ${field.control.params.forceEditor},</#if>
                    <#if field.control.params.forceContent??>forceContent: ${field.control.params.forceContent},</#if>
                    <@editorParameters field />
                    <@renderEditorOptions field />
                }).setMessages(${messages});
            })();
        //]]></script>
    </#if>

    <#if field.label != ""><label for="${fieldHtmlId}">${field.label?html}:<#if field.mandatory><span class="mandatory-indicator">${msg("form.required.fields.marker")}</span></#if></label></#if>
    <textarea id="${fieldHtmlId}" name="${field.name}" rows="${rows}" columns="${columns}" tabindex="0"
        <#if field.description??>title="${field.description?html}"</#if>
        <#if field.control.params.styleClass??>class="${field.control.params.styleClass}"</#if>
        <#if field.control.params.style??>style="${field.control.params.style}"</#if>
        <#if field.disabled && !(field.control.params.forceEditable?? && field.control.params.forceEditable == "true")>disabled="true"</#if>>${(field.content!"")?html}</textarea>
</div>
</#if>