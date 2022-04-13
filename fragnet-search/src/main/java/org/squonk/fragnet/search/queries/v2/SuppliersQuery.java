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
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;
import org.squonk.fragnet.service.GraphDB;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class SuppliersQuery {

    private static final Logger LOG = Logger.getLogger(SuppliersQuery.class.getName());
    private static final String QUERY = "MATCH (s:Supplier) RETURN s";

    private Session session;

    public SuppliersQuery(Session session) {
        this.session = session;
    }

    public List<Map<String,String>> getSuppliers() {
        List<Map<String,String>> suppliers = session.writeTransaction((tx) -> {
            LOG.info("Executing NeighbourhoodQuery: " + QUERY);
            Result result = tx.run(QUERY);
            List<Map<String,String>> results = new ArrayList<>();
            result.stream().forEachOrdered((r) -> {
                LOG.finer("Handling record " + r);
                Map<String, Object> m = r.asMap();
                m.forEach((k,v)  -> {
                    //LOG.info("Found " + k + " -> " + v);
                    Node n = (Node)v;
                    Map<String,String> map = new LinkedHashMap<>();
                    Value nameValue = n.get("name");
                    String name = nameValue.asString();
                    Value labelValue = n.get("label");
                    String label = labelValue.asString();
                    LOG.fine("Found Supplier " + name + " label=" + label);
                    map.put("name", name);
                    map.put("label", label);
                    results.add(map);
                });
            });
            return results;
        });
        return suppliers;
    }

}


