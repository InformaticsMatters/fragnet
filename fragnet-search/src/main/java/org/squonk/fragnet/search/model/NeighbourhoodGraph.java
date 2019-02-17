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
package org.squonk.fragnet.search.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.types.Path;
import org.neo4j.driver.v1.types.Relationship;

import java.util.*;
import java.util.logging.Logger;

/** Represents the results of a neighbourhood search, containing nodes, edges and groupings that are classified by the
 * transformation type. Each group represents a particular category of transformation e.g. deletion of the 4-OH group
 * and will contain one or more nodes. Each group thus represents a transform 'vector' and can involve one or two edges.
 *
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({"query", "parameters", "refmol", "resultAvailableAfter", "processingTime", "calculationTime", "nodes", "edges", "groups"})
public class NeighbourhoodGraph extends FragmentGraph {

    private static final Logger LOG = Logger.getLogger(NeighbourhoodGraph.class.getName());
    private static final String SEP = "::";

    /** The classifications of the types of transforms/vectors.
     *
     */
    public enum GroupingType {
        DELETION,
        DELETIONS,
        ADDITION,
        ADDITIONS,
        SUBSTITUTE_FG,
        SUBSTITUTE_LINKER,
        ADDITION_DELETION,
        UNDEFINED
    }

    /**
     * The query molecule as SMILES.
     */
    private final String refmol;
    private Grouping grouping = new Grouping();


    public NeighbourhoodGraph(String refmol) {
        this.refmol = refmol;
    }

    public String getRefmol() {
        return refmol;
    }

    public Collection<Group> getGroups() {
        return grouping.collectGroups();
    }

    public int numGroups() {
        return grouping.size();
    }

    /**
     * Add a path to this NeighbourhoodGraph. The first element of the path will be the node for the query molecule and
     * last element is the node for the molecule that is a neighbour as defined by the query.
     * In a 1-hop scenario there will be a single edge linking these two nodes.
     * In a 2-hop scenario there will be an intermediate node and two edges.
     *
     * @param path
     */
    @Override
    public void add(Path path) {

        super.add(path);

        MoleculeNode start = nodes.get(path.start().id());
        MoleculeNode end = nodes.get(path.end().id());

        assert start != null;
        assert end != null;

        Number startHac = start.getProp("hac", Number.class);
        Number endHac = end.getProp("hac", Number.class);
        String msg = path.length() + " " + start.getSmiles() + " -> " + end.getSmiles();

        Relationship rel;
        String label;
        String[] parts;
        switch (path.length()) {
            case 0:
                LOG.finer("Path length 0");
                break;

            case 1:
                LOG.finer("Path length 1");
                rel = path.relationships().iterator().next();
                label = FragmentUtils.getLabel(rel);
                LOG.fine(msg + "\nLabel: " + label);
                parts = label.split("\\|");
//                if (startHac == endHac) {
//                    // wierd?
//                } else if (startHac < endHac) {
//                    LOG.info("Addition");
//                    findOrCreateGrouping(GroupingType.ADDITION, parts).addMember(end.getSmiles(), new String[]{label}, null);
//                } else if (startHac > endHac) {
//                    LOG.info("Deletion");
//                    findOrCreateGrouping(GroupingType.DELETION, parts).addMember(end.getSmiles(), new String[]{label}, null);
//                }
//
//                end.addPath(parts[4], label);

                grouping.add(end, edges.get(rel.id()));

                break;

            case 2:
                LOG.finer("Path length 2");

//                String s = "SMILES: " + start.getSmiles() + " -> " + end.getSmiles();
//                String[][] labelParts = new String[2][];
//                String[] labelStrings = new String[2];
//                Node[] pathNodes = new Node[3];
//                String[] smiles = new String[3];
//                int i = 0;
//                for (Path.Segment seg : path) {
//                    Node startNode = seg.start();
//                    Node endNode = seg.end();
//                    String startSmiles = FragmentUtils.getSmiles(startNode);
//                    String endSmiles = FragmentUtils.getSmiles(endNode);
//                    if (i == 0) {
//                        smiles[i] = startSmiles;
//                        pathNodes[i] = startNode;
//                    }
//                    smiles[i + 1] = endSmiles;
//                    pathNodes[i + 1] = seg.end();
//                    Relationship r = seg.relationship();
//                    label = FragmentUtils.getLabel(r);
//                    msg += "\nLabel: " + label;
//                    Node[] relationshipNodes = FragmentUtils.orderNodesFromRelationship(seg);
//                    msg += "\n" + FragmentUtils.getSmiles(relationshipNodes[0]) + " -> " + FragmentUtils.getSmiles(relationshipNodes[1]);
//                    labelStrings[i] = label;
//                    parts = label.split("\\|");
//                    labelParts[i] = parts;
//                    i++;
//                }
//                LOG.info(msg);
//                boolean firstBigger = FragmentUtils.getHeavyAtomCount(pathNodes[0]) < FragmentUtils.getHeavyAtomCount(pathNodes[1]);
//                boolean secondBigger = FragmentUtils.getHeavyAtomCount(pathNodes[1]) < FragmentUtils.getHeavyAtomCount(pathNodes[2]);
//                if (firstBigger && secondBigger) {
//                    findOrCreateGrouping(GroupingType.ADDITIONS, labelParts[0]).addMember(end.getSmiles(), labelStrings, smiles[1]);
//                } else if (!firstBigger && !secondBigger) {
//                    findOrCreateGrouping(GroupingType.DELETIONS, labelParts[0]).addMember(end.getSmiles(), labelStrings, smiles[1]);
//                } else {
//                    if (labelParts[0][3].equals(labelParts[1][3]) && labelParts[0][4].equals(labelParts[1][4])) {
//                        findOrCreateGrouping(GroupingType.SUBSTITUTE_LINKER, labelParts[0]).addMember(end.getSmiles(), labelStrings, smiles[1]);
//                    } else {
//                        findOrCreateGrouping(GroupingType.ADDITION_DELETION, labelParts[0]).addMember(end.getSmiles(), labelStrings, smiles[1]);
//                    }
//                }
//
//                end.addPath(labelParts[0][4] + SEP + labelParts[1][4], labelStrings[0] + SEP + labelStrings[1]);

                int i = 0;
                MoleculeEdge[] edges = new MoleculeEdge[2];
                for (Path.Segment seg : path) {
                    Relationship r = seg.relationship();
                    edges[i] = this.edges.get(r.id());
                    msg += "\nLabel: " + FragmentUtils.getLabel(r);
                    Node[] relationshipNodes = FragmentUtils.orderNodesFromRelationship(seg);
                    msg += "\n" + FragmentUtils.getSmiles(relationshipNodes[0]) + " -> " + FragmentUtils.getSmiles(relationshipNodes[1]);
                    i++;
                }
                LOG.fine(msg);
                grouping.add(end, edges);

                break;
        }
    }

    static String generateGroupingKey(GroupingType groupingType, String code, String smiles) {
        //return groupingType.toString() + SEP + code + SEP + smiles;
        return smiles;
    }

    private String[] splitLabel(String label) {
        return label.split("\\|");
    }

    protected class Grouping {

        private final Map<Long, GroupMember> members = new LinkedHashMap<>();

        /** Fetch the GroupMember for the node, creating it if it doesn't yet exist.
         *
         * @param node The node
         * @return The GroupMember for the node
         */
        protected GroupMember findOrCreateMember(MoleculeNode node) {
            GroupMember m = members.get(node.getId());
            if (m == null) {
                m = new GroupMember(node);
                members.put(node.getId(), m);
            }
            return m;
        }

        protected int size() {
            return members.size();
        }

        /** Add the molecule node with these edges.
         * The node is used to fetch or create the GroupMember for the node, and then the edges are added to it.
         * Where the same node is added with different edges those edges will be added to the same GroupMemeber.
         * This happens in the case of a 2-hops where there are alternative paths to the node.
         *
         * @param node
         * @param edges
         */
        protected void add(MoleculeNode node, MoleculeEdge... edges) {
            GroupMember m = findOrCreateMember(node);
            m.addEdges(edges);
        }

        protected Collection<Group> collectGroups() {
            Map<String, Group> result = new LinkedHashMap<>();
            members.values().forEach((m) -> {
                String key = m.generateGroupingKey();
                Group group = result.get(key);
                if (group == null) {
                    group = new Group(key);
                    result.put(key, group);
                }
                group.addMember(m);
            });
            return result.values();
        }

    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public class Group {

        private final String key;
        private GroupingType classification;
        private final List<GroupMember> members = new ArrayList<>();

        protected Group(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public List<GroupMember> getMembers() {
            return members;
        }

        protected void addMember(GroupMember member) {
            members.add(member);
        }

        public GroupingType getClassification() {

            if (classification == null) {

                GroupMember first = members.get(0);
                List<Long[]> edgeIdsList = first.getEdgeIds();
                GroupingType result = null;
                for (Long[] edgeIds : edgeIdsList) {
                    if (edgeIds.length == 1) {
                        if (edgeIds[0] > 0) {
                            result = determineClassification(result, GroupingType.ADDITION);
                        } else {
                            result = determineClassification(result, GroupingType.DELETION);
                        }
                    } else if (edgeIds.length == 2) {
                        if (edgeIds[0] > 0 && edgeIds[1] > 0) {
                            result = determineClassification(result, GroupingType.ADDITIONS);
                        } else if (edgeIds[0] < 0 && edgeIds[1] < 0) {
                            result = determineClassification(result, GroupingType.DELETIONS);
                        } else {
                            // TODO - this can sometimes be better classified as SUBSTITUTE_FG and SUBSTITUTE_LINKER
                            result = determineClassification(result, GroupingType.ADDITION_DELETION);
                        }
                    } else {
                        LOG.warning("Unexpected number of edges to classify: " + edgeIds.length);
                    }
                }

                if (result == null) {
                    LOG.warning("Unable to classify group " + getKey());
                    result = GroupingType.UNDEFINED;
                }
                classification = result;
            }

            return classification;
        }

        private GroupingType determineClassification(GroupingType current, GroupingType proposed) {
            if (current == null) {
                return proposed;
            } else if (current == GroupingType.UNDEFINED) {
                return current;
            } else if (current == proposed){
                return current;
            } else {
                LOG.warning("Inconsistent classification encountered");
                return GroupingType.UNDEFINED;
            }
        }

    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public class GroupMember {

        private final Long id;
        private final MoleculeNode node;
        private final List<MoleculeEdge[]> edges = new ArrayList<>();

        protected GroupMember(MoleculeNode node, MoleculeEdge[] edges) {
            this.id = node.getId();
            this.node = node;
            this.edges.add(edges);
        }

        protected GroupMember(MoleculeNode node) {
            this.id = node.getId();
            this.node = node;
        }

        public Long getId() {
            return id;
        }

        public String getSmiles() {
            return node.getSmiles();
        }

        /** Get the IDs of the edges.
         * This is a List of Long[] values. The list is typically of size 1, except for 2-hop scenarios when there are
         * multiple paths from the query to the node e.g. in the case of an addition followed by a deletion, or vice-versa.
         * The Long[] array is the IDs of the edges that are traversed from the query node to this node, with the ID being
         * positive for parent to child edges (e.g. a molecule to its fragment) or negative for child to parent edges.
         * TODO - check that this logic is correct!
         *
         * @return
         */
        public List<Long[]> getEdgeIds() {
            List<Long[]> result = new ArrayList<>();
            for (MoleculeEdge[] es : this.edges) {
                Long[] ids = new Long[es.length];
                int i = es.length - 1;
                long idToMatch = node.getId();
                while (i >= 0) {
                    MoleculeEdge e = es[i];
                    if (idToMatch == e.getChildId()) {
                        ids[i] = e.getId();
                        idToMatch = e.getParentId();
                    } else if (idToMatch == e.getParentId()) {
                        ids[i] = 0 - e.getId();
                        idToMatch = e.getChildId();
                    } else {
                        LOG.warning("Edges don't match");
                        return null;
                    }
                    i -=1;
                }
                result.add(ids);
            }

            return result;
        }

        protected void addEdges(MoleculeEdge[] edges) {
            this.edges.add(edges);
        }

        private String[] fetchLabels(MoleculeEdge[] edges) {
            String[] labels = new String[edges.length];
            for (int i = 0; i < edges.length; i++) {
                labels[i] = edges[i].getLabel();
            }
            return labels;
        }

        protected String generateGroupingKey() {
            if (edges.size() == 1) {
                String[] onlyLabels = fetchLabels(edges.get(0));
                if (onlyLabels.length == 1) {
                    String[] parts = splitLabel(onlyLabels[0]);
                    return parts[4];
                } else if (onlyLabels.length == 2) {
                    String[] parts0 = splitLabel(onlyLabels[0]);
                    String[] parts1 = splitLabel(onlyLabels[1]);
                    return parts0[4] + SEP + parts1[4];
                }
            } else if (edges.size() == 2) {
                String[] labels0 = fetchLabels(edges.get(0));
                String[] labels1 = fetchLabels(edges.get(1));
                String[] parts0 = splitLabel(labels0[0]);
                String[] parts1 = splitLabel(labels1[0]);
                if (parts0[4].compareTo(parts1[4]) < 0) {
                    return parts0[4] + "$$" + parts1[4];
                } else {
                    return parts1[4] + "$$" + parts0[4];
                }
            } else {
                // TODO - don't know how to handle these yet so we just generate some key
                String s = "UNRECOGNISED";
                for (MoleculeEdge[] edgesArray: edges) {
                    for (MoleculeEdge edge: edgesArray) {
                        s = "##" + edge.getLabel();
                    }
                }
                return s;
            }

            LOG.warning("Failed to generate grouping key");
            return null;
        }

    }

}
