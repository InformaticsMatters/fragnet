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
package org.squonk.fragnet.service.v1;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestParamType;
import org.neo4j.driver.Session;
import org.squonk.fragnet.chem.Calculator;
import org.squonk.fragnet.search.model.v1.NeighbourhoodGraph;
import org.squonk.fragnet.search.queries.v1.Query;
import org.squonk.fragnet.service.AbstractFragnetSearchRouteBuilder;
import org.squonk.fragnet.service.GraphDB;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Defines the v1 REST APIs
 *
 */
public class FragnetSearchRouteBuilder extends AbstractFragnetSearchRouteBuilder {

    private static final Logger LOG = Logger.getLogger(FragnetSearchRouteBuilder.class.getName());

    @Inject
    private GraphDB graphdb;

    public FragnetSearchRouteBuilder() {
        this(true);
    }

    public FragnetSearchRouteBuilder(boolean writeQueryLog) {
        super(writeQueryLog ? "fragnet-queries-v1.log" : null);
    }

    @Override
    public void configure() throws Exception {

        //These are the v1 REST endpoints - exposed as public web services
        //
        // test like this:
        // curl "http://localhost:8080/fragnet-search/rest/v1/ping"
        rest("/v1/ping").description("Simple ping service to check things are running")
                .get()
                .produces("text/plain")
                .route()
                .transform(constant("OK\n"))
                .endRest();


        // example:
        // curl "http://localhost:8080/fragnet-search/rest/v1/search/neighbourhood/c1ccc%28Nc2nc3ccccc3o2%29cc1?hac=3&rac=1&hops=2&calcs=LOGP,SIM_RDKIT_TANIMOTO"
        rest("/v1/search/").description("Fragnet search")
                .bindingMode(RestBindingMode.json)
                // service descriptor
                .get("/neighbourhood/{smiles}").description("Neighbourhood search")
                .param().name("hac").type(RestParamType.query).description("Heavy atom count bounds").endParam()
                .param().name("rac").type(RestParamType.query).description("Ring atom count bounds").endParam()
                .param().name("hops").type(RestParamType.query).description("Number of edge traversals").endParam()
                .param().name("calcs").type(RestParamType.query).description("Calculations to execute").endParam()
                .param().name("limit").type(RestParamType.query).description("Limit parameter for the number of paths to return from the graph query").endParam()
                .bindingMode(RestBindingMode.json)
                .produces("application/json")
                .route()
                .process((Exchange exch) -> {
                    executeNeighbourhoodQuery(exch);
                })
                .endRest();
    }

    void executeNeighbourhoodQuery(Exchange exch) {

        Message message = exch.getIn();

//        message.getHeaders().forEach((k,v) -> {
//            System.out.println(k + " -> " +v);
//        });

        long t0 = new Date().getTime();
        String username = getUsername(exch);


        try {
            String smilesQuery = message.getHeader("smiles", String.class);
            if (smilesQuery == null || smilesQuery.isEmpty()) {
                throw new IllegalArgumentException("Smiles must be specified");
            }
            Integer hops = message.getHeader("hops", Integer.class);
            Integer hac = message.getHeader("hac", Integer.class);
            Integer rac = message.getHeader("rac", Integer.class);
            Integer limit = message.getHeader("limit", Integer.class);
            if (limit != null && limit > 5000) {
                throw new IllegalArgumentException("Limit cannot be greater than 5000");
            }
            String calcs = message.getHeader("calcs", String.class);
            LOG.info(String.format("hops=&s hac=%s rac=%s calcs=%s", hops, hac, rac, calcs));

            List<Calculator.Calculation> calculations = parseCalculations(calcs);

            NeighbourhoodGraph result;
            try (Session session = graphdb.getSession()) {
                // execute the query
                Query query = new Query(session);
                if (limit != null) { // default limit is 1000
                    query.setLimit(limit);
                }
                result = query.executeNeighbourhoodQuery(smilesQuery, hops, hac, rac);
            }
            // if calculations have been specified then calculate them
            if (!calculations.isEmpty()) {
                LOG.info("Running " + calculations.size() + " calculations");
                result.calculate(result.getRefmol(), calculations.toArray(new Calculator.Calculation[calculations.size()]));
            }

            message.setBody(result);
            message.setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
            long t1 = new Date().getTime();
            writeToNeighbourhoodQueryLog(username, "NeighbourhoodQuery", t1 - t0, result.numNodes(), result.numEdges(), result.numGroups());

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "NeighbourhoodQuery Failed", ex);
            message.setBody("{\"error\": \"NeighbourhoodQuery Failed\",\"message\",\"" + ex.getLocalizedMessage() + "\"}");
            message.setHeader(Exchange.HTTP_RESPONSE_CODE, 500);

            long t1 = new Date().getTime();
            writeErrorToQueryLog(username, "NeighbourhoodQuery", t1 - t0, ex.getLocalizedMessage());
        }
    }

}
