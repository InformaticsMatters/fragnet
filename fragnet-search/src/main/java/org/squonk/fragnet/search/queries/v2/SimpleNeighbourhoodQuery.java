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
package org.squonk.fragnet.search.queries.v2;


import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.types.Path;
import org.squonk.fragnet.chem.MolStandardize;
import org.squonk.fragnet.search.model.v2.NeighbourhoodGraph;
import org.squonk.fragnet.search.queries.AbstractSimpleNeighbourhoodQuery;
import org.squonk.fragnet.search.queries.QueryAndParams;
import org.squonk.fragnet.service.GraphDB;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.neo4j.driver.v1.Values.parameters;

/**
 * v2 API Query for fragment network neighbours
 *
 */
public class SimpleNeighbourhoodQuery extends AbstractSimpleNeighbourhoodQuery {

    private static final Logger LOG = Logger.getLogger(SimpleNeighbourhoodQuery.class.getName());

    private final Map<String,String> supplierMappings;

    public SimpleNeighbourhoodQuery(Session session, Map<String,String> supplierMappings) {
        super(session);
        this.supplierMappings = supplierMappings;
    }

    /** Query has 3 values that are substituted
     * 1. a value for the number of edges to traverse
     * 2. a value for the vendor labels e.g. V_MP for MolPort
     * 3. a value for the additional query terms such as heavy atom count change.
     * When coding these substitutions make sure that problems with "SQL injection" are avoided e.g. validate the input.
     * The query smiles and the limit are handled ass query parameters.
     *
     * Example query smiles: c1ccc(Nc2nc3ccccc3o2)cc1
     *
     */
    private final String NEIGHBOURHOOD_QUERY = "MATCH p=(m:F2)-[:FRAG%s]-(e:Mol)\n" +
            "WHERE %sm.smiles=$smiles AND e.smiles <> $smiles%s\nRETURN p LIMIT $limit";

    protected String getQueryTemplate() {
        return NEIGHBOURHOOD_QUERY;
    }

    /** Execute a query for the neighbourhood around a particular molecule in the fragment network.
     *
     * @param smiles The query structure. Will be canonicalised.
     * @param hops The number of edges to traverse. Defaults to 1 if not specified.
     * @param hac A limit for the change in the heavy atom counts. If null then no limit.
     * @param rac A limit for the change in the ring atom counts. If null then no limit.
     * @param suppliers Comma separated list of suppliers to include. If null or empty then all suppliers are returned.
     * @return
     */
    public NeighbourhoodGraph executeNeighbourhoodQuery(@NotNull String smiles, Integer hops, Integer hac, Integer rac, List<String> suppliers) {

        String stdSmiles = MolStandardize.prepareMol(smiles);

        QueryAndParams qandp = generateCypherQuery(stdSmiles, hops, hac, rac, suppliers);

        NeighbourhoodGraph graph = getSession().writeTransaction((tx) -> {
            LOG.info("Executing Query: " + qandp.getQuery());
            StatementResult result = tx.run(qandp.getQuery(), parameters(qandp.getParams().toArray()));
            return handleResult(result, stdSmiles);
        });
        return graph;
    }

    private QueryAndParams generateCypherQuery(String stdSmiles, Integer hops, Integer hac, Integer rac, List<String> suppliers) {

        if (hops == null) {
            hops = 1;
        }

        List<Object> params = new ArrayList<>();
        params.add("smiles");
        params.add(stdSmiles);
        params.add("limit");
        params.add(getLimit());

        String filter = "";

        if (hac != null) {
            filter += " AND abs(m.hac - e.hac) <= $hac";
            params.add("hac");
            params.add(hac);
        }

        if (rac != null) {
            filter += " AND abs(m.chac - e.chac) <= $rac";
            params.add("rac");
            params.add(rac);
        }

        String queryTemplate = getQueryTemplate();
        String vendorLabels = null;
        if (suppliers == null || suppliers.isEmpty()) {
            vendorLabels = "";
        } else {
            StringBuilder builder = new StringBuilder("(e");
            for (String supplier: suppliers) {
                String label = supplierMappings.get(supplier);
                if (label == null) {
                    // only accept valid suppliers or we risk injection attacks
                    throw new IllegalArgumentException("Invalid supplier: " + supplier);
                }
                builder.append(":").append(label);
            }
            vendorLabels = builder.append(") AND ").toString();
        }
        String hopsQuery = null;
        if (hops == 1) {
            hopsQuery = "";
        } else if (hops == 2) {
            hopsQuery = "*1..2";
        } else {
            throw new IllegalArgumentException("Hops must be 1 or 2");
        }

        String q = String.format(queryTemplate, hopsQuery, vendorLabels, filter);

        return new QueryAndParams(q, params);
    }

    protected NeighbourhoodGraph handleResult(@NotNull StatementResult result, @NotNull String querySmiles) {

        NeighbourhoodGraph graph = new NeighbourhoodGraph(querySmiles);
        long t0 = new Date().getTime();
        result.stream().forEachOrdered((r) -> {
            LOG.finer("Handling record " + r);
            Map<String, Object> m = r.asMap();
            m.forEach((k, v) -> {
                LOG.finer("Handling value " + k);
                Path path = (Path) v;
                graph.add(path);
            });
        });
        long t1 = new Date().getTime();
        graph.setQuery(result.summary().statement().text());
        graph.setParameters(result.summary().statement().parameters().asMap());
        graph.setResultAvailableAfter(result.summary().resultAvailableAfter(TimeUnit.MILLISECONDS));
        graph.setProcessingTime(t1 - t0);

        LOG.info(String.format("Results built. %s nodes, %s edges", graph.getNodeCount(), graph.getEdgeCount()));

        return graph;
    }

    public static final void main(String[] args) {
        GraphDB db = new GraphDB();
        Session session = db.getSession();
        SimpleNeighbourhoodQuery query = new SimpleNeighbourhoodQuery(session, Collections.emptyMap());
        NeighbourhoodGraph result = query.executeNeighbourhoodQuery("c1ccccc1", 2, 10, 3, null);
        LOG.info("Found " + result.getNodes().size() + " nodes");
    }

}
