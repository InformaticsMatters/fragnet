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
package org.squonk.fragnet.search.model.v1;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MoleculeEdge {

    public enum Property {

        LABEL("label");

        public final String dbname;

        Property(String dbname) {
            this.dbname = dbname;
        }

    }

    private static final String SEP = "::";

    private final long id;
    private final long parentId;
    private final long childId;
    private final String label;

    public MoleculeEdge(
            @JsonProperty("id") long id,
            @JsonProperty("parentId") long parentId,
            @JsonProperty("childId") long childId,
            @JsonProperty("label") String label) {
        this.id = id;
        this.parentId = parentId;
        this.childId = childId;
        this.label = label;
    }

    public long getId() {
        return id;
    }

    public long getParentId() {
        return parentId;
    }

    public long getChildId() {
        return childId;
    }

    public String getLabel() {
        return label;
    }

}
