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

import java.util.HashSet;
import java.util.Set;

public class ExpandedHit {

    private final String smiles;
    private final Set<String> vendorIds = new HashSet<>();
    private final Set<String> sourceMols = new HashSet<>();

    public ExpandedHit(String smiles, Set<String> vendorIds) {
        this.smiles = smiles;
        this.vendorIds.addAll(vendorIds);
    }

    public void addSourceMol(String smiles) {
        sourceMols.add(smiles);
    }

    public String getSmiles() {
        return smiles;
    }

    public Set<String> getVendorIds() {
        return vendorIds;
    }

    public Set<String> getSourceMols() {
        return sourceMols;
    }
}
