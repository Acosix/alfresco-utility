/*
 * Copyright 2016 - 2021 Acosix GmbH
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
    var Dom = YAHOO.util.Dom;

    Acosix.AuthorityObjectFinder = function Acosix_utility_AuthorityObjectFinder(htmlId, currentValueHtmlId)
    {
        Acosix.AuthorityObjectFinder.superclass.constructor.call(this, htmlId, currentValueHtmlId);

        this.name = 'Acosix.AuthorityObjectFinder';
        Alfresco.util.ComponentManager.reregister(this);

        this.setOptions({
            allowEmptySearch: false
        });

        return this;
    };

    YAHOO
            .extend(
                    Acosix.AuthorityObjectFinder,
                    Alfresco.ObjectFinder,
                    {
                        _fireRefreshEvent: function Acosix_utility_AuthorityObjectFinder__fireRefreshEvent()
                        {
                            var searchTerm;
                            if (this._inAuthorityMode())
                            {
                                searchTerm = Dom.get(this.pickerId + '-searchText').value;
                                if (searchTerm)
                                {
                                    if (this.options.allowEmptySearch || searchTerm.length >= this.options.minSearchTermLength)
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
                                else if (this.options.allowEmptySearch)
                                {
                                    YAHOO.Bubbling.fire('refreshItemList',
                                    {
                                        eventGroup: this
                                    });
                                }
                                else
                                {
                                    Dom.get(this.pickerId + '-searchText').focus();
                                }
                            }
                            else
                            {
                                Acosix.AuthorityObjectFinder.superclass._fireRefreshEvent.apply(this, arguments);
                            }
                        },
                    });
}());
