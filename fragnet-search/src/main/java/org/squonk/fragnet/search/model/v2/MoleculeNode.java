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

import java.util.*;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MoleculeNode {

    public enum Property {

        SMILES("smiles"),
        HEAVY_ATOM_COUNT("hac"),
        RING_ATOM_COUNT("chac");

        public final String dbname;

        Property(String dbname) {
            this.dbname = dbname;
        }

    }


    public enum MoleculeType {
        NET_MOL,  // real molecule that is part of the nonisomeric fragment network
        NET_FRAG, // a fragment that is part of the nonisomeric fragment network
        ISO_MOL   // an isomeric molecule that is not a direct part of the fragment network
    }

    private final long id;
    private final String smiles;
    private final MoleculeType molType;
    private final Map<String,Object> props = new LinkedHashMap<>();
    private final List<String> labels = new ArrayList<>();
    private final Map<String,String> paths = new HashMap<>();

    public MoleculeNode(
            @JsonProperty("id") long id,
            @JsonProperty("smiles") String smiles,
            @JsonProperty("molType") MoleculeType molType,
            @JsonProperty("labels") List<String> labels,
            @JsonProperty("props") Map<String,Object> props
            ) {
        this.id = id;
        this.smiles = smiles;
        this.molType = molType;
        if (labels != null && !labels.isEmpty()) {
            this.labels.addAll(labels);
        }
        if (props != null && !props.isEmpty()) {
            this.props.putAll(props);
        }

    }

    public long getId() {
        return id;
    }

    public String getSmiles() {
        return smiles;
    }

    public MoleculeType getMolType() {
        return molType;
    }

    public List<String> getLabels() {
        return labels;
    }

    public Map<String, Object> getProps() {
        return props;
    }

    @SuppressWarnings("Unchecked")
    public <T> T getProp(String name, Class<T> type) {
        return (T)props.get(name);
    }

    public void addPath(String path, String labels) {
        paths.put(path, labels);
    }

    public Map<String, String> getPaths() {
        return paths;
    }

    public Set<String> getPathKeys() {
        return paths.keySet();
    }

    public Collection<String> getPathLabels() {
        return paths.values();
    }

    public void addProp(String name, Object value) {
        if (value != null) {
            props.put(name, value);
        }
    }
}

