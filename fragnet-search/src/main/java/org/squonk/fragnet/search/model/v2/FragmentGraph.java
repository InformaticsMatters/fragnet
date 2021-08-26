/*
 * Copyright (c) 2021 Informatics Matters Ltd.
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
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.RDKit.ExplicitBitVect;
import org.RDKit.RWMol;
import org.RDKit.SparseIntVectu32;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.types.Path;
import org.neo4j.driver.v1.types.Relationship;
import org.squonk.fragnet.chem.Calculator;
import org.squonk.fragnet.search.FragmentUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Represents the a fragment graph, a directed acyclic graph of molecules and their fragments that is the result
 * of a query against the fragment network.
 * The key parts of this are the nodes (molecules and fragments) and the edges (connections between the nodes).
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({"apiVersion","query","parameters","shortMessage", "longMessage", "resultAvailableAfter","processingTime","calculationTime","nodeCount","edgeCount","nodes","edges"})
public class FragmentGraph {

    private static final Logger LOG = Logger.getLogger(FragmentGraph.class.getName());
    private static final String API_VERSION = "v2";

    protected final Map<Long, MoleculeNode> nodes = new LinkedHashMap<>();
    protected final Map<Long, MoleculeEdge> edges = new LinkedHashMap<>();
    private String query;
    private final Map<String, Object> parameters = new LinkedHashMap<>();
    private Long resultAvailableAfter;
    private Long processingTime;
    private Long calculationTime;
    private String shortMessage;
    private String longMessage;

    /** Cache of molecules
     *
     */
    protected Map<String,RWMol> molecules = new HashMap<>();


    public FragmentGraph() {

    }

    public FragmentGraph(
            @JsonProperty("nodes") Collection<MoleculeNode> nodes,
            @JsonProperty("edges") Collection<MoleculeEdge> edges,
            @JsonProperty("query") String query,
            @JsonProperty("parameters") Map<String, Object> parameters,
            @JsonProperty("resultAvailableAfter") Long resultAvailableAfter,
            @JsonProperty("processingTime") Long processingTime,
            @JsonProperty("calculationTime") Long calculationTime
            ) {
        nodes.forEach((n) -> this.nodes.put(n.getId(), n));
        edges.forEach((e) -> this.edges.put(e.getId(), e));
        this.query = query;
        setParameters(parameters);
        this.resultAvailableAfter = resultAvailableAfter;
        this.processingTime = processingTime;
        this.calculationTime = calculationTime;
    }

    public String getApiVersion() {
        return API_VERSION;
    }

    public int getNodeCount() {
        return nodes.size();
    }

    public int getEdgeCount() {
        return edges.size();
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        if (parameters != null) {
            this.parameters.putAll(parameters);
        }
    }

    public Long getResultAvailableAfter() {
        return resultAvailableAfter;
    }

    public Long getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(Long processingTime) {
        this.processingTime = processingTime;
    }

    public Long getCalculationTime() {
        return calculationTime;
    }

    public void setResultAvailableAfter(Long resultAvailableAfter) {
        this.resultAvailableAfter = resultAvailableAfter;
    }

    public Collection<MoleculeNode> getNodes() {
        return nodes.values();
    }

    public Collection<MoleculeEdge> getEdges() {
        return edges.values();
    }

    public String getShortMessage() {
        return shortMessage;
    }

    public void setShortMessage(String shortMessage) {
        this.shortMessage = shortMessage;
    }

    public String getLongMessage() {
        return longMessage;
    }

    public void setLongMessage(String longMessage) {
        this.longMessage = longMessage;
    }

    protected RWMol fetchMolecule(String smiles) {
        RWMol mol = molecules.get(smiles);
        if (mol == null) {
            mol = RWMol.MolFromSmiles(smiles);
            molecules.put(smiles, mol);
        }
        return mol;
    }

    public void calculate(String refmolSmiles, Calculator.Calculation... calcs) {
        long t0 = new Date().getTime();
        RWMol refmol = fetchMolecule(refmolSmiles);
        AtomicReference<ExplicitBitVect> rdkit1 = new AtomicReference<>();
        AtomicReference<SparseIntVectu32> morgan12 = new AtomicReference<>();
        AtomicReference<SparseIntVectu32> morgan13 = new AtomicReference<>();
        AtomicInteger count = new AtomicInteger(0);
        nodes.values().parallelStream().forEach((n) -> {
            count.incrementAndGet();
            String smiles = n.getSmiles();
            RWMol mol = fetchMolecule(smiles);
            for (Calculator.Calculation calc : calcs) {
                switch (calc) {

                    case MW:
                        Float mw = Calculator.calcMolWeight(mol);
                        n.addProp(calc.propname, mw);
                        break;

                    case LOGP:
                        Float logp = Calculator.calcLogP(mol);
                        n.addProp(calc.propname, logp);
                        break;

                    case TPSA:
                        Float tpsa = Calculator.calcTPSA(mol);
                        n.addProp(calc.propname, tpsa);
                        break;

                    case ROTB:
                        Integer rotb = Calculator.calcRotatableBonds(mol);
                        n.addProp(calc.propname, rotb);
                        break;

                    case HBA:
                        Integer hba = Calculator.calcHydrogenBondAcceptors(mol);
                        n.addProp(calc.propname, hba);
                        break;

                    case HBD:
                        Integer hbd = Calculator.calcHydrogenBondDonors(mol);
                        n.addProp(calc.propname, hbd);
                        break;

                    case SIM_RDKIT_TANIMOTO:
                        if (rdkit1.get() == null) {
                            rdkit1.set(Calculator.calcRDKitFingerprint(refmol));
                        }
                        ExplicitBitVect rdkit2 = Calculator.calcRDKitFingerprint(mol);
                        Float rsim = Calculator.calcTanimotoSimilarity(rdkit1.get(), rdkit2);
                        n.addProp(calc.propname, rsim);
                        break;

                    case SIM_MORGAN2_TANIMOTO:
                        if (morgan12.get() == null) {
                            morgan12.set(Calculator.calcMorganFingerprint(refmol, 2));
                        }
                        SparseIntVectu32 morgan22 = Calculator.calcMorganFingerprint(mol, 2);
                        Float sim2 = Calculator.calcTanimotoSimilarity(morgan12.get(), morgan22);
                        n.addProp(calc.propname, sim2);
                        break;

                    case SIM_MORGAN3_TANIMOTO:
                        if (morgan13.get() == null) {
                            morgan13.set(Calculator.calcMorganFingerprint(refmol, 3));
                        }
                        SparseIntVectu32 morgan23 = Calculator.calcMorganFingerprint(mol, 3);
                        Float sim3 = Calculator.calcTanimotoSimilarity(morgan13.get(), morgan23);
                        n.addProp(calc.propname, sim3);
                        break;
                }
            }
        });
        long t1 = new Date().getTime();
        long duration = t1 - t0;
        if (calculationTime == null) {
            calculationTime = duration;
        } else {
            calculationTime += duration;
        }
        LOG.info(String.format("Calculated %s properties for %s molecules", calcs.length, count));
    }


    /**
     * Add the elements of this path to the graph.
     *
     * @param path
     */
    public void add(Path path) {
        path.forEach((seg) -> {
            Node start = seg.start();
            Node end = seg.end();
            Relationship rel = seg.relationship();
            Node parent = null;
            Node child = null;
            if (start.id() == rel.startNodeId()) {
                parent = start;
            } else if (start.id() == rel.endNodeId()) {
                parent = end;
            }
            if (end.id() == rel.endNodeId()) {
                child = end;
            } else if (end.id() == rel.startNodeId()) {
                child = start;
            }
            if (parent == null || child == null) {
                LOG.warning("Couldn't find parent or child");
            } else {
                List<String> parentLabels = new ArrayList<>();
                parent.labels().forEach((l) -> parentLabels.add(l));
                List<String> childLabels = new ArrayList<>();
                child.labels().forEach((l) -> childLabels.add(l));
                MoleculeNode parentNode = generateMoleculeNode(parent);
                MoleculeNode childNode = generateMoleculeNode(child);
                MoleculeEdge edge = new MoleculeEdge(rel.id(), parentNode.getId(), childNode.getId(), FragmentUtils.getLabel(rel));
                add(parentNode, childNode, edge);
            }

        });
    }

    public static MoleculeNode generateMoleculeNode(Node node) {
        List<String> labels = new ArrayList<>();
        node.labels().forEach((l) -> labels.add(l));
        MoleculeNode.MoleculeType type = null;
        if (node.hasLabel("MOL")) {
            type = MoleculeNode.MoleculeType.NET_MOL;
        } else if (node.hasLabel("F2")) {
            type = MoleculeNode.MoleculeType.NET_FRAG;
        }

        Map<String, Object> props = new LinkedHashMap<>(node.asMap());
        props.remove("smiles");

        return new MoleculeNode(node.id(), FragmentUtils.getSmiles(node), type, labels, props);
    }

    public void add(MoleculeNode parent, MoleculeNode child, MoleculeEdge edge) {

        if (edge.getParentId() != parent.getId()) {
            throw new IllegalArgumentException("Incompatible edge. Parent in edge defined as " + edge.getParentId()
                    + " but parent node was " + parent.getId());
        }
        if (edge.getChildId() != child.getId()) {
            throw new IllegalArgumentException("Incompatible edge. Child in edge defined as " + edge.getChildId()
                    + " but child node was " + child.getId());
        }

        if (!nodes.containsKey(parent.getId())) {
            nodes.put(parent.getId(), parent);
            LOG.fine("Node " + parent.getSmiles() + " added");
        } else {
            LOG.fine("Node " + parent.getSmiles() + " already present");
        }
        if (!nodes.containsKey(child.getId())) {
            nodes.put(child.getId(), child);
            LOG.fine("Node " + child.getSmiles() + " added");
        } else {
            LOG.fine("Node " + child.getSmiles() + " already present");
        }
        Long id = edge.getId();
        if (!edges.containsKey(id)) {
            edges.put(id, edge);
            LOG.fine("Edge " + id + " added");
        } else {
            LOG.fine("Edge " + id + " already present");
        }
    }


    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("FragmentGraph [");
        b.append(nodes.size() + " nodes, ").append(edges.size()).append(" edges]");
        return b.toString();
    }

    public String toJson() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }
}
