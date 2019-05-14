package org.squonk.fragnet.search.queries;

import java.util.ArrayList;
import java.util.List;

public class QueryAndParams {
    private final String query;
    private final List<Object> params = new ArrayList();

    public QueryAndParams(String query, List<Object> params) {
        this.query = query;
        this.params.addAll(params);
    }

    public String getQuery() {
        return query;
    }

    public List<Object> getParams() {
        return params;
    }
}
