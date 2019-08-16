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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Availability {

    private final String smiles;
    private final List<Item> items = new ArrayList<>();

    public Availability(String smiles) {
        this.smiles = smiles;
    }

    public Availability(
            @JsonProperty("smiles") String smiles,
            @JsonProperty("items") List<Item> items) {
        this.smiles = smiles;
        this.items.addAll(items);
    }

    public void addItem(String supplier, String code, String smiles) {
        items.add(new Item(supplier, code, smiles));
    }

    public String getSmiles() {
        return smiles;
    }

    public List<Item> getItems() {
        return Collections.unmodifiableList(items);
    }

    static class Item {
        protected String supplier;
        protected String code;
        protected String smiles;

        public Item(@JsonProperty("supplier") String supplier,
             @JsonProperty("code") String code,
             @JsonProperty("smiles") String smiles) {
            this.supplier = supplier;
            this.code = code;
            this.smiles = smiles;
        }

        public String getSupplier() {
            return supplier;
        }

        public String getCode() {
            return code;
        }

        public String getSmiles() {
            return smiles;
        }
    }

}
