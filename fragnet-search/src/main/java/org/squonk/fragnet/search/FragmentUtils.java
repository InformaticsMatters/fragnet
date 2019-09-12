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
package org.squonk.fragnet.search;

import org.neo4j.driver.v1.types.MapAccessor;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.types.Path;
import org.neo4j.driver.v1.types.Relationship;
import org.squonk.fragnet.Constants;
import org.squonk.fragnet.search.model.v1.MoleculeEdge;
import org.squonk.fragnet.search.model.v1.MoleculeNode;

import java.util.logging.Logger;

public class FragmentUtils implements Constants {

    public static final Logger LOG = Logger.getLogger("FragmentUtils.class");


    /**
     * Get the start and end node of the relationship
     *
     * @param seg
     * @return
     */
    public static Node[] orderNodesFromRelationship(Path.Segment seg) {
        if (seg.relationship().startNodeId() == seg.start().id()) {
            return new Node[]{seg.start(), seg.end()};
        } else {
            return new Node[]{seg.end(), seg.start()};
        }
    }

    public static String getStringProp(MapAccessor obj, String propname) {
        return obj.get(propname).asString();
    }

    public static int getIntProp(MapAccessor obj, String propname) {
        return obj.get(propname).asInt();
    }

    public static float getFloatProp(MapAccessor obj, String propname) {
        return obj.get(propname).asFloat();
    }

    public static String getSmiles(Node node) {
        return node.get(MoleculeNode.Property.SMILES.dbname).asString();
    }

    public static String getLabel(Relationship rel) {
        return rel.get(MoleculeEdge.Property.LABEL.dbname).asString();
    }


    public static int getHeavyAtomCount(Node node) {
        return node.get(MoleculeNode.Property.HEAVY_ATOM_COUNT.dbname).asInt();
    }

    public static int getRingAtomCount(Node node) {
        return node.get(MoleculeNode.Property.RING_ATOM_COUNT.dbname).asInt();
    }

    public static String[] splitLabel(String label) {
        return label.split("\\|");
    }

}
