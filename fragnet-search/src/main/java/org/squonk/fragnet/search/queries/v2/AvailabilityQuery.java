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

import org.neo4j.driver.Session;
import org.neo4j.driver.Result;
import org.neo4j.driver.types.Node;
import org.squonk.fragnet.search.model.v2.Availability;

import java.util.Map;
import java.util.logging.Logger;

import static org.neo4j.driver.Values.parameters;

public class AvailabilityQuery {

    private static final Logger LOG = Logger.getLogger(AvailabilityQuery.class.getName());
    private static final String QUERY = "MATCH (m:F2)-[:HasVendor]->(a:Available)-[:Availability]->(s:Supplier) " +
            "WHERE m.smiles = $smiles RETURN DISTINCT a,s";

    private Session session;

    public AvailabilityQuery(Session session) {
        this.session = session;
    }

    public Availability getAvailability(String smiles) {
        Availability value = session.writeTransaction((tx) -> {
            LOG.fine("Executing NeighbourhoodQuery: " + QUERY);
            Result result = tx.run(QUERY, parameters(new Object[] {"smiles", smiles}));
            Availability availability = new Availability(smiles);
            result.stream().forEachOrdered((r) -> {
                LOG.fine("Handling record " + r);
                Map<String, Object> m = r.asMap();
                Node a = (Node)m.get("a");
                Node s = (Node)m.get("s");
                String supplier = s.get("name").asString();
                String code = a.get("cmpd_id").asString();
                String osmiles = a.get("osmiles").asString();
                availability.addItem(supplier, code, osmiles);
            });
            return availability;
        });
        return value;
    }

}


