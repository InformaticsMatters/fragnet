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

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestParamType;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.squonk.fragnet.Utils;
import org.squonk.fragnet.chem.Calculator;
import org.squonk.fragnet.search.model.NeighbourhoodGraph;
import org.squonk.fragnet.search.queries.SimpleNeighbourhoodQuery;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Principal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FragnetRouteBuilder extends RouteBuilder implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(FragnetRouteBuilder.class.getName());

    static {
        // this is necessary to load the RDKit libraries.
        // Needs to be done once somewhere before any RDKit code is called.
        System.loadLibrary("GraphMolWrap");
    }

    private static final String NEO4J_SERVER = Utils.getConfiguration("NEO4J_SERVER", "neo4j");
    private static final String NEO4J_USER = Utils.getConfiguration("NEO4J_USER", "neo4j");
    private static final String NEO4J_PASSWORD = Utils.getConfiguration("NEO4J_PASSWORD", null);
    private static final String NEO4J_URL = "bolt://" + NEO4J_SERVER + ":7687";
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private final File queryLogFile;

    private Driver driver = null;

    public FragnetRouteBuilder() {
        this(true);
    }

    public FragnetRouteBuilder(boolean writeQueryLog) {
        if (writeQueryLog) {
            String home = System.getProperty("user.home");
            if (home != null) {
                File file = new File(home, "fragnet-queries.log");
                if (Utils.createFileIfNotPresent(file) == 1) {
                    try {
                        // test we can write to it
                        try (FileOutputStream fos = new FileOutputStream(file)) {
                            String msg = "Query log created on " + DATE_FORMAT.format(new Date()) + "\n";
                            fos.write(msg.getBytes());
                        }
                    } catch (Exception e) {
                        LOG.warning("Failed to create query log file for writing");
                        file = null;
                    }
                }
                queryLogFile = file;
                LOG.info("Writing query log to " + file.getPath());

            } else {
                queryLogFile = null;
                LOG.warning("No home dir found. Cannot create fragnet-queries.log");
            }
        } else {
            queryLogFile = null;
            LOG.warning("Writing to fragnet-queries.log is disabled");
        }
    }

    @Override
    public void configure() throws Exception {


        LOG.info("Connecting to Neo4j at " + NEO4J_URL + " as user " + NEO4J_USER);


        restConfiguration().component("servlet").host("0.0.0.0");
//                .apiContextPath("/api-doc")
//                .apiProperty("api.title", "Fragnet services").apiProperty("api.version", "1.0")
//                .apiProperty("cors", "true");

        //These are the REST endpoints - exposed as public web services
        //
        // test like this:
        // curl "http://localhost:8080/fragnet-search/rest/ping"
        rest("/ping").description("Simple ping service to check things are running")
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

    private String getUsername(Exchange exch) {
        HttpServletRequest request = exch.getIn().getBody(HttpServletRequest.class);
        Principal principal = request.getUserPrincipal();
        if (principal != null) {
            return principal.getName();
        } else {
            return "unauthenticated";
        }
    }

    void executeNeighbourhoodQuery(Exchange exch) {

        Message message = exch.getIn();

//        message.getHeaders().forEach((k,v) -> {
//            System.out.println(k + " -> " +v);
//        });

        // If we have no database connection, then let's try and get one...
        if (driver == null) {
            driver = GraphDatabase.
                driver(NEO4J_URL,
                       AuthTokens.basic(NEO4J_USER, NEO4J_PASSWORD));
        }

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

            List<Calculator.Calculation> calculations = new ArrayList<>();
            if (calcs != null) {
                String[] names = calcs.split(",");
                for (String name : names) {
                    name = name.trim();
                    if (!name.isEmpty()) {
                        LOG.finer("Adding calculation " + name);
                        calculations.add(Calculator.Calculation.valueOf(name));
                    }
                }
            }

            NeighbourhoodGraph result;
            try (Session session = driver.session()) {
                // execute the query
                SimpleNeighbourhoodQuery query = new SimpleNeighbourhoodQuery(session);
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
            writeToQueryLog(username, "NeighbourhoodQuery", t1 - t0, result.numNodes(), result.numEdges(), result.numGroups());

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Query Failed", ex);
            message.setBody("{\"error\": \"Query Failed\",\"message\",\"" + ex.getLocalizedMessage() + "\"}");
            message.setHeader(Exchange.HTTP_RESPONSE_CODE, 500);

            long t1 = new Date().getTime();
            writeErrorToQueryLog(username, "NeighbourhoodQuery", t1 - t0, ex.getLocalizedMessage());
        }
    }

    @Override
    public void close() throws Exception {
        driver.close();
        driver = null;
    }

    private void writeToQueryLog(String user, String searchType, long executionTime, int nodes, int edges, int groups) {
        String date = DATE_FORMAT.format(new Date());
        String txt = String.format("%s\t%s\t%s\t%s\tnodes=%s,edges=%s,groups=%s\n",
                user, date, searchType, executionTime, nodes, edges, groups);
        writeToQueryLog(txt);
    }

    private void writeErrorToQueryLog(String user, String searchType, long executionTime, String msg) {
        String date = DATE_FORMAT.format(new Date());
        String txt = String.format("%s\t%s\t%s\t%s\terror=%s\n",
                user, date, searchType, executionTime, msg);
        writeToQueryLog(txt);
    }

    private void writeToQueryLog(String txt) {
        if (queryLogFile != null) {
            try {
                Utils.appendToFile(queryLogFile, txt);
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Failed to write to query log", e);
            }
        }
    }
}
