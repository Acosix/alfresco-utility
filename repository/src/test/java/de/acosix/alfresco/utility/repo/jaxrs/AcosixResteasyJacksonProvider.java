/*
 * Copyright 2016, 2017 Acosix GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.acosix.alfresco.utility.repo.jaxrs;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJacksonProvider;

/**
 * @author Axel Faust, <a href="http://acosix.de">Acosix GmbH</a>
 */
@Provider
@Consumes({ "application/*+json", "text/json" })
@Produces({ "application/*+json", "text/json" })
public class AcosixResteasyJacksonProvider extends ResteasyJacksonProvider
{

    public AcosixResteasyJacksonProvider()
    {
        super();

        final ObjectMapper configuredMapper = this._mapperConfig.getConfiguredMapper();
        final ObjectMapper defaultMapper = this._mapperConfig.getDefaultMapper();

        final ObjectMapper effectiveMapper = configuredMapper != null ? configuredMapper : defaultMapper;
        effectiveMapper.setSerializationInclusion(Inclusion.NON_EMPTY);
    }
}
