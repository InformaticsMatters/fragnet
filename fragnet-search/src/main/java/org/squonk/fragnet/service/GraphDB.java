package org.squonk.fragnet.service;

import org.neo4j.driver.*;
import org.squonk.fragnet.Utils;
import org.squonk.fragnet.service.v1.FragnetSearchRouteBuilder;

import javax.inject.Singleton;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Defines the Neo4j database, and handles getting a Session from it.
 */
@Singleton
public class GraphDB implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(FragnetSearchRouteBuilder.class.getName());

    private static final String NEO4J_SERVER = Utils.getConfiguration("NEO4J_SERVER", "neo4j");
    private static final String NEO4J_PORT = Utils.getConfiguration("NEO4J_PORT", "7687");
    private static final String NEO4J_USER = Utils.getConfiguration("NEO4J_USER", "neo4j");
    private static final String NEO4J_PASSWORD = Utils.getConfiguration("NEO4J_PASSWORD", null);
    private static final String NEO4J_URL = "bolt://" + NEO4J_SERVER + ":" + NEO4J_PORT;

    private Future<Driver> future = null;
    /** Series of delays to try when getting a connection to the database.
     * See the {@link #createDriverFuture() method for details}
     * These waits add up to 32 secs
     */
    private static final int[] DELAYS = new int[]{1, 1, 1, 3, 3, 3, 5, 5, 5, 5};


    public GraphDB() {
        LOG.info("Using to Neo4j at " + NEO4J_URL + " as user " + NEO4J_USER);
    }

    /** Utility method that allows to check if the database can be connected to
     *
     * @param timeout_secs The number of seconds to wait for
     * @return True if connection can be obtained, otherwise false
     */
    public boolean connectionOK(int timeout_secs) {
        try {
            Config config = Config
                    .builder()
                    .withConnectionTimeout(timeout_secs, TimeUnit.SECONDS)
                    .build();
            Driver driver = GraphDatabase.driver(NEO4J_URL, AuthTokens.basic(NEO4J_USER, NEO4J_PASSWORD), config);
            if (driver != null) {
                driver.close();
                return true;
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "DB connection failed", ex);
        }
        return false;
    }

    /** Get a read-only session
     *
     * @return
     * @throws IOException
     */
    public Session getSession() throws IOException {
        return getSession(AccessMode.READ);
    }

    /** Get a session of the specified type.
     * This method may block if the database is still starting up. See the DELAYS property.
     *
     * @param mode
     * @return
     * @throws IOException
     */
    public Session getSession(AccessMode mode) throws IOException {
        SessionConfig config = SessionConfig.builder()
                .withDefaultAccessMode(mode)
                .build();

        try {
            return getDriver().session(config);
        } catch (ExecutionException | InterruptedException ex) {
            throw new IOException("Failed to connect to database", ex);
        }
    }

    /**  Get a driver which allows to create sessions for the database.
     * This method may block if the database is still starting up. See the DELAYS property.
     *
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    protected Driver getDriver() throws ExecutionException, InterruptedException {
        if (future == null) {
            future = createDriverFuture();
        }
        return future.get();
    }

    /** Creates a Future that provides access to the database. If the database is still starting up then creating the
     * driver will be retried a number of times (see the DELAYS property) before eventually giving up.
     *
     * @return
     */
    private synchronized Future<Driver> createDriverFuture() {

        if (future != null) {
            return future;
        }

        CompletableFuture<Driver> future = new CompletableFuture<>();

        Thread t = new Thread() {
            @Override
            public void run() {
                Throwable lastException = null;
                long t0 = new Date().getTime();
                for (int delay : DELAYS) {
                    try {
                        Driver driver = GraphDatabase.driver(NEO4J_URL, AuthTokens.basic(NEO4J_USER, NEO4J_PASSWORD));
                        if (driver != null) {
                            // we have a driver so we are ready to roll
                            future.complete(driver);
                            long t1 = new Date().getTime();
                            LOG.info("Obtained driver after " + (t1 - t0) + "ms");
                            return;
                        }
                    } catch (Throwable ex) {
                        lastException = ex;
                    }
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.log(Level.FINE, "Failed to get driver. Waiting " + delay + " secs", lastException);
                    } else {
                        LOG.info("Failed to get driver. Waiting " + delay + ": " + lastException.getLocalizedMessage());

                    }
                    try {
                        Thread.sleep(delay * 1000);
                    } catch (InterruptedException iex) {
                        LOG.log(Level.WARNING, "Sleep failed. Strange!", iex);
                        future.completeExceptionally(iex);
                        return;
                    }
                }
                future.completeExceptionally(lastException);
            }
        };
        t.start();

        return future;
    }

    @Override
    public void close() throws Exception {
        Driver driver = null;
        if (future != null) {
            driver = getDriver();
            future = null;
        }
        if (driver != null) {
            driver.close();
        }
    }

}
