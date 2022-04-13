/*
 * Copyright (c) 2021 Informatics Matters Ltd.
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
import org.neo4j.driver.Session;
import org.squonk.fragnet.Constants;
import org.squonk.fragnet.chem.Calculator;
import org.squonk.fragnet.chem.MolStandardize;
import org.squonk.fragnet.search.model.v2.*;
import org.squonk.fragnet.search.queries.AbstractQuery;
import org.squonk.fragnet.search.queries.v2.*;
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

    private final Counter moleculeSearchRequestsTotal = Counter.build()
            .name("requests_molecule_total")
            .help("Total number of molecule search requests")
            .register();

    private final Counter moleculeSearchNeo4jSearchDuration = Counter.build()
            .name("duration_molecule_neo4j_ns")
            .help("Total duration of molecule Neo4j cypher query")
            .register();

    private final Counter fragmentSearchRequestsTotal = Counter.build()
            .name("requests_fragment_total")
            .help("Total number of fragment search requests")
            .register();

    private final Counter synthonExpandRequestsTotal = Counter.build()
            .name("synthon_expand_total")
            .help("Total number of synthon expansion requests")
            .register();

    private final Counter synthonExpandNeo4jSearchDuration = Counter.build()
            .name("duration_synthon_expand_neo4j_ns")
            .help("Total duration of synthon expansion Neo4j cypher query")
            .register();

    private final Counter synthonExpandMoleculesTotal = Counter.build()
            .name("results_synthon_expand_molecules")
            .help("Total number of synthon expansion search fragments")
            .register();

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

    private final Counter fragmentSearchNeo4jSearchDuration = Counter.build()
            .name("duration_fragment_neo4j_ns")
            .help("Total duration of fragment Neo4j cypher query")
            .register();

    private final Counter neighbourhoodSearchNeo4jSearchDuration = Counter.build()
            .name("duration_neighbourhood_neo4j_ns")
            .help("Total duration of neighbourhood Neo4j cypher query")
            .register();

    private final Counter neighbourhoodSearchMCSDuration = Counter.build()
            .name("duration_neighbourhood_mcs_ns")
            .help("Total duration of mcs determination")
            .register();

    private final Counter moleculeSearchHitsTotal = Counter.build()
            .name("results_molecules_hits_molecules")
            .help("Total number of molecule search hits")
            .register();

    private final Counter moleculeSearchMissesTotal = Counter.build()
            .name("results_molecules_misses_molecules")
            .help("Total number of molecule search misses")
            .register();

    private final Counter fragmentSearchMissesTotal = Counter.build()
            .name("results_fragment_misses_molecules")
            .help("Total number of fragment search misses")
            .register();

    private final Counter fragmentSearchMoleculesTotal = Counter.build()
            .name("results_fragments_molecules")
            .help("Total number of fragment search fragments")
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
                .get("userinfo").description("User info")
                .produces("application/json")
                .route()
                .process((Exchange exch) -> {
                    getUserInfo(exch);
                })
                .endRest()
                // Is this molecule part of the fragment network
                // example:
                // curl "$FRAGNET_SERVER/fragnet-search/rest/v2/search/molecule/OC(Cn1ccnn1)C1CC1"
                .get("molecule/{smiles}").description("Molecule search")
                .param().name("smiles").type(RestParamType.path).description("SMILES query").endParam()
                .produces("application/json")
                .route()
                .process((Exchange exch) -> {
                    executeMoleculeQuery(exch);
                })
                .endRest()
                .post("molecule").description("Molecule search")
                .bindingMode(RestBindingMode.off)
                .param().name("molfile").type(RestParamType.body).description("Molfile query").endParam()
                .produces("application/json")
                .route()
                .process((Exchange exch) -> {
                    executeMoleculeQuery(exch);
                })
                .marshal().json(JsonLibrary.Jackson)
                .endRest()
                // Fetch the child fragments of a molecule
                // example:
                // curl "$FRAGNET_SERVER/fragnet-search/rest/v2/search/fragments/OC(Cn1ccnn1)C1CC1"
                .get("fragments/{smiles}").description("Find fragments of a molecule")
                .param().name("smiles").type(RestParamType.path).description("SMILES query").endParam()
                .produces("application/json")
                .route()
                .process((Exchange exch) -> {
                    executeFragmentQuery(exch);
                })
                .endRest()
                .post("fragments").description("Find fragments of a molecule")
                .bindingMode(RestBindingMode.off)
                .param().name("molfile").type(RestParamType.body).description("Molfile query").endParam()
                .produces("application/json")
                .route()
                .process((Exchange exch) -> {
                    executeFragmentQuery(exch);
                })
                .marshal().json(JsonLibrary.Jackson)
                .endRest()
                // Fetch the expansions of a molecule that involve a specific synthon
                // example:
                // curl "$FRAGNET_SERVER/fragnet-search/rest/v2/search/synthon-expand/OC(Cn1ccnn1)C1CC1"
                .get("synthon-expand/{smiles}").description("Find expansions of a molecule using a particular synthon")
                .param().name("smiles").type(RestParamType.path).description("SMILES query").endParam()
                .param().name("synthon").type(RestParamType.query).description("SMILES synthon").endParam()
                .param().name("hops").type(RestParamType.query).description("Number of edge traversals").endParam()
                .param().name("hacMin").type(RestParamType.query).description("Heavy atom count reduction ").endParam()
                .param().name("hacMax").type(RestParamType.query).description("Heavy atom count increase ").endParam()
                .param().name("racMin").type(RestParamType.query).description("Ring atom count reduction").endParam()
                .param().name("racMax").type(RestParamType.query).description("Ring atom count increase").endParam()
                .param().name("limit").type(RestParamType.query).description("Max number of results to be returned").endParam()
                .produces("application/json")
                .route()
                .process((Exchange exch) -> {
                    executeSynthonExpand(exch);
                })
                .endRest()
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
                .param().name("hacMin").type(RestParamType.query).description("Heavy atom count reduction ").endParam()
                .param().name("hacMax").type(RestParamType.query).description("Heavy atom count increase ").endParam()
                .param().name("racMin").type(RestParamType.query).description("Ring atom count reduction").endParam()
                .param().name("racMax").type(RestParamType.query).description("Ring atom count increase").endParam()
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
                .param().name("hacMin").type(RestParamType.query).description("Heavy atom count reduction ").endParam()
                .param().name("hacMax").type(RestParamType.query).description("Heavy atom count increase ").endParam()
                .param().name("racMin").type(RestParamType.query).description("Ring atom count reduction").endParam()
                .param().name("racMax").type(RestParamType.query).description("Ring atom count increase").endParam()
                .param().name("hops").type(RestParamType.query).description("Number of edge traversals").endParam()
                .param().name("suppliers").type(RestParamType.query).description("Suppliers to include").endParam()
                .param().name("pathLimit").type(RestParamType.query).description("Limit for the number of paths to return from the graph query").endParam()
                .produces("application/json")
                .route()
                .process((Exchange exch) -> {
                    String contentType = exch.getIn().getHeader(Exchange.CONTENT_TYPE, String.class);
                    executeExpansionQuery(exch, contentType);
                })
                .marshal().json(JsonLibrary.Jackson)
                .endRest()
                .post("expand-multi").description("Expansion search (multiple inputs)")
                .bindingMode(RestBindingMode.off)
                .param().name("smiles").type(RestParamType.body).description("SMILES queries").endParam()
                .param().name("hacMin").type(RestParamType.query).description("Heavy atom count reduction ").endParam()
                .param().name("hacMax").type(RestParamType.query).description("Heavy atom count increase ").endParam()
                .param().name("racMin").type(RestParamType.query).description("Ring atom count reduction").endParam()
                .param().name("racMax").type(RestParamType.query).description("Ring atom count increase").endParam()
                .param().name("hops").type(RestParamType.query).description("Number of edge traversals").endParam()
                .param().name("suppliers").type(RestParamType.query).description("Suppliers to include").endParam()
                .param().name("id_prop").type(RestParamType.query).description("Name of the property for the ID (use _Name for the mol name)").endParam()
                .produces("application/json")
                .route()
                .process((Exchange exch) -> {
                    executeExpansionMultiQuery(exch);
                })
                .marshal().json(JsonLibrary.Jackson)
                .endRest()
        ;
    }

    void getUserInfo(Exchange exch) {

        Message message = exch.getIn();
        String username = getUsername(exch);

        Map<String, Object> map = new LinkedHashMap<>();
        // TODO - implement the different tiers once these are defined in Keycloak
        map.put("username", username);
        map.put("tier", "evaluation");
        map.put("num_queries_executed", accountData.getQueryCount(username));
        map.put("num_queries_remaining", 50 - accountData.getQueryCount(username));
        map.put("period_days", accountData.getDaysCuttoff());

        message.setBody(map);
        message.setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
    }

    void findCalculations(Exchange exch) {

        Message message = exch.getIn();

        List<Map<String, String>> list = new ArrayList<>();
        for (Calculator.Calculation calc : Calculator.Calculation.values()) {
            // don't add the morgan fingerprints as they cause a strange RDKit crash in the servlet
            if (!calc.name().startsWith("SIM_MORGAN")) {
                Map<String, String> map = new LinkedHashMap<>();
                map.put("id", calc.toString());
                map.put("name", calc.propname);
                map.put("description", calc.description);
                map.put("type", calc.type);
                list.add(map);
            }
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
                message.setBody("{\"error\": \"AvailabilityQuery Failed\",\"message\": \"SMILES not found\"}");
                message.setHeader(Exchange.HTTP_RESPONSE_CODE, 404);
            } else {
                message.setBody(availability);
                message.setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "AvailabilityQuery Failed", ex);
            message.setBody("{\"error\": \"AvailabilityQuery Failed\",\"message\": \"" + ex.getLocalizedMessage() + "\"}");
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
            writeErrorResponse(message, 500, "{\"error\": \"NeighbourhoodQuery Failed\",\"message\",\"" + ex.getLocalizedMessage() + "\"}");
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

    void executeExpansionQuery(Exchange exch, String conentType) {

        expansionSearchRequestsTotal.inc();

        Message message = exch.getIn();

//        message.getHeaders().forEach((k,v) -> {
//            System.out.println(k + " -> " +v);
//        });

        long t0 = System.nanoTime();
        String username = getUsername(exch);

        try {
            Integer hops = message.getHeader("hops", Integer.class);
            Integer hacMin = message.getHeader("hacMin", Integer.class);
            Integer hacMax = message.getHeader("hacMax", Integer.class);
            Integer racMin = message.getHeader("racMin", Integer.class);
            Integer racMax = message.getHeader("racMax", Integer.class);
            Integer pathLimit = message.getHeader("pathLimit", Integer.class);
            if (pathLimit != null && pathLimit > AbstractQuery.DEFAULT_LIMIT) {
                throw new IllegalArgumentException("Path limit cannot be greater than " + AbstractQuery.DEFAULT_LIMIT);
            }
            String suppls = message.getHeader("suppliers", String.class);
            LOG.info(String.format("hops=%s hacMin=%s hacMax=%s racMin=%s racMax=%s", hops, hacMin, hacMax, racMin, racMax));

            List<String> suppliers = parseSuppliers(suppls);

            String molecule;
            if (Constants.MIME_TYPE_SMILES.equals(conentType)) {
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
                result = query.executeQuery(molecule, conentType, hops, hacMin, hacMax, racMin, racMax, suppliers);
                long n1 = System.nanoTime();
                expansionSearchNeo4jSearchDuration.inc((double) (n1 - n0));
                expansionSearchHitsTotal.inc((double) result.getSize());
            }

            if (result.getSize() == 0) { // no results found
                LOG.info("ExpansionQuery found no results");
                writeErrorResponse(message, 404,
                        "{\"error\": \"No Results\",\"message\": \"ExpansionQuery molecule not found in the database or could not be expanded\"}");
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
            writeErrorResponse(message, 500, "{\"error\": \"ExpansionQuery Failed\",\"message\",\"" + ex.getLocalizedMessage() + "\"}");

            long t1 = System.nanoTime();
            writeErrorToQueryLog(username, "ExpansionQuery", t1 - t0, ex.getLocalizedMessage());
        }
    }


    void executeExpansionMultiQuery(Exchange exch) {

        Message message = exch.getIn();

//        message.getHeaders().forEach((k,v) -> {
//            System.out.println(k + " -> " +v);
//        });

        long t0 = System.nanoTime();
        String username = getUsername(exch);

        try {
            Integer hops = message.getHeader("hops", Integer.class);
            Integer hacMin = message.getHeader("hacMin", Integer.class);
            Integer hacMax = message.getHeader("hacMax", Integer.class);
            Integer racMin = message.getHeader("racMin", Integer.class);
            Integer racMax = message.getHeader("racMax", Integer.class);
            Integer pathLimit = message.getHeader("pathLimit", Integer.class);
            if (pathLimit != null && pathLimit > AbstractQuery.DEFAULT_LIMIT) {
                throw new IllegalArgumentException("Path limit cannot be greater than " + AbstractQuery.DEFAULT_LIMIT);
            }
            String suppls = message.getHeader("suppliers", String.class);
            LOG.info(String.format("hops=%s hacMin=%s hacMax=%s racMin=%s racMax=%s", hops, hacMin, hacMax, racMin, racMax));

            List<String> suppliers = parseSuppliers(suppls);

            String body = message.getBody(String.class);
            if (body == null | body.isEmpty()) {
                LOG.info("NeighbourhoodQuery found no results");
                message.setBody("{\"error\": \"No Input\",\"message\": \"No molecules POSTed.\"}");
                message.setHeader(Exchange.HTTP_RESPONSE_CODE, 500);
            } else {
                String contentType = message.getHeader(Exchange.CONTENT_TYPE, String.class);
                String idProp = message.getHeader("id_prop", String.class);
                ConvertedSmilesMols queries;
                if (contentType == null || contentType.isEmpty()) {
                    throw new IllegalStateException("ContentType must be specified");
                } else if (Constants.MIME_TYPE_SMILES.equals(contentType)) {
                    queries = MolStandardize.readStdNonisoSmilesFromSmilesData(body);
                } else if (Constants.MIME_TYPE_SDFILE.equals(contentType)) {
                    queries = MolStandardize.readStdNonisoSmilesFromSDFData(body, idProp);
                } else {
                    throw new IllegalArgumentException("Unexpected content type: " + contentType);
                }
                LOG.info("Read " + queries.getMolecules().size() + " queries");


                expansionSearchRequestsTotal.inc(queries.getMolecules().size());

                // run the searches
                ExpandMultiResult result;
                try (Session session = graphdb.getSession()) {
                    // execute the query
                    HitExpander expander = new HitExpander(session);

                    long n0 = System.nanoTime();
                    result = expander.processMolecules(queries, hops, hacMin, hacMax, racMin, racMax, suppliers);
                    long n1 = System.nanoTime();
                    expansionSearchNeo4jSearchDuration.inc((double) (n1 - n0));
                    expansionSearchHitsTotal.inc((double) result.getResults().size());
                }

                if (result.getResults().size() == 0) { // no results found
                    LOG.info("ExpansionMultiQuery found no results");
                    message.setBody("{\"error\": \"No Results\",\"message\": \"ExpansionMultiQuery molecules not found in the database\"}");
                    message.setHeader(Exchange.HTTP_RESPONSE_CODE, 404);

                } else {
                    message.setBody(result);
                    message.setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
                    long t1 = System.nanoTime();
                    long duration = t1 - t0; //nanos
                    writeToExpansionQueryLog(username, "ExpansionMulti", duration, result.getResults().size(), queries.getMolecules().size());
                    expansionSearchRequestsDuration.inc((double) duration);
                }
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

    private String[] fetchSmilesOrMolfile(Message message) {
        String queryMol = null;
        String mimeType = message.getHeader(Exchange.CONTENT_TYPE, String.class);
        LOG.info("mime-type is " + mimeType);
        if (mimeType == null) {
            mimeType = Constants.MIME_TYPE_SMILES;
        }
        if (Constants.MIME_TYPE_SMILES.equals(mimeType)) {
            queryMol = message.getHeader("smiles", String.class);
        } else if (Constants.MIME_TYPE_MOLFILE.equals(mimeType)) {
            queryMol = message.getBody(String.class);
        } else {
            throw new IllegalStateException("Only support SMILES using GET or molfile using POST");
        }
        return new String[]{queryMol, mimeType};
    }

    void executeMoleculeQuery(Exchange exch) {

        LOG.info("Executing executeMoleculeQuery");

        moleculeSearchRequestsTotal.inc();

        Message message = exch.getIn();

        long t0 = System.nanoTime();
        String username = getUsername(exch);

        try {
            String[] data = fetchSmilesOrMolfile(message);
            String queryMol = data[0];
            String mimeType = data[1];

            if (queryMol == null || queryMol.isEmpty()) {
                throw new IllegalArgumentException("Query molecule must be specified");
            }

            MoleculeNode molNode;
            try (Session session = graphdb.getSession()) {
                // execute the query
                MoleculeQuery query = new MoleculeQuery(session);

                long n0 = System.nanoTime();
                molNode = query.execute(queryMol, mimeType);
                long n1 = System.nanoTime();
                moleculeSearchNeo4jSearchDuration.inc((double) (n1 - n0));
                if (molNode == null) {
                    moleculeSearchMissesTotal.inc(1.0d);
                    // throw 404
                    message.setBody("{\"error\": \"MoleculeQuery Failed\",\"message\": \"Molecule not found\"}");
                    message.setHeader(Exchange.HTTP_RESPONSE_CODE, 404);
                } else {
                    moleculeSearchHitsTotal.inc(1.0d);
                    message.setBody(molNode);
                    message.setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
                }
            }

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "MoleculeQuery Failed", ex);
            neighbourhoodSearchErrorsTotal.inc();
            message.setBody("{\"error\": \"MoleculeQuery Failed\",\"message\":\"" + ex.getLocalizedMessage() + "\"}");
            message.setHeader(Exchange.HTTP_RESPONSE_CODE, 500);

            long t1 = System.nanoTime();
            writeErrorToQueryLog(username, "MoleculeQuery", t1 - t0, ex.getLocalizedMessage());
        }
    }

    void executeFragmentQuery(Exchange exch) {
        LOG.info("Executing executeMoleculeQuery");

        fragmentSearchRequestsTotal.inc();

        Message message = exch.getIn();

        long t0 = System.nanoTime();
        String username = getUsername(exch);

        try {
            String[] data = fetchSmilesOrMolfile(message);
            String queryMol = data[0];
            String mimeType = data[1];

            if (queryMol == null || queryMol.isEmpty()) {
                throw new IllegalArgumentException("Query molecule must be specified");
            }

            List<String> smiles;
            try (Session session = graphdb.getSession()) {
                // execute the query
                FragmentQuery query = new FragmentQuery(session);

                long n0 = System.nanoTime();
                smiles = query.execute(queryMol, mimeType);
                long n1 = System.nanoTime();
                fragmentSearchNeo4jSearchDuration.inc((double) (n1 - n0));
                if (smiles == null || smiles.isEmpty()) {
                    fragmentSearchMissesTotal.inc(1.0d);
                    // throw 404
                    message.setBody("{\"error\": \"MoleculeQuery Failed\",\"message\": \"Molecule not found\"}");
                    message.setHeader(Exchange.HTTP_RESPONSE_CODE, 404);
                } else {
                    int size = smiles.size();
                    fragmentSearchMoleculesTotal.inc((double) size);
                    LOG.info(size + " fragments found");
                    message.setBody(smiles);
                    message.setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
                }
            }

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "MoleculeQuery Failed", ex);
            neighbourhoodSearchErrorsTotal.inc();
            message.setBody("{\"error\": \"MoleculeQuery Failed\",\"message\":\"" + ex.getLocalizedMessage() + "\"}");
            message.setHeader(Exchange.HTTP_RESPONSE_CODE, 500);

            long t1 = System.nanoTime();
            writeErrorToQueryLog(username, "MoleculeQuery", t1 - t0, ex.getLocalizedMessage());
        }
    }


    void executeSynthonExpand(Exchange exch) {
        LOG.info("Executing executeMoleculeQuery");

        synthonExpandRequestsTotal.inc();

        Message message = exch.getIn();

        long t0 = System.nanoTime();
        String username = getUsername(exch);

        String queryMol = message.getHeader("smiles", String.class);
        String synthon = message.getHeader("synthon", String.class);
        Integer hops = message.getHeader("hops", Integer.class);
        Integer hacMin = message.getHeader("hacMin", Integer.class);
        Integer hacMax = message.getHeader("hacMax", Integer.class);
        Integer racMin = message.getHeader("racMin", Integer.class);
        Integer racMax = message.getHeader("racMax", Integer.class);
        Integer limit = message.getHeader("limit", Integer.class);

        List<String> smiles;
        try (Session session = graphdb.getSession()) {
            // execute the query
            SynthonExpandQuery query = new SynthonExpandQuery(session);

            long n0 = System.nanoTime();
            smiles = query.execute(queryMol, synthon, hops, hacMin, hacMax, racMin, racMax, limit);
            long n1 = System.nanoTime();
            synthonExpandNeo4jSearchDuration.inc((double) (n1 - n0));
            if (smiles == null || smiles.isEmpty()) {
                fragmentSearchMissesTotal.inc(1.0d);
                // throw 404
                message.setBody("{\"error\": \"MoleculeQuery Failed\",\"message\": \"Molecule not found\"}");
                message.setHeader(Exchange.HTTP_RESPONSE_CODE, 404);
            } else {
                int size = smiles.size();
                synthonExpandMoleculesTotal.inc((double) size);
                LOG.info(size + " expansions found");
                message.setBody(smiles);
                message.setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
            }

        } catch (
                Exception ex) {
            LOG.log(Level.SEVERE, "MoleculeQuery Failed", ex);
            neighbourhoodSearchErrorsTotal.inc();
            message.setBody("{\"error\": \"MoleculeQuery Failed\",\"message\":\"" + ex.getLocalizedMessage() + "\"}");
            message.setHeader(Exchange.HTTP_RESPONSE_CODE, 500);

            long t1 = System.nanoTime();
            writeErrorToQueryLog(username, "MoleculeQuery", t1 - t0, ex.getLocalizedMessage());
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
