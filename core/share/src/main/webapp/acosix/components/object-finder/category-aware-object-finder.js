/*
 * Copyright 2016 - 2019 Acosix GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* jshint -W003 */
if (typeof Acosix === 'undefined' || !Acosix)
{
    var Acosix = {};
}
/* jshint +W003 */

(function()
{
    var Dom, $html, $hasEventInterest;

    Dom = YAHOO.util.Dom;
    $html = Alfresco.util.encodeHTML;
    $hasEventInterest = Alfresco.util.hasEventInterest;

    Acosix.CategoryAwareObjectFinder = function Acosix_utility_CategoryAwareObjectFinder(htmlId, currentValueHtmlId)
    {
        Acosix.CategoryAwareObjectFinder.superclass.constructor.call(this, htmlId, currentValueHtmlId);

        this.name = 'Acosix.CategoryAwareObjectFinder';
        Alfresco.util.ComponentManager.reregister(this);

        this.setOptions({
            categoryBreadcrumbSeparator : ' > '
        });

        return this;
    };

    YAHOO
            .extend(
                    Acosix.CategoryAwareObjectFinder,
                    Alfresco.ObjectFinder,
                    {

                        onRenderCurrentValue : function Acosix_utility_CategoryAwareObjectFinder_onRenderCurrentValue(layer, args)
                        {
                            var items, displayValue, item, link, l, key, template;

                            if ($hasEventInterest(this, args))
                            {
                                this._adjustCurrentValues();

                                items = this.selectedItems;
                                displayValue = '';

                                if (items === null)
                                {
                                    displayValue = '<span class="error">' + this.msg('form.control.object-picker.current.failure')
                                            + '</span>';
                                }
                                else
                                {
                                    if (this.options.displayMode === 'list')
                                    {
                                        l = this.widgets.currentValuesDataTable.getRecordSet().getLength();
                                        if (l > 0)
                                        {
                                            this.widgets.currentValuesDataTable.deleteRows(0, l);
                                        }
                                    }

                                    for (key in items)
                                    {
                                        if (items.hasOwnProperty(key))
                                        {
                                            item = items[key];

                                            if (item.type === 'cm:category')
                                            {
                                                if (item.displayPath.indexOf('/categories/Tags') !== -1)
                                                {
                                                    item.type = 'tag';
                                                }
                                                else
                                                {
                                                    // 12 - length of /categories/ path prefix
                                                    // so we aim to find the path below the specific classification
                                                    item.breadcrumb = item.displayPath.substring(item.displayPath.indexOf('/', 12) + 1);
                                                    item.breadcrumb += '/' + item.name;
                                                    item.breadcrumb = item.breadcrumb.split('/').join(
                                                            this.options.categoryBreadcrumbSeparator);
                                                }
                                            }

                                            if (this.options.showLinkToTarget && this.options.targetLinkTemplate !== null)
                                            {
                                                if (this.options.displayMode === 'items')
                                                {
                                                    link = null;
                                                    if (YAHOO.lang.isFunction(this.options.targetLinkTemplate))
                                                    {
                                                        link = this.options.targetLinkTemplate.call(this, item);
                                                    }
                                                    else
                                                    {
                                                        var linkTemplate = (item.site) ? Alfresco.constants.URL_PAGECONTEXT
                                                                + 'site/{site}/document-details?nodeRef={nodeRef}'
                                                                : Alfresco.constants.URL_PAGECONTEXT + 'document-details?nodeRef={nodeRef}';
                                                        link = YAHOO.lang.substitute(linkTemplate, {
                                                            nodeRef : item.nodeRef,
                                                            site : item.site
                                                        });
                                                    }

                                                    template = '<div>{icon} <a href="{link}">{name}</a></div>';
                                                    if (item.type === 'cm:category')
                                                    {
                                                        template = '<div>{icon} <a href="{link}">{breadcrumb}</a></div>';
                                                    }

                                                    displayValue += this.options.objectRenderer.renderItem(item, 16, template.replace(
                                                            '{link}', link));
                                                }
                                                else if (this.options.displayMode === 'list')
                                                {
                                                    this.widgets.currentValuesDataTable.addRow(item);
                                                }
                                            }
                                            else
                                            {
                                                if (this.options.displayMode === 'items')
                                                {
                                                    if (item.type === 'tag')
                                                    {
                                                        displayValue += this.options.objectRenderer.renderItem(item, null,
                                                                '<div class="itemtype-tag">{name}</div>');
                                                    }
                                                    else
                                                    {
                                                        template = '<div class="itemtype-{type}" style="word-wrap: break-word;">{icon} {name}</div>';

                                                        if (item.type === 'cm:category')
                                                        {
                                                            template = '<div class="itemtype-{type}" style="word-wrap: break-word;">{icon} {breadcrumb}</div>';
                                                        }

                                                        displayValue += this.options.objectRenderer.renderItem(item, 16, template.replace(
                                                                '{link}', link));
                                                    }
                                                }
                                                else if (this.options.displayMode === 'list')
                                                {
                                                    this.widgets.currentValuesDataTable.addRow(item);
                                                }
                                            }
                                        }
                                    }
                                    if (this.options.displayMode === 'items')
                                    {
                                        Dom.get(this.id + '-currentValueDisplay').innerHTML = displayValue;
                                    }
                                }
                                this._enableActions();
                            }
                        }
                    });
}());
