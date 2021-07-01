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

   var ${picker} = new Acosix.CategoryAwareObjectFinder("${controlId}", "${fieldHtmlId}").setOptions(
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
      itemType: "cm:category",
      multipleSelectMode: ${(field.control.params.multipleSelectMode!true)?string},
      parentNodeRef: "${field.control.params.parentNodeRef!"alfresco://category/root"}",
      itemFamily: "category",
      maintainAddedRemovedItems: false,
      params: "${field.control.params.params!""}",
      createNewItemUri: "${field.control.params.createNewItemUri!}",
      createNewItemIcon: "${field.control.params.createNewItemIcon!}",
      categoryBreadcrumbSeparator: "${field.control.params.categoryBreadcrumbSeparator!" > "}",
      finderAPI: Alfresco.constants.PROXY_URI + '${field.control.params.finderAPI!'acosix/api/forms/picker/{itemFamily}'}',
      multiTieredClassification: ${((field.control.params.multiTieredClassification!'true') == 'true')?string('true', 'false')}
   });
})();
//]]></script>

<div class="form-field inlineable">
   <#if form.mode == "view">
      <div id="${controlId}" class="viewmode-field inlineable">
         <#if (field.mandatory!false) && (field.value == "")>
            <span class="incomplete-warning"><img src="${url.context}/res/components/form/images/warning-16.png" title="${msg("form.field.incomplete")}" /></span>
         </#if>
         <#if field.label != ""><span class="viewmode-label">${field.label?html}:</span></#if>
         <span id="${controlId}-currentValueDisplay" class="viewmode-value current-values"></span>
      </div>
   <#else>
      <#if field.label != "">
      <label for="${controlId}">${field.label?html}:<#if field.mandatory!false><span class="mandatory-indicator">${msg("form.required.fields.marker")}</span></#if></label>
      </#if>
      
      <div id="${controlId}" class="object-finder inlineable">
         
         <div id="${controlId}-currentValueDisplay" class="current-values inlineable"></div>
         
         <#if field.disabled == false>
            <input type="hidden" id="${fieldHtmlId}" name="${field.name}" value="${field.value?html}" />
            <input type="hidden" id="${fieldHtmlId}_isCategory" name="${field.name}_isCategory" value="true" />
            <div id="${controlId}-itemGroupActions" class="show-picker inlineable"></div>
            
            <#if field.control.params.showSubCategoriesOption?? && field.control.params.showSubCategoriesOption == "true">
               <div class="subcats-option">
                  <input type="checkbox" name="${field.name}_usesubcats" value="true" checked="true" />&nbsp;${msg("form.control.category.include.subcats")}
               </div>
            </#if>
            
            <#if field.control.params.mode?? && isValidMode(field.control.params.mode?upper_case)>
               <input id="${fieldHtmlId}-mode" type="hidden" name="${field.name}-mode" value="${field.control.params.mode?upper_case}" />
            </#if>
            
            <@renderPickerHTML controlId />
         </#if>
      </div>
   </#if>
</div>

<#function isValidMode modeValue>
   <#return modeValue == "OR" || modeValue == "AND">
</#function>