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
public enum GroupingType {

    DELETION,
    DELETIONS,
    FG_DELETION,
    FG_DELETIONS,
    RING_DELETION,
    RING_DELETIONS,

    ADDITION,
    ADDITIONS,
    FG_ADDITION,
    FG_ADDITIONS,
    RING_ADDITION,
    RING_ADDITIONS,

    ADDITION_DELETION,
    FG_ADDITION_DELETION,
    RING_ADDITION_DELETION,

    SUBSTITUTE_FG,
    SUBSTITUTE_LINKER,
    SUBSTITUTE_RING,

    UNDEFINED
}
