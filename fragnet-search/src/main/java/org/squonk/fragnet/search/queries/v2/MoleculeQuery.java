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
import org.neo4j.driver.v1.types.Node;
import org.squonk.fragnet.chem.MolStandardize;

import org.squonk.fragnet.search.model.v2.FragmentGraph;
import org.squonk.fragnet.search.model.v2.MoleculeNode;
import org.squonk.fragnet.search.queries.AbstractQuery;

import javax.validation.constraints.NotNull;
import java.util.Map;
import java.util.logging.Logger;

import static org.neo4j.driver.v1.Values.parameters;

public class MoleculeQuery extends AbstractQuery {

    private static final Logger LOG = Logger.getLogger(MoleculeQuery.class.getName());

    public MoleculeQuery(Session session) {
        super(session);

    }

    private final String MOLECULE_QUERY = "MATCH p=(m:F2) WHERE m.smiles=$smiles RETURN m";

    @Override
    protected String getQueryTemplate() {
        return MOLECULE_QUERY;
    }

    public MoleculeNode execute(@NotNull String mol, @NotNull String mimeType) {

        // standardize the mol. It can be in smiles or molfile formats
        String stdSmiles = MolStandardize.prepareNonisoMol(mol, mimeType);

        MoleculeNode value = getSession().writeTransaction((tx) -> {
            LOG.fine("Executing MoleculeQuery: " + MOLECULE_QUERY);
            StatementResult result = tx.run(MOLECULE_QUERY, parameters(new Object[]{"smiles", stdSmiles}));

            MoleculeNode molNode = null;
            if (result.hasNext()) {
                Record rec = result.next();
                Map<String, Object> m = rec.asMap();
                Node n = (Node) m.get("m");
                molNode = FragmentGraph.generateMoleculeNode(n);
            }
            return molNode;
        });

        return value;
    }
}
