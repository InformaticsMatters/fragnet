/*
 * Copyright (c) 2019 Informatics Matters Ltd.
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
package org.squonk.fragnet.service;

import org.apache.camel.builder.RouteBuilder;

import java.util.logging.Logger;

/**
 * Defines the generic REST services and the REST configuration that is used by the version specific APIs.
 */
public class FragnetSearchRouteBuilderCommon extends RouteBuilder {

    private static final Logger LOG = Logger.getLogger(FragnetSearchRouteBuilderCommon.class.getName());

    static {
        // This is necessary to load the RDKit libraries.
        // Needs to be done once somewhere before any RDKit code is called.
        LOG.info("Loading RDKit libraries");
        System.loadLibrary("GraphMolWrap");
    }

    @Override
    public void configure() throws Exception {


        restConfiguration().component("servlet").host("0.0.0.0");
//                .apiContextPath("/api-doc")
//                .apiProperty("api.title", "Fragnet services").apiProperty("api.version", "1.0")
//                .apiProperty("cors", "true");

        /* These are the REST endpoints - exposed as public web services

        Test like this:
        curl "http://localhost:8080/fragnet-search/rest/ping"
        */
        rest().get("/ping").description("Simple ping service to check things are running")
                .produces("text/plain")
                .route()
                .transform(constant("OK\n"))
                .endRest()
                .get("/versions").description("List API versions that are available")
                .produces("application/json")
                .route()
                .transform(constant("[\"/fragnet-search/rest/v1\",\"/fragnet-search/rest/v2\"]"))
                .endRest();

    }
}
