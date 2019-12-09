package org.squonk.fragnet.search.queries.examples;

import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Value;
import org.squonk.fragnet.Utils;
import org.squonk.fragnet.service.GraphDB;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AllMols {

    private static final Logger LOG = Logger.getLogger(AllMols.class.getName());

    static {
        System.loadLibrary("GraphMolWrap");
    }

    private static final Integer LIMIT = new Integer(Utils.getConfiguration("LIMIT", "1000"));

    GraphDB db;

    public AllMols() {
        db = new GraphDB();
    }

    void executeQuery() throws IOException {

        String cypher = "MATCH (m:Mol:F2) RETURN m.smiles, m.cmpd_ids";

        if (LIMIT > 0) {
            cypher += " LIMIT " + LIMIT;
        }
        final String query = cypher;

        Session session = db.getSession();
        session.writeTransaction((tx) -> {
            LOG.info("Executing Query: " + query);
            StatementResult result = tx.run(query);

            AtomicInteger count = new AtomicInteger(0);
            result.stream().forEachOrdered((r) -> {
//                LOG.info("Handling record " + r);
                Map<String, Object> m = r.asMap();
                String smiles = r.get("m.smiles").asString();
                List ids = r.get("m.cmpd_ids").asList();
                System.out.println(smiles + "\t" + ids.stream().collect(Collectors.joining(",")));
                count.incrementAndGet();
            });

            LOG.info("Processed " + count.get() + " molecules");

            return null;
        });


    }



    public static void main(String[] args) throws IOException {


        AllMols e = new AllMols();
        e.executeQuery();

        LOG.info("Processing complete");

    }
}
