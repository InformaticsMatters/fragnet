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
package org.squonk.fragnet.service.v2;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestParamType;
import org.neo4j.driver.v1.Session;
import org.squonk.fragnet.chem.Calculator;
import org.squonk.fragnet.search.model.v2.Availability;
import org.squonk.fragnet.search.model.v2.NeighbourhoodGraph;
import org.squonk.fragnet.search.queries.v2.AvailabilityQuery;
import org.squonk.fragnet.search.queries.v2.SimpleNeighbourhoodQuery;
import org.squonk.fragnet.search.queries.v2.SuppliersQuery;
import org.squonk.fragnet.service.AbstractFragnetSearchRouteBuilder;
import org.squonk.fragnet.service.GraphDB;

import javax.inject.Inject;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Defines the v2 REST APIs
 */
public class FragnetSearchRouteBuilder extends AbstractFragnetSearchRouteBuilder {

    private static final Logger LOG = Logger.getLogger(FragnetSearchRouteBuilder.class.getName());

    @Inject
    private GraphDB graphdb;

    private List<Map<String, String>> suppliers;
    private Map<String, String> supplierMappings;

    public FragnetSearchRouteBuilder() {
        this(true);
    }

    public FragnetSearchRouteBuilder(boolean writeQueryLog) {
        super(writeQueryLog ? "fragnet-queries-v2.log" : null);
    }

    @Override
    public void configure() throws Exception {

        //These are the v2 REST endpoints - exposed as public web services
        //
        // test like this:
        // curl "http://localhost:8080/fragnet-search/rest/v2/ping"
        rest("/v2/ping").description("Simple ping service to check things are running")
                .get()
                .produces("text/plain")
                .route()
                .transform(constant("OK\n"))
                .endRest();


        // example:
        // curl "$FRAGNET_SERVER/fragnet-search/rest/v2/search/neighbourhood/c1ccc%28Nc2nc3ccccc3o2%29cc1?hac=3&rac=1&hops=2&calcs=LOGP,SIM_RDKIT_TANIMOTO"
        rest("/v2/search/").description("Fragnet search")
                .bindingMode(RestBindingMode.json)
                // service descriptor
                .get("neighbourhood/{smiles}").description("Neighbourhood search")
                .param().name("smiles").type(RestParamType.path).description("SMILES query").endParam()
                .param().name("hac").type(RestParamType.query).description("Heavy atom count bounds").endParam()
                .param().name("rac").type(RestParamType.query).description("Ring atom count bounds").endParam()
                .param().name("hops").type(RestParamType.query).description("Number of edge traversals").endParam()
                .param().name("calcs").type(RestParamType.query).description("Calculations to execute").endParam()
                .param().name("suppliers").type(RestParamType.query).description("Suppliers to include").endParam()
                .param().name("limit").type(RestParamType.query).description("Limit parameter for the number of paths to return from the graph query").endParam()
                .produces("application/json")
                .route()
                .process((Exchange exch) -> {
                    executeNeighbourhoodQuery(exch);
                })
                .endRest()
                .get("suppliers").description("List the available suppliers")
                .produces("application/json")
                .route()
                .process((Exchange exch) -> {
                    executeSuppliersQuery(exch);
                })
                .endRest()
                .get("availability/{smiles}").description("Get molecule availability")
                .param().name("smiles").type(RestParamType.path).description("SMILES query").endParam()
                .produces("application/json")
                .route()
                .process((Exchange exch) -> {
                    executeAvailabilityQuery(exch);
                })
                .endRest()
        ;
    }

