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


import java.util.logging.Logger;

/**
 * The classifications of the types of transforms/vectors.
 */
public enum GroupingType {

    FG_ADDITION(1, 10),

    ADDITION(2, 19),

    SUBSTITUTE_FG(3, 30),
    FG_ADDITIONS(4, 31),
    ADDITIONS(5, 39),

    FG_ADDITION_DELETION(6, 40),

    RING_ADDITION(7, 11),
    RING_ADDITIONS(8, 38),
    SUBSTITUTE_RING(9, 33),
    RING_ADDITION_DELETION(10, 41),

    ADDITION_DELETION(11, 49),

    SUBSTITUTE_LINKER(12, 42),

    FG_DELETION(13, 1),
    RING_DELETION(14, 2),
    DELETION(15, 3),
    FG_DELETIONS(16, 20),
    RING_DELETIONS(17, 21),
    DELETIONS(18, 22),

    UNDEFINED(19, 100);

    private static Logger LOG = Logger.getLogger(GroupingType.class.getName());

    private final int order;
    private final int priority;

    GroupingType(int order, int priority) {
        this.order = order;
        this.priority = priority;
    }

    public Integer getOrder() {
        return order;
    }

    public int getPriority() {
        return priority;
    }

    public TransformData prioritise(GroupingType other, TransformData myData, TransformData otherData) {
        int pa = getPriority();
        int pb = other.getPriority();
        if (this == other) {

            if (myData.getNumMidComponents() < otherData.getNumMidComponents()) {
                return myData;
            } else if (myData.getNumMidComponents() > otherData.getNumMidComponents()) {
                return otherData;
            }
            boolean[] myIsAdditions = myData.getIsAdditions();
            boolean[] otherIsAdditions = otherData.getIsAdditions();

            // these are likely to be addition/deletion so we prefer deletion+addition to addition+deletion
            if (myIsAdditions[1]) {
                return myData;
            } else {
                return otherData;
            }

        } else if (pa < pb) {
            return myData;
        } else {
            return otherData;
        }
    }

}
