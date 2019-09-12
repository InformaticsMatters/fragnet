package org.squonk.fragnet.search.model.v2;

import org.squonk.fragnet.Constants;
import org.squonk.fragnet.search.FragmentUtils;

public class TransformData implements Constants {

    private final String fromSmiles;
    private final String toSmiles;
    private final String midSmiles;
    private final int numHops;
    private final String[] edges;
    private final boolean[] isAdditions;


    private String[][] parts;
    private final int numMidComponents;

    /**
     * One hop constructor
     *
     * @param fromSmiles
     * @param toSmiles
     * @param edges
     * @param isAdditions
     */
    public TransformData(String fromSmiles, String[] edges, boolean[] isAdditions, String toSmiles) {
        this.fromSmiles = fromSmiles;
        this.toSmiles = toSmiles;
        this.edges = edges;
        this.isAdditions = isAdditions;
        this.midSmiles = null;
        this.numMidComponents = 0;
        this.numHops = 1;
        this.parts = new String[1][];
        parts[0] = FragmentUtils.splitLabel(edges[0]);
    }

    public TransformData(String fromSmiles, String edge, boolean isAddition, String toSmiles) {
        this(fromSmiles, new String[]{edge}, new boolean[]{isAddition}, toSmiles);
    }

    /**
     * Two hop constructor
     *
     * @param fromSmiles
     * @param toSmiles
     */
    public TransformData(String fromSmiles, String midSmiles, String[] edges, boolean[] isAdditions, String toSmiles) {
        assert edges.length == isAdditions.length;
        this.numHops = edges.length;
        this.fromSmiles = fromSmiles;
        this.midSmiles = midSmiles;
        this.toSmiles = toSmiles;
        this.edges = edges;
        this.isAdditions = isAdditions;
        this.parts = new String[numHops][];
        for (int i = 0; i < numHops; i++) {
            parts[i] = FragmentUtils.splitLabel(edges[i]);
        }
        this.numMidComponents = midSmiles.length() - midSmiles.replace(".", "").length() + 1;
    }

    public TransformData(String fromSmiles,
                         String edge1, boolean isAddition1,
                         String midSmiles,
                         String edge2, boolean isAddition2,
                         String toSmiles) {
        this(fromSmiles, midSmiles, new String[]{edge1, edge2}, new boolean[]{isAddition1, isAddition2}, toSmiles);
    }

    public String getToSmiles() {
        return toSmiles;
    }

    public String getFromSmiles() {
        return fromSmiles;
    }

    public String getMidSmiles() {
        return midSmiles;
    }

    public int getNumHops() {
        return numHops;
    }

    public String[] getEdges() {
        return edges;
    }

    public String[][] getParts() {
        return parts;
    }

    public boolean[] getIsAdditions() {
        return isAdditions;
    }

    public int getNumMidComponents() {
        return numMidComponents;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("TransformData: [");
        b.append(fromSmiles).append(SPACE);
        if (numHops == 1) {
            b.append(edges[0])
                    .append(isAdditions[0] ? " + " : " - ");
        } else if (numHops == 2) {
            b.append(edges[0])
                    .append(isAdditions[0] ? " + " : " - ")
                    .append(midSmiles).append(SPACE)
                    .append(edges[1])
                    .append(isAdditions[1] ? " + " : " - ");
        } else {
            b.append("UNEXPECTED NUMBER OF HOPS: ").append(numHops);
        }
        b.append(toSmiles).append("]");
        return b.toString();
    }
}
