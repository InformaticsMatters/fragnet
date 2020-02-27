/*
 * Copyright (c) 2020 Informatics Matters Ltd.
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

import java.util.ArrayList;
import java.util.List;

@JsonPropertyOrder({"originalMimeType", "molecules"})
public class ConvertedSmilesMols {

    private final String origMimeType;
    private final List<Mol> molecules = new ArrayList<>();


    public ConvertedSmilesMols(String origMimeType) {
        this.origMimeType = origMimeType;
    }

    public String getMimeType() {
        return origMimeType;
    }

    public List<Mol> getMolecules() {
        return molecules;
    }

    public void addMol(String origMol, String stdNonisoSmiles, String id) {
        molecules.add(new Mol(origMol, stdNonisoSmiles, id));
    }

    @JsonPropertyOrder({"id", "originalMol", "smiles"})
    public class Mol {
        private final String origMol;
        private final String smiles;
        private final String id;

        Mol(String origMol, String smiles, String id) {
            this.origMol = origMol;
            this.smiles = smiles;
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public String getOriginalMol() {
            return origMol;
        }

        public String getSmiles() {
            return smiles;
        }

    }
}