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
import org.squonk.fragnet.search.model.v2.GroupingType;
import org.squonk.fragnet.search.model.v2.MolTransform;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

public class TransformClassifierUtils implements Constants {

    private static final Logger LOG = Logger.getLogger(TransformClassifierUtils.class.getName());

    /**
     * Generate a 1-hop transform
     *
     * @param fromSmiles The query smiles
     * @param edge       Edge label
     * @param isAddition Is this an addition
     * @param toSmiles   The result smiles
     * @return
     */
    public static MolTransform generateMolTransform(String fromSmiles,
                                                    String edge, boolean isAddition,
                                                    String toSmiles) {

        TransformClassifier1Hop classifier = new TransformClassifier1Hop(fromSmiles, edge, isAddition, toSmiles);
        return classifier.classifyTransform();
    }


    /**
     * Generate a 2-hop transform
     *
     * @param fromSmiles  The query smiles
     * @param edge1       First edge label
     * @param isAddition1 First hop is an addition
     * @param midSmiles   The intermediate smiles
     * @param edge2       Second edge label
     * @param isAddition2 Second hop is an addition
     * @param toSmiles    The result smiles
     * @return
     */
    public static MolTransform generateMolTransform(String fromSmiles,
                                                    String edge1, boolean isAddition1,
                                                    String midSmiles,
                                                    String edge2, boolean isAddition2,
                                                    String toSmiles) {
        TransformClassifier2Hops classifier = new TransformClassifier2Hops(
                fromSmiles, edge1, isAddition1, midSmiles, edge2, isAddition2, toSmiles);

        MolTransform tx = classifier.classifyTransform();
        return tx;
    }

    public static MolTransform createUndefinedMolTransform(boolean[] isAddition) {
        if (isAddition.length == 0) {
            return new MolTransform(UNDEFINED + HYPHEN + "undefined", GroupingType.UNDEFINED, 0);
        } else if (isAddition.length == 1) {
            if (isAddition[0]) {
                return new MolTransform(UNDEFINED + HYPHEN + GroupingType.ADDITION.toString().toLowerCase(), GroupingType.ADDITION, 1);
            } else {
                return new MolTransform(UNDEFINED + HYPHEN + GroupingType.DELETION.toString().toLowerCase(), GroupingType.DELETION, 1);
            }
        } else {
            if (isAddition[0] && isAddition[1]) {
                return new MolTransform(UNDEFINED + HYPHEN + GroupingType.ADDITIONS.toString().toLowerCase(), GroupingType.ADDITIONS, 0);
            } else if (!isAddition[0] && !isAddition[1]) {
                return new MolTransform(UNDEFINED + HYPHEN + GroupingType.DELETIONS.toString().toLowerCase(), GroupingType.DELETIONS, 0);
            } else {
                return new MolTransform(UNDEFINED + HYPHEN + GroupingType.ADDITION_DELETION.toString().toLowerCase(), GroupingType.ADDITION_DELETION, 0);
            }
        }
    }

