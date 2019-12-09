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
import org.squonk.fragnet.search.model.v2.ExpansionResults;
import org.squonk.fragnet.search.queries.AbstractQuery;
import org.squonk.fragnet.search.queries.QueryAndParams;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.neo4j.driver.v1.Values.parameters;

/**
 * v2 API ExpansionQuery for fragment network neighbours.
 * Given a molecule retunr all related actual molecules from suppliers.
 * Unlike the Neighbourhood search this API returns chiral molecules, but does not group by transform type.
 *
 */
public class ExpansionQuery extends AbstractQuery {

    private static final Logger LOG = Logger.getLogger(ExpansionQuery.class.getName());

    private final Map<String,String> supplierMappings;

    public ExpansionQuery(Session session, Map<String,String> supplierMappings) {
        super(session);
        this.supplierMappings = supplierMappings;
    }

    /** ExpansionQuery has 3 values that are substituted
     * 1. a value for the number of edges to traverse
     * 2. a value for the vendor labels e.g. V_MP for MolPort
     * 3. a value for the additional query terms such as heavy atom count change.
     * When coding these substitutions make sure that problems with "SQL injection" are avoided e.g. validate the input.
     * The query smiles and the limit are handled as query parameters.
     *
     * Example query smiles: c1ccc(Nc2nc3ccccc3o2)cc1
     *
     */
    private final String EXPANSION_QUERY = "MATCH p=(m:F2)-[:FRAG%s]-(e:Mol)<-[:NonIso*0..1]-(c:Mol)\n" +
            "WHERE %sm.smiles=$smiles AND e.smiles <> $smiles%s\nRETURN p LIMIT $limit";

    protected String getQueryTemplate() {
        return EXPANSION_QUERY;
    }

    /** Execute a query for related molecules around a particular molecule.
     *
     * @param mol The query structure. Will be canonicalised and made achiral before being queried.
     * @param mimeType The format of the molecule. Currently must be one of chemical/x-daylight-smiles or chemical/x-mdl-molfile
     * @param hops The number of edges to traverse. Defaults to 1 if not specified.
     * @param hac A limit for the change in the heavy atom counts. If null then no limit.
     * @param rac A limit for the change in the ring atom counts. If null then no limit.
     * @param suppliers Comma separated list of suppliers to include. If null or empty then all suppliers are returned.
     * @return
     */
    public ExpansionResults executeQuery(
            @NotNull String mol,
            @NotNull String mimeType,
            Integer hops,
            Integer hac,
            Integer rac,
            List<String> suppliers) {

        String stdSmiles = MolStandardize.prepareMol(mol, mimeType, false, false);
//        LOG.info("Supplied SMILES: " + smiles);
//        LOG.info("Using SMILES: " + stdSmiles);

        QueryAndParams qandp = generateCypherQuery(stdSmiles, hops, hac, rac, suppliers);

        ExpansionResults results = getSession().writeTransaction((tx) -> {
            LOG.info("Executing ExpansionQuery: " + qandp.getQuery());
            StatementResult result = tx.run(qandp.getQuery(), parameters(qandp.getParams().toArray()));
            return handleResult(result, stdSmiles);
        });
        return results;
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
        } else if (hops == 3) {
            hopsQuery = "*1..3";
        } else {
            throw new IllegalArgumentException("Hops must be 1, 2 or 3");
        }

        String q = String.format(queryTemplate, hopsQuery, vendorLabels, filter);

        return new QueryAndParams(q, params);
    }

    protected ExpansionResults handleResult(@NotNull StatementResult result, @NotNull String querySmiles) {

        ExpansionResults expansion = new ExpansionResults(querySmiles);
        long t0 = new Date().getTime();
        AtomicInteger pathCount = new AtomicInteger(0);
        result.stream().forEachOrdered((r) -> {
            LOG.finer("Handling record " + r);
            Map<String, Object> m = r.asMap();
            m.forEach((k, v) -> {
                LOG.finer("Handling key " + k);
                Path path = (Path) v;
                expansion.add(path);
                pathCount.incrementAndGet();
            });
        });
        expansion.setPathCount(pathCount.get());
        long t1 = new Date().getTime();
        expansion.setQuery(result.summary().statement().text());
        expansion.setParameters(result.summary().statement().parameters().asMap());
        expansion.setResultAvailableAfter(result.summary().resultAvailableAfter(TimeUnit.MILLISECONDS));
        expansion.setProcessingTime(t1 - t0);

        if (getLimit() <= expansion.getPathCount()) {
            expansion.setShortMessage("Incomplete results");
            expansion.setLongMessage("Results are incomplete as the max path count of " + getLimit() + " was reached");
        }

        LOG.info(String.format("Results built. %s molecules found", expansion.getSize()));

        return expansion;
    }

}
