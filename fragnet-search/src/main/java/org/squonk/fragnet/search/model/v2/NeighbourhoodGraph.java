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
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.RDKit.MCSResult;
import org.RDKit.RDKFuncs;
import org.RDKit.ROMol_Vect;
import org.RDKit.RWMol;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;
import org.squonk.fragnet.Constants;
import org.squonk.fragnet.chem.TransformClassifierUtils;
import org.squonk.fragnet.search.FragmentUtils;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the results of a neighbourhood search, containing nodes, edges and groupings that are classified by the
 * transformation type. Each group represents a particular category of transformation e.g. deletion of the 4-OH group
 * and will contain one or more nodes. Each group thus represents a transform 'vector' and can involve one or two edges.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({"apiVersion", "query", "parameters", "shortMessage", "longMessage", "refmol", "resultAvailableAfter", "processingTime", "calculationTime", "pathCount", "nodeCount", "edgeCount", "groupCount", "groupMemberCount", "nodes", "edges", "groups"})
public class NeighbourhoodGraph extends FragmentGraph implements Constants {

    private static final Logger LOG = Logger.getLogger(NeighbourhoodGraph.class.getName());
    private static final String SEP = "::";

    private static final String SUBSTITUTION_FG = "FG";
    private static final String SUBSTITUTION_RING = "RING";

    /**
     * The query molecule as SMILES.
     */
    private final String refmol;
    private final Integer groupLimit;
    private Grouping grouping = new Grouping();
    private int pathCount;

    public NeighbourhoodGraph(String refmol, Integer groupLimit) {
        this.refmol = refmol;
        this.groupLimit = groupLimit;
    }

    public String getRefmol() {
        return refmol;
    }

    public int getPathCount() {
        return pathCount;
    }

    public void setPathCount(int pathCount) {
        this.pathCount = pathCount;
    }

    public Collection<Group> getGroups() {
        return grouping.getGroups();
    }

    public int getGroupCount() {
        return getGroups().size();
    }

