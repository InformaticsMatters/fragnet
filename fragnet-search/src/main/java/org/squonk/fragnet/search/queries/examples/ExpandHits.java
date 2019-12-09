package org.squonk.fragnet.search.queries.examples;

import org.neo4j.driver.v1.Session;
import org.squonk.fragnet.Utils;
import org.squonk.fragnet.search.model.v2.ExpansionResults;
import org.squonk.fragnet.search.queries.v2.ExpansionQuery;
import org.squonk.fragnet.service.GraphDB;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExpandHits {

    private static final Logger LOG = Logger.getLogger(ExpandHits.class.getName());

    static {
        System.loadLibrary("GraphMolWrap");
    }

    private static final Integer HOPS = new Integer(Utils.getConfiguration("HOPS", "1"));
    private static final Integer HAC = new Integer(Utils.getConfiguration("HAC", "3"));
    private static final Integer RAC = new Integer(Utils.getConfiguration("RAC", "1"));
    private static final String INPUT = Utils.getConfiguration("INPUT", "input.smi");

    GraphDB db;

    public ExpandHits() {
        db = new GraphDB();
    }

    ExpansionResults executeQuery(String smiles, int hops, int hac, int rac) throws IOException {
        Session session = db.getSession();
        ExpansionQuery query = new ExpansionQuery(session, null);
        ExpansionResults results = query.executeQuery(smiles, "chemical/x-daylight-smiles", hops, hac, rac, null);
        return results;
    }

    static List<String> readSmiles(String file) throws IOException {

        Path path = FileSystems.getDefault().getPath(file);

        Stream<String> lines = Files.lines(path);
        return lines.collect(Collectors.toList());
    }


    public static void main(String[] args) throws IOException {


        ExpandHits e = new ExpandHits();
        List<String> molecules = readSmiles(INPUT);
        LOG.info("Number of input smiles = " + molecules.size());

        Map<String,Set<String>> results = new LinkedHashMap<>();

        for (String smiles : molecules) {
            LOG.info("Processing " + smiles);
            ExpansionResults result = e.executeQuery(smiles, HOPS, HAC, RAC);
            for (ExpansionResults.Member m : result.getMembers()) {
                String s = m.getSmiles();
                if (!results.containsKey(s)) {
                    results.put(s, m.getCompoundIds());
                }
            }
        }

        LOG.info("Total number of results = " + results.size());

        for (Map.Entry<String,Set<String>> item : results.entrySet()) {
            System.out.println(item.getKey() + "\t" + item.getValue().stream().collect(Collectors.joining(",")));
        }

        LOG.info("Processing complete");

    }
}
