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
package org.squonk.fragnet.search.queries.v1;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.neo4j.driver.v1.*;
import org.neo4j.driver.v1.types.Path;
import org.squonk.fragnet.Constants;
import org.squonk.fragnet.chem.Calculator;
import org.squonk.fragnet.chem.MolStandardize;
import org.squonk.fragnet.search.model.v1.NeighbourhoodGraph;
import org.squonk.fragnet.search.queries.AbstractQuery;
import org.squonk.fragnet.search.queries.QueryAndParams;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.neo4j.driver.v1.Values.parameters;

/**
 * v1 API NeighbourhoodQuery for fragment network neighbours
 *
 */
public class Query extends AbstractQuery {

    private static final Logger LOG = Logger.getLogger(Query.class.getName());

    public Query(Session session) {
        super(session);
    }

    // example smiles c1ccc(Nc2nc3ccccc3o2)cc1
    private final String NEIGHBOURHOOD_QUERY = "MATCH p=(m:F2)-[:F2EDGE%s]-(e:MOL)\n" +
            "WHERE m.smiles=$smiles %s\nRETURN p LIMIT $limit";

    protected String getQueryTemplate() {
        return NEIGHBOURHOOD_QUERY;
    }

    /** Execute a query for the neighbourhood around a particular molecule in the fragment network.
     *
     * @param smiles The query structure. Will be canonicalised.
     * @param hops The number of edges to traverse. Defaults to 1 if not specified.
     * @param hac A limit for the change in the heavy atom counts. If null then no limit.
     * @param rac A limit for the change in the ring atom counts. If null then no limit.
     * @return
     */
    public NeighbourhoodGraph executeNeighbourhoodQuery(@NotNull String smiles, Integer hops, Integer hac, Integer rac) {

        String stdSmiles = MolStandardize.prepareMol(smiles, Constants.MIME_TYPE_SMILES, false, false);

        QueryAndParams qandp = generateCypherQuery(stdSmiles, hops, hac, rac);

        NeighbourhoodGraph graph = getSession().writeTransaction((tx) -> {
            LOG.info("Executing NeighbourhoodQuery: " + qandp.getQuery());
            StatementResult result = tx.run(qandp.getQuery(), parameters(qandp.getParams().toArray()));
            return handleResult(result, stdSmiles);
        });
        return graph;
    }

    private QueryAndParams generateCypherQuery(String stdSmiles, Integer hops, Integer hac, Integer rac) {

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

        String q;
        String queryTemplate = getQueryTemplate();
        if (hops == 1) {
            q = String.format(queryTemplate, "", filter);
        } else if (hops == 2) {
            q = String.format(queryTemplate, "*1..2", filter);
        } else {
            throw new IllegalArgumentException("Hops must be 1 or 2");
        }

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

        LOG.info(String.format("Results built. %s nodes, %s edges", graph.numNodes(), graph.numEdges()));

        return graph;
    }


    public static void main(String... args) throws Exception {

        System.loadLibrary("GraphMolWrap");


//        Driver driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "hts"));

        String pw = System.getenv("FRAGNET_PASSWORD");
        Driver driver = GraphDatabase.driver("bolt://100.25.105.8:7687", AuthTokens.basic("neo4j", pw));

        try (Session session = driver.session()) {

            Query query = new Query(session);
            //NeighbourhoodGraph result = query.executeNeighbourhoodQuery("c1ccc(Nc2nc3ccccc3o2)cc1", 2, 3, 1);
            //NeighbourhoodGraph result = query.executeNeighbourhoodQuery("N(C1=NC2=CC=CC=C2O1)C1=CC=CC=C1", 2, 3, 1);
            //NeighbourhoodGraph result = query.executeNeighbourhoodQuery("Oc1ccc(-c2ccccc2)cc1", 2, 3, 1); // 4-OH biphenyl
            NeighbourhoodGraph result = query.executeNeighbourhoodQuery("CN1C=NC2=C1C(=O)N(C)C(=O)N2C", 2, 3, 1); // caffeine

            result.calculate(result.getRefmol(), Calculator.Calculation.values());

//            Collection<NeighbourhoodGraph.Group> collected = result.getGroups();
//            System.out.println("Num groupings: " + collected.size());
//            collected.forEach((k,v) -> {
//                System.out.println(k +  " -> " + v.stream().map((m) -> m.getSmiles()).collect(Collectors.joining(".")));
//            });
            System.out.println("\n\n");
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(result);
            System.out.println(json);
        } finally {
            driver.close();
        }
    }
}
