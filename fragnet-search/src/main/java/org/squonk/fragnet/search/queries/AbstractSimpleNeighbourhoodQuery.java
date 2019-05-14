package org.squonk.fragnet.search.queries;

import org.neo4j.driver.v1.Session;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public abstract class AbstractSimpleNeighbourhoodQuery {

    private static final Logger LOG = Logger.getLogger(AbstractSimpleNeighbourhoodQuery.class.getName());

    private final Session session;
    private int limit = 1000;

    public AbstractSimpleNeighbourhoodQuery(Session session) {
        this.session = session;
    }

    protected Session getSession() {
        return session;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    protected abstract String getQueryTemplate();



}