    /**
     * Generate the grouping type for this transform
     *
     * @param isAddition          Array of one or two booleans for whether each hop is an addition
     * @param parts               Array of one or two String[]s for the split edge labels
     * @param numMiddleComponents The number of disconnected components in the middle smiles (only used for a 2-hop transform)
     * @param isSubstitution      Is this an addition+deletion at the same site (only used for a 2-hop additions)
     * @return
     */
    public static GroupingType createGroupingType(boolean[] isAddition, String[][] parts, int numMiddleComponents, boolean isSubstitution) {
        GroupingType type;
        if (isAddition.length == 1) {
            if (isAddition[0]) {
                if (FG.equals(parts[0][0])) {
                    type = GroupingType.FG_ADDITION;
                } else {
                    type = GroupingType.RING_ADDITION;
                }
            } else {
                if (FG.equals(parts[0][0])) {
                    type = GroupingType.FG_DELETION;
                } else {
                    type = GroupingType.RING_DELETION;
                }
            }
        } else if (isAddition.length == 2) {
            if (!isAddition[0] && !isAddition[1]) {
                if (FG.equals(parts[0][0]) && FG.equals(parts[1][0])) {
                    type = GroupingType.FG_DELETIONS;
                } else if (RING.equals(parts[0][0]) && RING.equals(parts[1][0])) {
                    type = GroupingType.RING_DELETIONS;
                } else {
                    type = GroupingType.DELETIONS;
                }
            } else if (isAddition[0] && isAddition[1]) {
                if (FG.equals(parts[0][0]) && FG.equals(parts[1][0])) {
                    type = GroupingType.FG_ADDITIONS;
                } else if (RING.equals(parts[0][0]) && RING.equals(parts[1][0])) {
                    type = GroupingType.RING_ADDITIONS;
                } else {
                    type = GroupingType.ADDITIONS;
                }
            } else if (isAddition[0] && !isAddition[1]) {
                if (RING.equals(parts[0][0]) && RING.equals(parts[0][3]) && RING.equals(parts[1][0]) && RING.equals(parts[1][3])) {
                    type = (isSubstitution ? GroupingType.SUBSTITUTE_RING : GroupingType.RING_ADDITION_DELETION);
                } else if (FG.equals(parts[0][0]) && RING.equals(parts[0][3]) && FG.equals(parts[1][0]) && RING.equals(parts[1][3])) {
                    type = (isSubstitution ? GroupingType.SUBSTITUTE_FG : GroupingType.FG_ADDITION_DELETION);
                } else {
                    type = GroupingType.ADDITION_DELETION;
                }
            } else if (!isAddition[0] && isAddition[1]) {
                if (FG.equals(parts[0][0]) && RING.equals(parts[0][3]) && FG.equals(parts[1][0]) && RING.equals(parts[1][3])) {
                    if (numMiddleComponents == 2) {
                        type = GroupingType.SUBSTITUTE_LINKER;
                    } else {
                        type = (isSubstitution ? GroupingType.SUBSTITUTE_FG : GroupingType.FG_ADDITION_DELETION);
                    }
                } else if (RING.equals(parts[0][0]) && RING.equals(parts[0][3]) && RING.equals(parts[1][0]) && RING.equals(parts[1][3])) {
                    type = (isSubstitution ? GroupingType.SUBSTITUTE_RING : GroupingType.RING_ADDITION_DELETION);
                } else {
                    type = GroupingType.ADDITION_DELETION;
                }
            } else {
                type = GroupingType.UNDEFINED;
            }
        } else {
            type = GroupingType.UNDEFINED;
        }
        return type;
    }

    public static String createUndefinedScaffold(GroupingType type) {
        return UNDEFINED + HYPHEN + type.toString().toLowerCase();
    }

    public static MolTransform createUndefinedMolTransform(GroupingType type, int length) {
        return new MolTransform(createUndefinedScaffold(type), type, length);
    }

    public static MolTransform triageMolTransforms(Collection<MolTransform> items, boolean[] isAddition) {
        if (items.size() == 0) {
            return new MolTransform(UNDEFINED + HYPHEN + "undefined", GroupingType.UNDEFINED, isAddition.length);
        } else if (items.size() == 1) {
            return items.iterator().next();
        } else {
            Set<MolTransform> unique = new HashSet<>();
            unique.addAll(items);
            if (unique.size() == 1) {
                return unique.iterator().next();
            } else {
                // look for cases where there are paths of mixed length - we'll use the 1 hop ones in preference to the 2 hops
                Integer length = null;
                boolean mixedLengths = false;
                Iterator<MolTransform> it = unique.iterator();
                while (it.hasNext()) {
                    MolTransform tx = it.next();
                    if (length == null) {
                        length = tx.getLength();
                    } else {
                        if (length != tx.getLength()) {
                            mixedLengths = true;
                            break;
                        }
                    }
                }
                if (mixedLengths) {
                    it = unique.iterator();
                    while (it.hasNext()) {
                        MolTransform tx = it.next();
                        if (tx.getLength() > 1) {
                            it.remove();
                        }
                    }
                }
                if (unique.size() == 1) {
                    return unique.iterator().next();
                }

                // OK, so that didn't work so let's remove any undefined ones and accept the other(s)
                it = unique.iterator();
                while (it.hasNext()) {
                    MolTransform tx = it.next();
                    if (tx.getClassification() == GroupingType.UNDEFINED) {
                        it.remove();
                    }
                }
                if (unique.size() == 1) {
                    LOG.warning("Grouping contained undefined items");
                    return unique.iterator().next();
                } else {
                    LOG.warning("Inconsistent groupings");
                    return createUndefinedMolTransform(isAddition);
                }
            }
        }
    }


}
