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

import javax.validation.constraints.NotNull;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MolTransform {

    private final String scaffold;
    private final GroupingType classification;
    private final int length;

    public MolTransform(@NotNull String scaffold, @NotNull GroupingType classification, int length) {
        this.scaffold = scaffold;
        this.classification = classification;
        this.length = length;
    }

    public String getScaffold() {
        return scaffold;
    }

    public GroupingType getClassification() {
        return classification;
    }

    public int getLength() {
        return length;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MolTransform) {
            MolTransform other = (MolTransform) obj;
            return scaffold.equals(other.getScaffold()) && classification.equals(other.getClassification()) && length == other.getLength();
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(scaffold, classification, length);
    }

    @Override
    public String toString() {
        return "MolTransform: [" + scaffold + ", " + classification + ", " + length + "]";
    }
}
