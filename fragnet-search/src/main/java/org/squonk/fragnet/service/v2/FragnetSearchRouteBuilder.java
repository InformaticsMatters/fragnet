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

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.exporter.common.TextFormat;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestParamType;
import org.apache.camel.spi.DataFormat;
import org.neo4j.driver.v1.Session;
import org.squonk.fragnet.Constants;
import org.squonk.fragnet.chem.Calculator;
import org.squonk.fragnet.search.model.v2.Availability;
import org.squonk.fragnet.search.model.v2.ExpansionResults;
import org.squonk.fragnet.search.model.v2.NeighbourhoodGraph;
import org.squonk.fragnet.search.queries.AbstractQuery;
import org.squonk.fragnet.search.queries.v2.AvailabilityQuery;
import org.squonk.fragnet.search.queries.v2.ExpansionQuery;
import org.squonk.fragnet.search.queries.v2.NeighbourhoodQuery;
import org.squonk.fragnet.search.queries.v2.SuppliersQuery;
import org.squonk.fragnet.service.AbstractFragnetSearchRouteBuilder;
import org.squonk.fragnet.service.GraphDB;

import javax.inject.Inject;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;
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

    private final Counter neighbourhoodSearchRequestsTotal = Counter.build()
            .name("requests_neighbourhood_total")
            .help("Total number of neighbourhood search requests")
            .register();

    private final Counter neighbourhoodSearchErrorsTotal = Counter.build()
            .name("requests_neighbourhood_errors")
            .help("Total number of neighbourhood search errors")
            .register();

    private final Counter neighbourhoodSearchRequestsDuration = Counter.build()
            .name("duration_neighbourhood_total_ns")
            .help("Total duration of neighbourhood search requests")
            .register();

    private final Counter neighbourhoodSearchCalculationsDuration = Counter.build()
            .name("duration_neighbourhood_calculations_ns")
            .help("Total duration of calculations")
            .register();

    private final Counter neighbourhoodSearchNeo4jSearchDuration = Counter.build()
            .name("duration_neighbourhood_neo4j_ns")
            .help("Total duration of neighbourhood Neo4j cypher query")
            .register();

    private final Counter neighbourhoodSearchMCSDuration = Counter.build()
            .name("duration_neighbourhood_mcs_ns")
            .help("Total duration of mcs determination")
            .register();

    private final Counter neighbourhoodSearchHitsTotal = Counter.build()
            .name("results_neighbourhood_hits_molecules")
            .help("Total number of molecules found for neighbourhood search")
            .register();

    private final Counter expansionSearchRequestsTotal = Counter.build()
            .name("requests_expansion_total")
            .help("Total number of expansion search requests")
            .register();

    private final Counter expansionSearchErrorsTotal = Counter.build()
            .name("requests_expansion_errors")
            .help("Total number of expansion search errors")
            .register();

    private final Counter expansionSearchRequestsDuration = Counter.build()
            .name("duration_expansion_total_ns")
            .help("Total duration of expansion search requests")
            .register();

    private final Counter expansionSearchNeo4jSearchDuration = Counter.build()
            .name("duration_expansion_neo4j_ns")
            .help("Total duration of expansion Neo4j cypher query")
            .register();

    private final Counter expansionSearchHitsTotal = Counter.build()
            .name("results_expansion_hits_molecules")
            .help("Total number of molecules found for expansion search")
            .register();


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

        rest("/metrics").description("Prometheus metrics")
                .get()
                .produces("text/plain")
                .route()
                .process((Exchange exch) -> {
                    Message message = exch.getIn();
                    Writer writer = new StringWriter();
                    metrics(writer);
                    String s = writer.toString();
                    message.setBody(s);
                })
                .endRest();


        rest("/v2/search/").description("Fragnet search")
                .bindingMode(RestBindingMode.json)
                // example:
                // curl "$FRAGNET_SERVER/fragnet-search/rest/v2/search/neighbourhood/c1ccc%28Nc2nc3ccccc3o2%29cc1?hac=3&rac=1&hops=2&calcs=LOGP,SIM_RDKIT_TANIMOTO"
                .get("neighbourhood/{smiles}").description("Neighbourhood search")
                .param().name("smiles").type(RestParamType.path).description("SMILES query").endParam()
                .param().name("hac").type(RestParamType.query).description("Heavy atom count bounds").endParam()
                .param().name("rac").type(RestParamType.query).description("Ring atom count bounds").endParam()
                .param().name("hops").type(RestParamType.query).description("Number of edge traversals").endParam()
                .param().name("calcs").type(RestParamType.query).description("Calculations to execute").endParam()
                .param().name("suppliers").type(RestParamType.query).description("Suppliers to include").endParam()
                .param().name("pathLimit").type(RestParamType.query).description("Limit for the number of paths to return from the graph query").endParam()
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
                .get("calcs").description("List the available calculations")
                .produces("application/json")
                .route()
                .process((Exchange exch) -> {
                    findCalculations(exch);
                })
                .endRest()
                // example:
                // curl "$FRAGNET_SERVER/fragnet-search/rest/v2/search/expand/c1ccc%28Nc2nc3ccccc3o2%29cc1?hac=3&rac=1&hops=1"
                .get("expand/{smiles}").description("Expansion search")
                .bindingMode(RestBindingMode.off)
                .param().name("smiles").type(RestParamType.path).description("SMILES query").endParam()
                .param().name("hac").type(RestParamType.query).description("Heavy atom count bounds").endParam()
                .param().name("rac").type(RestParamType.query).description("Ring atom count bounds").endParam()
                .param().name("hops").type(RestParamType.query).description("Number of edge traversals").endParam()
                .param().name("suppliers").type(RestParamType.query).description("Suppliers to include").endParam()
                .param().name("pathLimit").type(RestParamType.query).description("Limit for the number of paths to return from the graph query").endParam()
                .produces("application/json")
                .route()
                .process((Exchange exch) -> {
                    executeExpansionQuery(exch, Constants.MIME_TYPE_SMILES);
                })
                .marshal().json(JsonLibrary.Jackson)
                .endRest()
                .post("expand").description("Expansion search")
                .bindingMode(RestBindingMode.off)
                .param().name("hac").type(RestParamType.query).description("Heavy atom count bounds").endParam()
                .param().name("rac").type(RestParamType.query).description("Ring atom count bounds").endParam()
                .param().name("hops").type(RestParamType.query).description("Number of edge traversals").endParam()
                .param().name("suppliers").type(RestParamType.query).description("Suppliers to include").endParam()
                .param().name("pathLimit").type(RestParamType.query).description("Limit for the number of paths to return from the graph query").endParam()
                .produces("application/json")
                .route()
                .process((Exchange exch) -> {
                    String contentType = exch.getIn().getHeader(Exchange.CONTENT_TYPE, Constants.MIME_TYPE_MOLFILE, String.class);
                    executeExpansionQuery(exch, contentType);
                })
                .marshal().json(JsonLibrary.Jackson)
                .endRest()
        ;
    }

    void findCalculations(Exchange exch) {

        Message message = exch.getIn();

        List<Map<String, String>> list = new ArrayList<>();
        for (Calculator.Calculation calc : Calculator.Calculation.values()) {
            Map<String, String> map = new LinkedHashMap<>();
            map.put("id", calc.toString());
            map.put("name", calc.propname);
            map.put("description", calc.description);
            map.put("type", calc.type);
            list.add(map);
        }

        message.setBody(list);
        message.setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
    }

    void executeAvailabilityQuery(Exchange exch) {

        Message message = exch.getIn();
        String smiles = message.getHeader("smiles", String.class);

        try {
            Availability availability = getAvailability(smiles);
            if (availability == null || availability.getItems().size() == 0) {
                message.setBody("{\"error\": \"NeighbourhoodQuery Failed\",\"message\",\"SMILES not found\"}");
                message.setHeader(Exchange.HTTP_RESPONSE_CODE, 404);
            } else {
                message.setBody(availability);
                message.setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "NeighbourhoodQuery Failed", ex);
            message.setBody("{\"error\": \"NeighbourhoodQuery Failed\",\"message\",\"" + ex.getLocalizedMessage() + "\"}");
            message.setHeader(Exchange.HTTP_RESPONSE_CODE, 500);
        }
    }

    private Availability getAvailability(String smiles) throws IOException {

        if (smiles == null || smiles.isEmpty()) {
            throw new IllegalArgumentException("No SMILES specified");
        }

        long t0 = System.nanoTime();
        try (Session session = graphdb.getSession()) {
            AvailabilityQuery query = new AvailabilityQuery(session);
            Availability availability = query.getAvailability(smiles);
            long t1 = System.nanoTime();
            LOG.fine("Availability query took " + (t1 - t0) + "ns");
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
            LOG.log(Level.SEVERE, "NeighbourhoodQuery Failed", ex);
            message.setBody("{\"error\": \"NeighbourhoodQuery Failed\",\"message\",\"" + ex.getLocalizedMessage() + "\"}");
            message.setHeader(Exchange.HTTP_RESPONSE_CODE, 500);
        }
    }

    private List<Map<String, String>> getSuppliers() throws IOException {
        if (suppliers == null) {
            long t0 = System.nanoTime();
            try (Session session = graphdb.getSession()) {
                SuppliersQuery query = new SuppliersQuery(session);
                suppliers = query.getSuppliers();
            }
            long t1 = System.nanoTime();
            LOG.info("Suppliers query took " + (t1 - t0) + "ns");
            supplierMappings = new HashMap<>();
            suppliers.forEach((m) -> supplierMappings.put(m.get("name"), m.get("label")));
        }
        return suppliers;
    }

    private Map<String, String> getSupplierMappings() throws IOException {
        if (suppliers == null || supplierMappings == null) {
            getSuppliers();
        }
        return supplierMappings;
    }

    void executeExpansionQuery(Exchange exch, String mimeType) {

        expansionSearchRequestsTotal.inc();

        Message message = exch.getIn();

//        message.getHeaders().forEach((k,v) -> {
//            System.out.println(k + " -> " +v);
//        });

        long t0 = System.nanoTime();
        String username = getUsername(exch);

        try {
            Integer hops = message.getHeader("hops", Integer.class);
            Integer hac = message.getHeader("hac", Integer.class);
            Integer rac = message.getHeader("rac", Integer.class);
            Integer pathLimit = message.getHeader("pathLimit", Integer.class);
            if (pathLimit != null && pathLimit > AbstractQuery.DEFAULT_LIMIT) {
                throw new IllegalArgumentException("Path limit cannot be greater than " + AbstractQuery.DEFAULT_LIMIT);
            }
            String suppls = message.getHeader("suppliers", String.class);
            LOG.info(String.format("hops=&s hac=%s rac=%s", hops, hac, rac));

            List<String> suppliers = parseSuppliers(suppls);

            String molecule;
            if (Constants.MIME_TYPE_SMILES.equals(mimeType)) {
                molecule = message.getHeader("smiles", String.class);
            } else {
                molecule = message.getBody(String.class);
            }
            if (molecule == null || molecule.isEmpty()) {
                throw new IllegalArgumentException("Query molecule must be specified");
            }

            ExpansionResults result;
            try (Session session = graphdb.getSession()) {
                // execute the query
                ExpansionQuery query = new ExpansionQuery(session, getSupplierMappings());
                if (pathLimit != null) { // default limit is AbstractQuery.DEFAULT_LIMIT
                    query.setLimit(pathLimit);
                }
                long n0 = System.nanoTime();
                result = query.executeQuery(molecule, mimeType, hops, hac, rac, suppliers);
                long n1 = System.nanoTime();
                expansionSearchNeo4jSearchDuration.inc((double) (n1 - n0));
                expansionSearchHitsTotal.inc((double) result.getSize());
            }

            if (result.getSize() == 0) { // no results found
                LOG.info("ExpansionQuery found no results");
                message.setBody("{\"error\": \"No Results\",\"message\": \"ExpansionQuery molecule not found in the database\"}");
                message.setHeader(Exchange.HTTP_RESPONSE_CODE, 404);

            } else {
                message.setBody(result);
                message.setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
                long t1 = System.nanoTime();
                long duration = t1 - t0; //nanos
                writeToExpansionQueryLog(username, "ExpansionQuery", duration, result.getSize(), result.getPathCount());
                expansionSearchRequestsDuration.inc((double) duration);
            }


        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "ExpansionQuery Failed", ex);
            expansionSearchErrorsTotal.inc();
            message.setBody("{\"error\": \"ExpansionQuery Failed\",\"message\",\"" + ex.getLocalizedMessage() + "\"}");
            message.setHeader(Exchange.HTTP_RESPONSE_CODE, 500);

            long t1 = System.nanoTime();
            writeErrorToQueryLog(username, "ExpansionQuery", t1 - t0, ex.getLocalizedMessage());
        }
    }


    void executeNeighbourhoodQuery(Exchange exch) {

        neighbourhoodSearchRequestsTotal.inc();

        Message message = exch.getIn();

//        message.getHeaders().forEach((k,v) -> {
//            System.out.println(k + " -> " +v);
//        });

        long t0 = System.nanoTime();
        String username = getUsername(exch);

        try {
            String smilesQuery = message.getHeader("smiles", String.class);
            if (smilesQuery == null || smilesQuery.isEmpty()) {
                throw new IllegalArgumentException("Smiles must be specified");
            }
            Integer hops = message.getHeader("hops", Integer.class);
            Integer hac = message.getHeader("hac", Integer.class);
            Integer rac = message.getHeader("rac", Integer.class);
            Integer pathLimit = message.getHeader("pathLimit", Integer.class);
            Integer groupLimit = message.getHeader("groupLimit", Integer.class);
            if (pathLimit != null && pathLimit > AbstractQuery.DEFAULT_LIMIT) {
                throw new IllegalArgumentException("Path limit cannot be greater than " + AbstractQuery.DEFAULT_LIMIT);
            }
            String suppls = message.getHeader("suppliers", String.class);
            String calcs = message.getHeader("calcs", String.class);
            LOG.info(String.format("hops=&s hac=%s rac=%s calcs=%s", hops, hac, rac, calcs));

            List<Calculator.Calculation> calculations = parseCalculations(calcs);
            List<String> suppliers = parseSuppliers(suppls);

            NeighbourhoodGraph result;
            try (Session session = graphdb.getSession()) {
                // execute the query
                NeighbourhoodQuery query = new NeighbourhoodQuery(session, getSupplierMappings());
                if (pathLimit != null) { // default limit is AbstractQuery.DEFAULT_LIMIT
                    query.setLimit(pathLimit);
                }
                long n0 = System.nanoTime();
                result = query.executeNeighbourhoodQuery(smilesQuery, hops, hac, rac, suppliers, groupLimit);
                long n1 = System.nanoTime();
                neighbourhoodSearchNeo4jSearchDuration.inc((double) (n1 - n0));
                neighbourhoodSearchHitsTotal.inc((double) result.getNodes().size());
            }

            if (result.getNodes().size() == 0) { // no results found
                LOG.info("NeighbourhoodQuery found no results");
                message.setBody("{\"error\": \"No Results\",\"message\": \"NeighbourhoodQuery molecule not found in the database\"}");
                message.setHeader(Exchange.HTTP_RESPONSE_CODE, 404);

            } else {

                // generate the group info for each group
                LOG.fine("Generating group info");
                long m0 = System.nanoTime();
                result.generateGroupInfo();
                long m1 = System.nanoTime();
                neighbourhoodSearchMCSDuration.inc((double) (m1 - m0));

                // if calculations have been specified then calculate them
                if (!calculations.isEmpty()) {
                    LOG.info("Running " + calculations.size() + " calculations");
                    long c0 = System.nanoTime();
                    result.calculate(result.getRefmol(), calculations.toArray(new Calculator.Calculation[calculations.size()]));
                    long c1 = System.nanoTime();
                    neighbourhoodSearchCalculationsDuration.inc((double) (c1 - c0));
                }

                message.setBody(result);
                message.setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
                long t1 = System.nanoTime();
                long duration = t1 - t0; //nanos
                writeToNeighbourhoodQueryLog(username, "NeighbourhoodQuery", duration, result.getNodeCount(), result.getEdgeCount(), result.getGroupCount());
                neighbourhoodSearchRequestsDuration.inc((double) duration);
            }

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "NeighbourhoodQuery Failed", ex);
            neighbourhoodSearchErrorsTotal.inc();
            message.setBody("{\"error\": \"NeighbourhoodQuery Failed\",\"message\",\"" + ex.getLocalizedMessage() + "\"}");
            message.setHeader(Exchange.HTTP_RESPONSE_CODE, 500);

            long t1 = System.nanoTime();
            writeErrorToQueryLog(username, "NeighbourhoodQuery", t1 - t0, ex.getLocalizedMessage());
        }
    }


    public void metrics(Writer responseWriter) throws IOException {
        TextFormat.write004(responseWriter, CollectorRegistry.defaultRegistry.metricFamilySamples());
        responseWriter.close();
    }

}
