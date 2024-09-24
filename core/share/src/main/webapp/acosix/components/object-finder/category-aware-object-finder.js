/*
 * Copyright 2016 - 2024 Acosix GmbH
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
            categoryBreadcrumbSeparator : ' > ',
            multiTieredClassification: true
        });

        return this;
    };

    YAHOO
            .extend(
                    Acosix.CategoryAwareObjectFinder,
                    Alfresco.ObjectFinder,
                    {
                    
                        _createNavigationControls: function Acosix_utility_CategoryAwareObjectFinder__createNavigationControls()
                        {
                            var zInput;
                            if (this._inCategoryMode())
                            {
                                // enable search
                                Dom.setStyle(this.pickerId + '-searchContainer', 'display', 'block');

                                this.widgets.searchButton = new YAHOO.widget.Button(this.pickerId + '-searchButton');
                                this.widgets.searchButton.on('click', this.onSearch, this.widgets.searchButton, this);

                                Dom.get(this.pickerId + '-searchButton').name = '-';

                                zInput = Dom.get(this.pickerId + '-searchText');
                                new YAHOO.util.KeyListener(zInput, 
                                {
                                    keys: 13
                                }, 
                                {
                                    fn: this.onSearch,
                                    scope: this,
                                    correctScope: true
                                }, 'keydown').enable();
                                
                                if (this.options.multiTieredClassification)
                                {
                                    // delegation to superclass will enables folderUp/navigator
                                    Acosix.CategoryAwareObjectFinder.superclass._createNavigationControls.apply(this, arguments);
                                }
                                else
                                {
                                    Dom.setStyle(this.pickerId + '-folderUpContainer', 'display', 'none');
                                    Dom.setStyle(this.pickerId + '-navigatorContainer', 'display', 'none');
                                }
                            }
                            else
                            {
                                Acosix.CategoryAwareObjectFinder.superclass._createNavigationControls.apply(this, arguments);
                            }
                        },
                        
                        _inCategoryMode: function Acosix_utility_CategoryAwareObjectFinder__inCategoryMode()
                        {
                            return (this.options.itemFamily === 'category');
                        },

                        _fireRefreshEvent: function Acosix_utility_CategoryAwareObjectFinder__fireRefreshEvent()
                        {
                            var searchTerm;
                            if (this._inCategoryMode())
                            {
                                searchTerm = Dom.get(this.pickerId + '-searchText').value;
                                if (searchTerm)
                                {
                                    if (searchTerm.length >= this.options.minSearchTermLength)
                                    {
                                        YAHOO.Bubbling.fire('refreshItemList',
                                        {
                                            eventGroup: this,
                                            searchTerm: searchTerm
                                        });
                                    }
                                    else
                                    {
                                        Dom.get(this.pickerId + '-searchText').focus();
                                    }
                                }
                                else
                                {
                                    YAHOO.Bubbling.fire('refreshItemList',
                                    {
                                        eventGroup: this
                                    });
                                }
                            }
                            else
                            {
                                Acosix.CategoryAwareObjectFinder.superclass._fireRefreshEvent.apply(this, arguments);
                            }
                        },
                        
                        onParentChanged: function Acosix_utility_CategoryAwareObjectFinder_onParentChanged(layer, args)
                        {
                            if ($hasEventInterest(this, args) && this._inCategoryMode())
                            {
                                Dom.get(this.pickerId + '-searchText').value = '';
                            }
                            Acosix.CategoryAwareObjectFinder.superclass.onParentChanged.call(this, layer, args);
                        },

                        onSearch: function Acosix_utility_CategoryAwareObjectFinder_onSearch()
                        {
                            var searchTerm = YAHOO.lang.trim(Dom.get(this.pickerId + '-searchText').value);

                            // special case - revert to normal listing
                            if (this._inCategoryMode() && searchTerm.length === 0)
                            {
                                YAHOO.Bubbling.fire('refreshItemList',
                                {
                                    eventGroup: this
                                });
                            }
                            else
                            {
                                Acosix.CategoryAwareObjectFinder.superclass.onSearch.apply(this, arguments);
                            }
                        },

                        onRenderCurrentValue : function Acosix_utility_CategoryAwareObjectFinder_onRenderCurrentValue(layer, args)
                        {
                            var items, displayValue, item, link, l, key, lastPathSepIdx, template;

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
                                                    lastPathSepIdx = item.displayPath.indexOf('/', 12);
                                                    if (lastPathSepIdx !== -1)
                                                    {
                                                        item.breadcrumb = item.displayPath.substring(lastPathSepIdx + 1) + '/';
                                                    }
                                                    else
                                                    {
                                                        item.breadcrumb = '';
                                                    }
                                                    item.breadcrumb += item.name;
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
