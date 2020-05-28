/*
 * Copyright (c) 2020 Informatics Matters Ltd.
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
import org.squonk.fragnet.search.model.v2.*;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Handles expanding a set of hits using the fragment network
 *
 */
public class HitExpander {

    private static final Logger LOG = Logger.getLogger(HitExpander.class.getName());

    private final Session session;

    public HitExpander(Session session) {
        this.session = session;
    }

    protected ExpansionResults executeQuery(String smiles, Integer hops, Integer hacMin, Integer hacMax, Integer racMin, Integer racMax, List<String> suppliers) throws IOException {
        ExpansionQuery query = new ExpansionQuery(session, null);
        ExpansionResults results = query.executeQuery(smiles, "chemical/x-daylight-smiles", hops, hacMin, hacMax, racMin, racMax, suppliers);
        return results;
    }


    /** Expand the specified molecules using the specified parameters.
     * Each molecule is searched using an @{ExpansionQuery} search and the results aggregated into an ExpandMultiResult
     * instance which is typically serialized to JSON.
     *
     * @param queries The molecules to process (must be standardized correctly as the search is for an exact match on the SMILES).
     * @param hops The number of fragment network edges to traverse.
     * @param hacMin Lower limit for the change in the heavy atom counts. If null then no limit.
     * @param hacMax Upper limit for the change in the heavy atom counts. If null then no limit.
     * @param racMin Lower limit for the change in the ring atom counts. If null then no limit.
     * @param racMax Upper limit for the change in the ring atom counts. If null then no limit.
     * @param suppliers Suppliers to restrict the results to. Can be null for all suppliers.
     * @return ExpandMultiResult instance containing the aggregated results and information about the query.
     * @throws IOException
     */
    public ExpandMultiResult processMolecules(
            ConvertedSmilesMols queries, Integer hops,
            Integer hacMin, Integer hacMax, Integer racMin, Integer racMax,
            List<String> suppliers) throws IOException {

        // standardize molecules

        Map<String,ExpandedHit> queryResults = new LinkedHashMap<>();
        Map<String,Object> params = new LinkedHashMap<>();
        params.put("hops", hops);
        if (hacMin != null) {
            params.put("hacMin", hacMin);
        }
        if (hacMax != null) {
            params.put("hacMax", hacMax);
        }
        if (racMin != null) {
            params.put("racMin", racMin);
        }
        if (racMax != null) {
            params.put("racMax", racMax);
        }
        if (suppliers != null && !suppliers.isEmpty()) {
            params.put("suppliers", suppliers);
        }
        ExpandMultiResult json = new ExpandMultiResult(queries, params);

        int count = 0;
        long t0 = System.currentTimeMillis();
        LOG.info(String.format("Processing %s queries", queries.getMolecules().size()));
        for (ConvertedSmilesMols.Mol mol : queries.getMolecules()) {
            String query = mol.getSmiles();
            LOG.fine("Processing " + query);
            ExpansionResults result = executeQuery(query, hops, hacMin, hacMax, racMin, racMax, suppliers);
            LOG.info(String.format("Found %s hits for query %s", result.getMembers().size(), count ));
            for (ExpansionResults.Member m : result.getMembers()) {
                String smiles = m.getSmiles();
                ExpandedHit expandedHit;
                if (queryResults.containsKey(smiles)) {
                    expandedHit = queryResults.get(smiles);
                } else {
                    Set<String> ids = m.getCompoundIds();
                    expandedHit = new ExpandedHit(smiles, ids);
                    queryResults.put(smiles, expandedHit);
                }
                expandedHit.addSourceMol(mol.getId());
                json.addHitCount(mol.getId(), result.getMembers().size());
            }
            count++;
        }
        long t1 = System.currentTimeMillis();

        List<ExpandedHit> hits = new ArrayList(queryResults.values());
        json.setExecutionTimeMillis(t1 - t0);
        json.setResults(hits);
        return json;
    }
}
