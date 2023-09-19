/*
 * Copyright (c) 2023 Informatics Matters Ltd.
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
package org.squonk.fragnet.search.model.v2;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.squonk.fragnet.Utils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Result object for an ExpandMulti search executed by the HitExpander class.
 * This class is designed to be serialized to JSON.
 *
 */
@JsonPropertyOrder({"executionDate", "executionTimeMillis", "resultCount", "parameters", "queries", "hitCounts", "results"})
public class ExpandMultiResult {

    private final ConvertedSmilesMols queries;
    private final Map<String,Object> parameters;
    private List<ExpandedHit> results;
    private final Map<String,Integer> hitCounts = new LinkedHashMap<>();
    private long executionTimeMillis;
    private String executionDate = Utils.getCurrentTime();

    public ExpandMultiResult(ConvertedSmilesMols queries, Map<String,Object> parameters) {
        this.queries = queries;
        this.parameters = parameters;
    }

    public String getExecutionDate() {
        return executionDate;
    }

    public ConvertedSmilesMols getQueries() {
        return queries;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public List<ExpandedHit> getResults() {
        return results;
    }

    public void setResults(List<ExpandedHit> results) {
        this.results = results;
    }

    public int getResultCount() {
        return results.size();
    }

    public Map<String, Integer> getHitCounts() {
        return hitCounts;
    }

    public long getExecutionTimeMillis() {
        return executionTimeMillis;
    }

    public void addHitCount(String id, int count) {
        hitCounts.put(id, count);
    }

    public void setExecutionTimeMillis(long executionTimeMillis) {
        this.executionTimeMillis = executionTimeMillis;
    }
}