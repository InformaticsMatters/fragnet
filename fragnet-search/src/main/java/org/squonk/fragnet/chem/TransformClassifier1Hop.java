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
package org.squonk.fragnet.chem;

import org.squonk.fragnet.Constants;
import org.squonk.fragnet.search.FragmentUtils;
import org.squonk.fragnet.search.model.v2.GroupingType;
import org.squonk.fragnet.search.model.v2.MolTransform;

import java.util.logging.Logger;

class TransformClassifier1Hop implements Constants {

    private static final Logger LOG = Logger.getLogger(TransformClassifier1Hop.class.getName());

    private final String fromSmiles;
    private final String edge;
    private final String[] parts;
    boolean isAddition;
    private final String toSmiles;

    TransformClassifier1Hop(String fromSmiles,
                                   String edge, boolean isAddition,
                                   String toSmiles) {
        this.fromSmiles = fromSmiles;
        this.edge = edge;
        this.parts = FragmentUtils.splitLabel(edge);
        this.isAddition = isAddition;
        this.toSmiles = toSmiles;
    }


    MolTransform classifyTransform() {
        String scaffold;
        GroupingType type = TransformClassifierUtils.createGroupingType(new boolean[]{isAddition}, new String[][]{parts}, 0, false);

        if (isAddition) {
            scaffold = parts[4];
        } else {
            scaffold = toSmiles;
        }
        LOG.info(type + SPACE + scaffold);

        if (type == null) {
            type = TransformClassifierUtils.createGroupingType(new boolean[]{isAddition}, new String[][]{parts}, 0, false);
        }
        return new MolTransform(scaffold, type, 1);

    }

}
