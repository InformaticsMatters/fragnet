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
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.types.Path;
import org.neo4j.driver.v1.types.Relationship;
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
@JsonPropertyOrder({"apiVersion", "query", "parameters", "refmol", "resultAvailableAfter", "processingTime", "calculationTime", "nodeCount", "edgeCount", "groupCount", "nodes", "edges", "groups"})
public class NeighbourhoodGraph extends FragmentGraph implements Constants {

    private static final Logger LOG = Logger.getLogger(NeighbourhoodGraph.class.getName());
    private static final String SEP = "::";

    private static final String SUBSTITUTION_FG = "FG";
    private static final String SUBSTITUTION_RING = "RING";

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
        return grouping.getGroups();
    }

    public int getGroupCount() {
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
     * Generate information about the groups.
     * What is generated is an active area of development. Currently it comprises:
     * - a prototype structure (SMILES) for the group
     * - the number of atoms in the refmol that are not present in the MCS of the refmol and group members
     * <p>
     * The aim is to generate a prototype structure that better represents the group, probably by creating an
     * R-group representation, but currently the member with the smallest number of atoms is used.
     */
    public void generateGroupMCS() {
        for (Group group : getGroups()) {

            // TODO generate a more meaningful prototype structure such as an R-group representation of the group
            long smallestAtoms = Long.MAX_VALUE;
            String smallestSmiles = null;


            // generate MCS so that we can determine how many atoms have been lost etc.v
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
                    long atoms = mol.getNumAtoms();
                    if (atoms < smallestAtoms) {
                        smallestAtoms = atoms;
                        smallestSmiles = smiles;
                    }
                    if (mol != null) {
                        mols.add(mol);
                    }
                }

                LOG.info("Setting prototype to " + smallestSmiles);
                group.setPrototype(smallestSmiles);

                if (mols.size() > 1) {
                    long t0 = System.nanoTime();
                    MCSResult mcs = RDKFuncs.findMCS(mols);
                    long t1 = System.nanoTime();
                    int mcsAtoms = (int) mcs.getNumAtoms();
                    String smarts = mcs.getSmartsString();
                    LOG.info("Refmol/MCS Atoms: " + refMolAtoms + "/" + mcsAtoms + " Took: " + (t1 - t0) +
                            "ns Smarts: " + smarts);
                    group.setRefmolAtomsMissing(refMolAtoms - mcsAtoms);
                }
            }
        }
    }

    protected class Grouping {

        private final Map<Long, GroupMember> members = new LinkedHashMap<>();

        private Collection<Group> groups;

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

        protected Collection<Group> getGroups() {
            if (groups == null) {
                groups = collectGroups();
            }
            return groups;
        }

        protected void clearGroups() {
            groups = null;
        }

        private Collection<Group> collectGroups() {
            Map<MolTransform, Group> result = new LinkedHashMap<>();
            members.values().forEach((m) -> {
                MolTransform transform = m.getMolTransform();
                Group group = result.get(transform);
                if (group == null) {
                    group = new Group(transform);
                    result.put(transform, group);
                }
                group.addMember(m);
            });
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
    @JsonPropertyOrder({"key", "classification", "prototype", "scaffold", "refmolAtomsMissing", "members"})
    public class Group {

        private final MolTransform molTransform;
        private String prototype;
        private Integer refmolAtomsMissing;
        private final List<GroupMember> members = new ArrayList<>();

        protected Group(MolTransform molTransform) {
            this.molTransform = molTransform;
        }

        public String getKey() {
            return molTransform.getScaffold();
        }

        public List<GroupMember> getMembers() {
            return members;
        }

        /**
         * A prototype structure (SMILES) for the group.
         *
         * @return
         */
        public String getPrototype() {
            return prototype;
        }

        protected void setPrototype(String prototype) {
            this.prototype = prototype;
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

        protected MolTransform getMolTransform() {
            if (molTransform == null) {
                List<MolTransform> transforms = new ArrayList<>();
                int numErrors = 0;
                boolean[] directions = null;

                for (MoleculeEdge[] edges : edges) {
                    MolTransform tf = null;
                    if (edges.length == 1) {
                        boolean isAddition = edges[0].getChildId() != getId();
                        directions = new boolean[]{isAddition};
                        try {
                            tf = TransformClassifierUtils.generateMolTransform(
                                    getRefmol(),
                                    edges[0].getLabel(),
                                    isAddition,
                                    getSmiles());

                        } catch (Exception ex) {
                            numErrors++;
                            LOG.log(Level.WARNING, "Failed to generate transform for " +
                                            getRefmol() + SPACE +
                                            edges[0].getLabel() + SPACE +
                                            isAddition + SPACE +
                                            getSmiles()
                                    , ex);
                        }

                    } else if (edges.length == 2) {
                        boolean isDeletion2 = edges[1].getChildId() == getId();
                        long midNodeId = isDeletion2 ? edges[1].getParentId() : edges[1].getChildId();
                        boolean isDeletion1 = edges[0].getChildId() == midNodeId;
                        directions = new boolean[]{!isDeletion1, !isDeletion2};

                        try {

                            tf = TransformClassifierUtils.generateMolTransform(
                                    getRefmol(),
                                    edges[0].getLabel(),
                                    !isDeletion1,
                                    nodes.get(midNodeId).getSmiles(),
                                    edges[1].getLabel(),
                                    !isDeletion2,
                                    getSmiles());
                        } catch (Exception ex) {
                            numErrors++;
                            LOG.log(Level.WARNING, "Failed to generate transform for " +
                                            getRefmol() + SPACE +
                                            edges[0].getLabel() + SPACE +
                                            !isDeletion1 + SPACE +
                                            nodes.get(midNodeId).getSmiles() + SPACE +
                                            edges[1].getLabel() + SPACE +
                                            !isDeletion2 + SPACE +
                                            getSmiles()
                                    , ex);
                        }
                    } else {
                        throw new IllegalStateException("Hops greater than 2 are not supported");
                    }
                    if (tf != null) {
                        transforms.add(tf);
                    }
                }
                if (numErrors > 0) {
                    LOG.warning("There were errors in classifying the transform. See earlier entries for details");
                }
                if (transforms.size() == 1) {
                    molTransform = transforms.get(0);
                } else {
                    molTransform = TransformClassifierUtils.triageMolTransforms(transforms, directions);
                }
                LOG.info("Transform for " + getSmiles() + " is " + molTransform);
            }
            return molTransform;
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