    public int getGroupMemberCount() {
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

                grouping.add(end, edges.get(rel.id()));

                break;

            case 2:
                LOG.finer("Path length 2");
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

    /**
     * Generate information about the groups and truncate the list of members if needed.
     * What is generated is an active area of development. Currently it comprises:
     * - a sorted list of group members, sorted by lowest HAC and then if cases of a tie MW
     * - the first sorted member becomes the prototype for the group
     * - the number of atoms in the refmol that are not present in the MCS of the refmol and group members
     * <p>
     * The aim is to generate a prototype structure that better represents the group, probably by creating an
     * R-group representation, but currently the member with the smallest number of atoms is used.
     */
    public void generateGroupInfo() {
        getGroups().parallelStream().forEach((g) -> generateGroupInfo(g));
    }


    protected void generateGroupInfo(Group group) {

        group.sortMembersByHacAndTruncate(groupLimit);

        // TODO generate a more meaningful prototype structure such as an R-group representation of the group

        // generate MCS so that we can determine how many atoms have been lost etc.
        ROMol_Vect mols = new ROMol_Vect();
        RWMol m = fetchMolecule(refmol);
        if (m == null) {
            LOG.warning("Can't obtain the refmol for " + refmol + ". Can't continue");
        } else {
            mols.add(m);
            int refMolAtoms = (int) m.getNumAtoms();
            for (GroupMember member : group.getMembers()) {
                String smiles = member.getSmiles();
                RWMol mol = fetchMolecule(smiles);
                if (mol != null) {
                    mols.add(mol);
                }
            }

            if (mols.size() > 1) {
                long t0 = System.nanoTime();
                MCSResult mcs = RDKFuncs.findMCS(mols);
                long t1 = System.nanoTime();
                int mcsAtoms = (int) mcs.getNumAtoms();
                String smarts = mcs.getSmartsString();
                LOG.fine("Refmol/MCS Atoms: " + refMolAtoms + "/" + mcsAtoms + " Took: " + (t1 - t0) +
                        "ns Smarts: " + smarts);
                group.setRefmolAtomsMissing(refMolAtoms - mcsAtoms);
            }
        }
    }

    protected GroupMember createGroupMember(MoleculeNode node) {
        return new GroupMember(node);
    }

    protected class Grouping {

        private final Map<Long, GroupMember> members = new HashMap<>();

        private List<Group> groups;

        /**
         * Fetch the GroupMember for the node, creating it if it doesn't yet exist.
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

        /**
         * Add the molecule node with these edges.
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

        public int getGroupCount() {
            return getGroups().size();
        }

        protected synchronized Collection<Group> getGroups() {
            if (groups == null) {

                Collection<Group> gs = collectGroups();
                List<Group> list = new ArrayList<>(gs);
                Collections.sort(list, new Comparator<Group>() {

                    @Override
                    public int compare(Group g1, Group g2) {
                        Integer i1 = g1.molTransform.getClassification().getOrder();
                        Integer i2 = g2.molTransform.getClassification().getOrder();
                        return i1.compareTo(i2);
                    }
                });

                groups = list;
            }
            return groups;
        }

        protected void clearGroups() {
            groups = null;
        }

        private Collection<Group> collectGroups() {
            LOG.fine("Collecting groups");
            Map<MolTransform, Group> result = Collections.synchronizedMap(new LinkedHashMap<>());
            members.values().parallelStream().forEach((m) -> {
                MolTransform transform = m.getMolTransform();
                Group group = result.get(transform);
                if (group == null) {
                    group = new Group(transform);
                    result.put(transform, group);
                }
                group.addMember(m);
            });
            LOG.info("Collected " + result.size() + " groups");
            return result.values();
        }
    }

    static GroupingType determineGroupingType(GroupingType current, GroupingType proposed) {
        if (current == null) {
            return proposed;
        } else if (current == proposed) {
            return current;
        } else if (current == GroupingType.ADDITION_DELETION) {
            if (proposed.toString().startsWith("SUBSTITUTE_")) {
                return current;
            }
        }
        return GroupingType.UNDEFINED;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonPropertyOrder({"key", "classification", "prototype", "scaffold", "refmolAtomsMissing", "memberCount", "members"})
    public class Group {

        private final MolTransform molTransform;
        private Integer refmolAtomsMissing;
        private final List<GroupMember> members = new ArrayList<>();
        private boolean sorted = false;
        private int memberCount;

        protected Group(MolTransform molTransform) {
            this.molTransform = molTransform;
        }

        public String getKey() {
            return molTransform.getScaffold();
        }

        public List<GroupMember> getMembers() {
            return members;
        }

        public int getMemberCount() {
            return memberCount;
        }


        public void sortMembersByHacAndTruncate(Integer limit) {
            if (!sorted) {
                // set the memberCount before we perform truncation
                memberCount = members.size();
                Collections.sort(members, new Comparator<GroupMember>() {
                    @Override
                    public int compare(GroupMember m1, GroupMember m2) {
                        String smiles1 = m1.getSmiles();
                        String smiles2 = m2.getSmiles();
                        RWMol mol1 = fetchMolecule(smiles1);
                        RWMol mol2 = fetchMolecule(smiles2);
                        Long hac1 = mol1.getNumHeavyAtoms();
                        Long hac2 = mol2.getNumHeavyAtoms();

                        int result = hac1.compareTo(hac2);
                        if (result == 0) {
                            Double mw1 = RDKFuncs.calcExactMW(mol1);
                            Double mw2 = RDKFuncs.calcExactMW(mol2);
                            return mw1.compareTo(mw2);
                        } else {
                            return result;
                        }
                    }
                });
                if (limit != null && limit > 0) {
                    while (members.size() > limit) {
                        members.remove(limit.intValue());
                    }
                }
                sorted = true;
            }
        }

        /**
         * A prototype structure (SMILES) for the group.
         *
         * @return
         */
        public String getPrototype() {
            return members.get(0).getSmiles();
        }

        public Integer getRefmolAtomsMissing() {
            return refmolAtomsMissing;
        }

        protected void setRefmolAtomsMissing(Integer refmolAtomsMissing) {
            this.refmolAtomsMissing = refmolAtomsMissing;
        }

        protected void addMember(GroupMember member) {
            members.add(member);
        }

        public GroupingType getClassification() {
            return molTransform.getClassification();
        }
    }


    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public class GroupMember {

        private final Long id;
        private final MoleculeNode node;
        private final List<MoleculeEdge[]> edges = new ArrayList<>();
        private MolTransform molTransform;

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

//        public int getNumPaths() {
//            return edges == null ? 0 : edges.size();
//        }

        public String getPathLengths() {
            StringBuffer buf = new StringBuffer();
            for (MoleculeEdge[] e : edges) {
                if (buf.length() > 0) {
                    buf.append(",");
                }
                buf.append(e.length);
            }
            return buf.toString();
        }

        GroupingType getClassification() {
            return getMolTransform().getClassification();
        }

        private Long[] edgeIdsForPath(MoleculeEdge[] es) {
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
                i -= 1;
            }
            return ids;
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

        List<TransformData> getTransformData() {
            List<TransformData> results = new ArrayList<>();
            for (MoleculeEdge[] path : edges) {
                TransformData data = null;
                if (path.length == 1) {
                    boolean isAddition = path[0].getChildId() != getId();
                    data = new TransformData(
                            getRefmol(),
                            path[0].getLabel(),
                            isAddition,
                            getSmiles());
                } else if (path.length == 2) {
                    boolean isDeletion2 = path[1].getChildId() == getId();
                    long midNodeId = isDeletion2 ? path[1].getParentId() : path[1].getChildId();
                    boolean isDeletion1 = path[0].getChildId() == midNodeId;
                    data = new TransformData(
                            getRefmol(),
                            path[0].getLabel(),
                            !isDeletion1,
                            nodes.get(midNodeId).getSmiles(),
                            path[1].getLabel(),
                            !isDeletion2,
                            getSmiles()
                    );
                } else {
                    throw new IllegalStateException("Only hops of 1 or 2 are supported");
                }
                results.add(data);

            }
            return results;
        }


        protected MolTransform getMolTransform() {

            try {
                List<TransformData> paths = getTransformData();
                TransformData best = TransformClassifierUtils.determineSimplestTransform(paths);
                MolTransform tf = TransformClassifierUtils.generateMolTransform(best);
                return tf;
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Failed to classify molecule. Resorting to undefined", ex);
                return new MolTransform("undefined-undefined", GroupingType.UNDEFINED, 0);
            }

        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder("GroupMember [")
                    .append(id)
                    .append(" ")
                    .append(getSmiles());
            int i = 0;
            for (MoleculeEdge[] earr : edges) {
                int j = 0;
                for (MoleculeEdge e : earr) {
                    String label = e.getLabel();
                    //b.append("\n").append(label);
                    String[] parts = FragmentUtils.splitLabel(label);
                    b.append("\n" + i + j + "\t" + parts[0] + " " + parts[3] + "\t" + parts[1] + "\t" + parts[4]);
                    j++;
                }
                b.append("\n");
                Long[] eids = edgeIdsForPath(earr);
                for (Long eid : eids) {
                    b.append(eid);
                }
                i++;
            }

            MolTransform tx = getMolTransform();
            b.append("\nGrouping Key: ").append(tx.getScaffold()).append(" ").append(tx.getClassification()).append("]\n");

            return b.toString();
        }
    }

}
