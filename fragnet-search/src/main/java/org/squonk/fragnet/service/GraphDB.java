package org.squonk.fragnet.service;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.squonk.fragnet.Utils;
import org.squonk.fragnet.service.v1.FragnetSearchRouteBuilder;

import javax.inject.Singleton;
import java.util.logging.Logger;

/** Defines the Neo4j database, and handles getting a Session from it.
 *
 */
@Singleton
public class GraphDB implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(FragnetSearchRouteBuilder.class.getName());

    private static final String NEO4J_SERVER = Utils.getConfiguration("NEO4J_SERVER", "neo4j");
    private static final String NEO4J_PORT = Utils.getConfiguration("NEO4J_PORT", "7687");
    private static final String NEO4J_USER = Utils.getConfiguration("NEO4J_USER", "neo4j");
    private static final String NEO4J_PASSWORD = Utils.getConfiguration("NEO4J_PASSWORD", null);
    private static final String NEO4J_URL = "bolt://" + NEO4J_SERVER + ":" + NEO4J_PORT;

    private Driver driver = null;

    public GraphDB() {
        LOG.info("Using to Neo4j at " + NEO4J_URL + " as user " + NEO4J_USER);
    }

    public Session getSession() {
        // If we have no database connection, then let's try and get one...
        if (driver == null) {
            driver = GraphDatabase.driver(NEO4J_URL, AuthTokens.basic(NEO4J_USER, NEO4J_PASSWORD));
        }

        return driver.session();
    }

    @Override
    public void close() throws Exception {
        if (driver != null) {
            driver.close();
        }
        driver = null;
    }
}
