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
package org.squonk.fragnet.search.model.v2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.types.Path;
import org.squonk.fragnet.Constants;
import org.squonk.fragnet.search.FragmentUtils;

import java.util.*;
import java.util.logging.Logger;

/**
 * Represents the results of an expansion search.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({"apiVersion", "query", "parameters", "shortMessage", "longMessage", "refmol", "resultAvailableAfter", "processingTime", "pathCount", "size", "members"})
public class ExpansionResults implements Constants {

    private static final Logger LOG = Logger.getLogger(ExpansionResults.class.getName());


    /**
     * The query molecule as SMILES.
     */
    private final String refmol;
    private int pathCount;
    private String query;
    private Map<String, Object> parameters = new LinkedHashMap<>();
    private Long resultAvailableAfter;
    private Long processingTime;
    private String shortMessage;
    private String longMessage;
    private Map<String, Member> members = new LinkedHashMap<>();

    public ExpansionResults(String refmol) {
        this.refmol = refmol;
    }

    public String getRefmol() {
        return refmol;
    }

    public int getPathCount() {
        return pathCount;
    }

    public void setPathCount(int pathCount) {
        this.pathCount = pathCount;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Long getResultAvailableAfter() {
        return resultAvailableAfter;
    }

    public void setResultAvailableAfter(Long resultAvailableAfter) {
        this.resultAvailableAfter = resultAvailableAfter;
    }

    public Long getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(Long processingTime) {
        this.processingTime = processingTime;
    }

    public String getShortMessage() {
        return shortMessage;
    }

    public void setShortMessage(String shortMessage) {
        this.shortMessage = shortMessage;
    }

    public String getLongMessage() {
        return longMessage;
    }

    public void setLongMessage(String longMessage) {
        this.longMessage = longMessage;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public Collection<Member> getMembers() {
        return members.values();
    }

    public int getSize() {
        return members.size();
    }

    /**
     * Add a path to the members.
     *
     * @param path
     */
    public void add(Path path) {

        Node end = path.end();

        String smiles = FragmentUtils.getSmiles(end);
        Map<String, Object> props = new LinkedHashMap<>(end.asMap());
        props.remove("smiles");
        props.remove("osmiles");
        List<String> ids = (List) props.remove("cmpd_ids");

        Member member = members.get(smiles);
        if (member == null) {
            member = new Member(smiles, props);
            members.put(smiles, member);
        }

        if (ids != null) {
            member.addCompoundIds(ids);
        }
    }

    class Member {

        private final String smiles;
        private final Map<String, Object> props;
        private final Set<String> cmpdids = new HashSet<>();

        Member(String smiles, Map<String, Object> props) {
            this.smiles = smiles;
            this.props = props;
        }

        public String getSmiles() {
            return smiles;
        }

        @JsonProperty("cmpd_ids")
        public Set<String> getCompoundIds() {
            return cmpdids;
        }

        public Map<String, Object> getProps() {
            return props;
        }

        protected void addCompoundIds(List<String> ids) {
            cmpdids.addAll(ids);
        }
    }
}
