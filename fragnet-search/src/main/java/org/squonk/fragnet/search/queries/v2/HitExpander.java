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
import org.squonk.fragnet.search.model.v2.ExpandMultiResult;
import org.squonk.fragnet.search.model.v2.ExpandedHit;
import org.squonk.fragnet.search.model.v2.ExpansionResults;
import org.squonk.fragnet.search.model.v2.SimpleSmilesMol;

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

    protected ExpansionResults executeQuery(String smiles, int hops, int hac, int rac, List<String> suppliers) throws IOException {
        ExpansionQuery query = new ExpansionQuery(session, null);
        ExpansionResults results = query.executeQuery(smiles, "chemical/x-daylight-smiles", hops, hac, rac, suppliers);
        return results;
    }

    public ExpandMultiResult processMolecules(Stream<SimpleSmilesMol> mols, int hops, int hac, int rac, List<String> suppliers) throws IOException {
        List<SimpleSmilesMol> list = mols.collect(Collectors.toList());
        return processMolecules(list, hops, hac, rac, suppliers);
    }

    /** Expand the specified molecules using the specified parameters.
     * Each molecule is searched using an @{ExpansionQuery} search and the results aggregated into an ExpandMultiResult
     * instance which is typically serialized to JSON.
     *
     * @param mols The molecule to process (must be standardized correctly as the search is for an exact match on the SMILES).
     * @param hops The number of fragment network edges to traverse.
     * @param hac The difference in heavy atom count that is allowed.
     * @param rac The difference in ring atom count that is allowed.
     * @param suppliers Suppliers to restrict the results to. Can be null for all suppliers.
     * @return ExpandMultiResult instance containing the aggregated results and information about the query.
     * @throws IOException
     */
    public ExpandMultiResult processMolecules(List<SimpleSmilesMol> mols, int hops, int hac, int rac, List<String> suppliers) throws IOException {

        Map<String,ExpandedHit> queryResults = new LinkedHashMap<>();
        Map<String,Object> params = new LinkedHashMap<>();
        params.put("hops", hops);
        params.put("hac", hac);
        params.put("rac", rac);
        if (suppliers != null && !suppliers.isEmpty()) {
            params.put("suppliers", suppliers);
        }
        ExpandMultiResult json = new ExpandMultiResult(mols, params);

        int count = 0;
        long t0 = System.currentTimeMillis();
        LOG.info(String.format("Processing %s queries", mols.size()));
        for (SimpleSmilesMol mol : mols) {
            String query = mol.getSmiles();
            LOG.fine("Processing " + query);
            ExpansionResults result = executeQuery(query, hops, hac, rac, suppliers);
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
