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
package org.squonk.fragnet.search.queries.v2;

import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.types.Relationship;
import org.squonk.fragnet.chem.MolStandardize;
import org.squonk.fragnet.search.queries.AbstractQuery;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import static org.neo4j.driver.v1.Values.parameters;

public class FragmentQuery extends AbstractQuery {

    private static final Logger LOG = Logger.getLogger(FragmentQuery.class.getName());

    public FragmentQuery(Session session) {
        super(session);

    }

    private final String SYNTHON_QUERY = "MATCH (fa:F2 {smiles: $smiles})-[e:FRAG*]->(f:F2) RETURN e";

    @Override
    protected String getQueryTemplate() {
        return SYNTHON_QUERY;
    }

    public List<String> execute(@NotNull String mol, @NotNull String mimeType) {

        // standardize the mol. It can be in smiles or molfile formats
        String stdSmiles = MolStandardize.prepareNonisoMol(mol, mimeType);

        HashSet<String> values = getSession().writeTransaction((tx) -> {
            LOG.fine("Executing MoleculeQuery: " + SYNTHON_QUERY);
            StatementResult result = tx.run(SYNTHON_QUERY, parameters(new Object[]{"smiles", stdSmiles}));
            HashSet<String> smiles = new HashSet<>();
            while (result.hasNext()) {
                Record rec = result.next();
                Value val = rec.get(0);
                List<Object> edges = val.asList();
                for (Object o : edges) {
                    Relationship rel = (Relationship)o;
                    String label = rel.get("label").asString();
                    String[] tokens = label.split("\\|");
                    LOG.fine("Label: " + label + " Tokens: " + tokens[1] + " " + tokens[4]);
                    smiles.add(tokens[1]);
                    smiles.add(tokens[4]);
                }
            }
            return smiles;
        });

        return new ArrayList(values);
    }
}
