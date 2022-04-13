package org.squonk.fragnet.search.queries;

import org.neo4j.driver.Session;

import java.util.logging.Logger;

public abstract class AbstractQuery {

    private static final Logger LOG = Logger.getLogger(AbstractQuery.class.getName());
    public static final int DEFAULT_LIMIT=5000;

    private final Session session;
    private int limit = DEFAULT_LIMIT;

    public AbstractQuery(Session session) {
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
