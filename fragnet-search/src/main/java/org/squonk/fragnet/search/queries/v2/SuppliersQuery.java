package org.squonk.fragnet.search.queries.v2;

import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.types.Node;
import org.squonk.fragnet.service.GraphDB;

import java.util.ArrayList;
import java.util.Collections;
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

    public List<String> getSuppliers() {
        List<String> suppliers = session.writeTransaction((tx) -> {
            LOG.info("Executing Query: " + QUERY);
            StatementResult result = tx.run(QUERY);
            List<String> results = new ArrayList<>();
            result.stream().forEachOrdered((r) -> {
                LOG.finer("Handling record " + r);
                Map<String, Object> m = r.asMap();
                m.forEach((k,v)  -> {
                    //LOG.info("Found " + k + " -> " + v);
                    Node n = (Node)v;
                    Value value = n.get("name");
                    String name = value.asString();
                    LOG.fine("Found Supplier " + name);
                    results.add(name);
                });
            });
            return results;
        });
        return suppliers;
    }

    public static final void main(String[] args) {
        GraphDB db = new GraphDB();
        Session session = db.getSession();
        SuppliersQuery query = new SuppliersQuery(session);
        List<String> suppliers = query.getSuppliers();
        LOG.info("Found " + suppliers.size() + " suppliers");
    }

}


