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

/**
 * The classifications of the types of transforms/vectors.
 */
public enum GroupingType{

    FG_ADDITION(1),
    RING_ADDITION(2),
    ADDITION(3),
    RING_ADDITIONS(4),
    FG_ADDITIONS(5),
    ADDITIONS(6),

    FG_ADDITION_DELETION(7),
    RING_ADDITION_DELETION(8),
    ADDITION_DELETION(9),

    SUBSTITUTE_FG(10),
    SUBSTITUTE_LINKER(11),
    SUBSTITUTE_RING(12),

    FG_DELETION(13),
    RING_DELETION(14),
    DELETION(15),
    FG_DELETIONS(16),
    RING_DELETIONS(17),
    DELETIONS(18),

    UNDEFINED(19);

    private final int order;
    GroupingType(int order) {
        this.order = order;
    }

    public Integer getOrder() {
        return order;
    }
}
