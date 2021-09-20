<#include "/org/alfresco/components/form/controls/common/picker.inc.ftl" />

<#assign controlId = fieldHtmlId + "-cntrl">

<#-- cannot use @renderPickerJS from picker.inc.ftl which hardcodes the widget to one of two variants of ObjectFinder -->
<#macro renderPickerJS field picker="picker" cloud=false>
   <#if field.control.params.selectedValueContextProperty??>
      <#if context.properties[field.control.params.selectedValueContextProperty]??>
         <#local renderPickerJSSelectedValue = context.properties[field.control.params.selectedValueContextProperty]>
      <#elseif args[field.control.params.selectedValueContextProperty]??>
         <#local renderPickerJSSelectedValue = args[field.control.params.selectedValueContextProperty]>
      <#elseif context.properties[field.control.params.selectedValueContextProperty]??>
         <#local renderPickerJSSelectedValue = context.properties[field.control.params.selectedValueContextProperty]>
      </#if>
   </#if>

   var ${picker} = new Acosix.AuthorityObjectFinder("${controlId}", "${fieldHtmlId}").setOptions(
   {
      <#if form.mode == "view" || (field.disabled && !(field.control.params.forceEditable?? && field.control.params.forceEditable == "true"))>disabled: true,</#if>
      field: "${field.name}",
      customFolderStyleConfig: <#if customFolderStyleConfig??>${(customFolderStyleConfig!"")}<#else>null</#if>,
      compactMode: ${compactMode?string},
   <#if field.mandatory??>
      mandatory: ${field.mandatory?string},
   <#elseif field.endpointMandatory??>
      mandatory: ${field.endpointMandatory?string},
   </#if>
   <#if field.control.params.startLocation??>
      startLocation: "${field.control.params.startLocation}",
      <#if form.mode == "edit" && args.itemId??>currentItem: "${args.itemId?js_string}",</#if>
      <#if form.mode == "create" && form.destination?? && form.destination?length &gt; 0>currentItem: "${form.destination?js_string}",</#if>
   </#if>
   <#if field.control.params.startLocationParams??>
      startLocationParams: "${field.control.params.startLocationParams?js_string}",
   </#if>
      currentValue: "${field.value?js_string}",
      <#if field.control.params.valueType??>valueType: "${field.control.params.valueType}",</#if>
      <#if renderPickerJSSelectedValue??>selectedValue: "${renderPickerJSSelectedValue}",</#if>
      <#if field.control.params.selectActionLabelId??>selectActionLabelId: "${field.control.params.selectActionLabelId}",</#if>
      selectActionLabel: "${field.control.params.selectActionLabel!msg("button.select")}",
      minSearchTermLength: ${field.control.params.minSearchTermLength!'1'},
      maxSearchResults: ${field.control.params.maxSearchResults!'100'}
   }).setMessages(
      ${messages}
   );
</#macro>

<script type="text/javascript">//<![CDATA[
(function()
{

   <@renderPickerJS field "picker" />
   picker.setOptions(
   {
      itemType: "${field.endpointType}",
      multipleSelectMode: ${field.endpointMany?string},
      itemFamily: "authority",
      finderAPI: Alfresco.constants.PROXY_URI + '${field.control.params.finderAPI!'acosix/api/forms/picker/{itemFamily}'}',
      allowEmptySearch: ${(field.control.params.allowEmptySearch!'false' == 'true')?string}
   });
})();
//]]></script>

<div class="form-field">
   <#if form.mode == "view">
      <div id="${controlId}" class="viewmode-field">
         <#if field.endpointMandatory && field.value == "">
            <span class="incomplete-warning"><img src="${url.context}/res/components/form/images/warning-16.png" title="${msg("form.field.incomplete")}" /><span>
         </#if>
         <span class="viewmode-label">${field.label?html}:</span>
         <span id="${controlId}-currentValueDisplay" class="viewmode-value current-values"></span>
      </div>
   <#else>
      <label for="${controlId}">${field.label?html}:<#if field.endpointMandatory><span class="mandatory-indicator">${msg("form.required.fields.marker")}</span></#if></label>
      
      <div id="${controlId}" class="object-finder">
         
         <div id="${controlId}-currentValueDisplay" class="current-values"></div>
         
         <#if field.disabled == false>
            <input type="hidden" id="${fieldHtmlId}" name="-" value="${field.value?html}" />
            <input type="hidden" id="${controlId}-added" name="${field.name}_added" />
            <input type="hidden" id="${controlId}-removed" name="${field.name}_removed" />
            <div id="${controlId}-itemGroupActions" class="show-picker"></div>

            <@renderPickerHTML controlId />
         </#if>
      </div>
   </#if>
</div>