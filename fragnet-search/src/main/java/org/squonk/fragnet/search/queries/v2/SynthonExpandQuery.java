/*
 * Copyright (c) 2023 Informatics Matters Ltd.
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

import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Result;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.squonk.fragnet.Constants;
import org.squonk.fragnet.chem.MolStandardize;
import org.squonk.fragnet.search.model.v2.FragmentGraph;
import org.squonk.fragnet.search.model.v2.MoleculeNode;
import org.squonk.fragnet.search.queries.AbstractQuery;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import static org.neo4j.driver.Values.parameters;

public class SynthonExpandQuery extends AbstractQuery {

    private static final Logger LOG = Logger.getLogger(SynthonExpandQuery.class.getName());

    public SynthonExpandQuery(Session session) {
        super(session);
    }

    private final String SYNTHON_QUERY = "MATCH (fa:F2 {smiles: $smiles})" +
            "-[:FRAG*0..%s]-(:F2)" +
            "<-[e:FRAG]-(c:Mol) WHERE %s" +
            " (split(e.label, '|')[1] = $synthon OR split(e.label, '|')[4] = $synthon)" +
            " RETURN DISTINCT c LIMIT $limit";

    @Override
    protected String getQueryTemplate() {
        return SYNTHON_QUERY;
    }

    private String expandTemplate(@NotNull Integer hops, Integer hacMin, Integer hacMax, Integer racMin, Integer racMax) {
        // all params are integers so no risk of cypher injection
        String queryTemplate = getQueryTemplate();
        List<String> filters = new ArrayList<>();
        if (hacMin != null) {
            filters.add("c.hac >= " + hacMin.toString());
        }
        if (hacMax != null) {
            filters.add("c.hac <= " + hacMax.toString());
        }
        if (racMin != null) {
            filters.add("c.chac >= " + racMin.toString());
        }
        if (racMax != null) {
            filters.add("c.chac <= " + racMax.toString());
        }

        String filter = "";
        if (filters.size() > 0) {
            filter = String.join(" AND ", filters) + " AND";
        }

        String q = String.format(queryTemplate, hops, filter);
        return q;
    }

    public List<String> execute(@NotNull String mol, @NotNull String synthon, @NotNull Integer hops,
                                Integer hacMin, Integer hacMax, Integer racMin, Integer racMax, Integer limit) {

        if (limit == null) {
            limit = 1000;
        }
        if (limit > 5000) {
            throw new IllegalArgumentException("Limits over 5000 are not supported");
        }

        // standardize the mol. It must be in smiles format
        final String stdSmiles = MolStandardize.prepareNonisoMol(mol, Constants.MIME_TYPE_SMILES);
        final String stdSynthon = MolStandardize.prepareNonisoMol(synthon, Constants.MIME_TYPE_SMILES);
        final int limitf = limit;
        final String query = expandTemplate(hops, hacMin, hacMax, racMin, racMax);

        HashMap<String, MoleculeNode> values = getSession().writeTransaction((tx) -> {
            LOG.info("Executing Synthon Query: " + query);
            Result result = tx.run(query, parameters(new Object[]{
                    "smiles", stdSmiles, "synthon", stdSynthon, "limit", limitf}));
            HashMap<String, MoleculeNode> molNodes = new HashMap<>();
            while (result.hasNext()) {
                Record rec = result.next();
                Value val = rec.get(0);
                Node node = val.asNode();
                String smi = node.get("smiles").asString();
                if (!molNodes.containsKey(smi)) {
                    molNodes.put(smi, FragmentGraph.generateMoleculeNode(node));
                }

            }
            return molNodes;
        });

        return new ArrayList(values.values());
    }
}