    void executeAvailabilityQuery(Exchange exch) {

        Message message = exch.getIn();
        String smiles = message.getHeader("smiles", String.class);

        try {
            Availability availability = getAvailability(smiles);
            if (availability == null || availability.getItems().size() == 0) {
                message.setBody("{\"error\": \"Query Failed\",\"message\",\"SMILES not found\"}");
                message.setHeader(Exchange.HTTP_RESPONSE_CODE, 404);
            } else {
                message.setBody(availability);
                message.setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Query Failed", ex);
            message.setBody("{\"error\": \"Query Failed\",\"message\",\"" + ex.getLocalizedMessage() + "\"}");
            message.setHeader(Exchange.HTTP_RESPONSE_CODE, 500);
        }
    }

    private Availability getAvailability(String smiles) {

        if (smiles == null || smiles.isEmpty()) {
            throw new IllegalArgumentException("No SMILES specified");
        }

        long t0 = new Date().getTime();
        try (Session session = graphdb.getSession()) {
            AvailabilityQuery query = new AvailabilityQuery(session);
            Availability availability = query.getAvailability(smiles);
            long t1 = new Date().getTime();
            LOG.fine("Availability query took " + (t1 - t0) + "ms");
            return availability;
        }
    }

    /**
     * Suppliers are read once the first time they are needed.
     * It is assumed that the database will not change.
     *
     * @param exch
     */
    void executeSuppliersQuery(Exchange exch) {

        Message message = exch.getIn();

        try {
            List<Map<String, String>> suppliers = getSuppliers();
            message.setBody(suppliers);
            message.setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Query Failed", ex);
            message.setBody("{\"error\": \"Query Failed\",\"message\",\"" + ex.getLocalizedMessage() + "\"}");
            message.setHeader(Exchange.HTTP_RESPONSE_CODE, 500);
        }
    }

    private List<Map<String, String>> getSuppliers() {
        if (suppliers == null) {
            long t0 = new Date().getTime();
            try (Session session = graphdb.getSession()) {
                SuppliersQuery query = new SuppliersQuery(session);
                suppliers = query.getSuppliers();
            }
            long t1 = new Date().getTime();
            LOG.info("Suppliers query took " + (t1 - t0) + "ms");
            supplierMappings = new HashMap<>();
            suppliers.forEach((m) -> supplierMappings.put(m.get("name"), m.get("label")));
        }
        return suppliers;
    }

    private Map<String, String> getSupplierMappings() {
        if (suppliers == null || supplierMappings == null) {
            getSuppliers();
        }
        return supplierMappings;
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
            String suppls = message.getHeader("suppliers", String.class);
            String calcs = message.getHeader("calcs", String.class);
            LOG.info(String.format("hops=&s hac=%s rac=%s calcs=%s", hops, hac, rac, calcs));

            List<Calculator.Calculation> calculations = parseCalculations(calcs);
            List<String> suppliers = parseSuppliers(suppls);

            NeighbourhoodGraph result;
            try (Session session = graphdb.getSession()) {
                // execute the query
                SimpleNeighbourhoodQuery query = new SimpleNeighbourhoodQuery(session, getSupplierMappings());
                if (limit != null) { // default limit is 1000
                    query.setLimit(limit);
                }
                result = query.executeNeighbourhoodQuery(smilesQuery, hops, hac, rac, suppliers);
            }
            // if calculations have been specified then calculate them
            if (!calculations.isEmpty()) {
                LOG.info("Running " + calculations.size() + " calculations");
                result.calculate(result.getRefmol(), calculations.toArray(new Calculator.Calculation[calculations.size()]));
            }

            message.setBody(result);
            message.setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
            long t1 = new Date().getTime();
            writeToQueryLog(username, "NeighbourhoodQuery", t1 - t0, result.getNodeCount(), result.getEdgeCount(), result.getGroupCount());

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Query Failed", ex);
            message.setBody("{\"error\": \"Query Failed\",\"message\",\"" + ex.getLocalizedMessage() + "\"}");
            message.setHeader(Exchange.HTTP_RESPONSE_CODE, 500);

            long t1 = new Date().getTime();
            writeErrorToQueryLog(username, "NeighbourhoodQuery", t1 - t0, ex.getLocalizedMessage());
        }
    }

}
