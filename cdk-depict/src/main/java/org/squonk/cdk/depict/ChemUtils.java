/*
 * Copyright (c) 2023 Informatics Matters Ltd.
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

package org.squonk.cdk.depict;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.geometry.GeometryUtil;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.IChemObjectReader;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.io.MDLV2000Writer;
import org.openscience.cdk.io.MDLV3000Reader;
import org.openscience.cdk.io.iterator.IteratingSDFReader;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.silent.AtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.smsd.Isomorphism;
import org.openscience.cdk.smsd.interfaces.Algorithm;
import org.openscience.cdk.smsd.tools.ExtAtomContainerManipulator;

import javax.vecmath.Point2d;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class ChemUtils {

    private static final Logger LOG = Logger.getLogger(ChemUtils.class.getName());
    private static final SmilesParser smilesParser = new SmilesParser(SilentChemObjectBuilder.getInstance());


    public static IAtomContainer readMol(String molstr, String format) throws Exception {
        if (format == null || format.equals("smiles")) {
            return readSmiles(molstr);
        } else if (format.equals("mol") || format.equals("molfile")) {
            return readMolfile(molstr);
        }
        throw new IllegalArgumentException("Format not supported: " + format);
    }
    /**
     * Convenience method to generate IAtomContainer from SMILES string
     *
     * @param smiles
     * @return
     * @throws IOException
     */
    public static IAtomContainer readSmiles(String smiles) throws InvalidSmilesException {
        return smilesParser.parseSmiles(smiles);
    }

    public static IAtomContainer readMolfile(String molstr) {
        try {
            MDLV2000Reader parser = new MDLV2000Reader(new ByteArrayInputStream(molstr.getBytes()),
                    IChemObjectReader.Mode.RELAXED);
            IAtomContainer mol = parser.read(SilentChemObjectBuilder.getInstance().newAtomContainer());
            return mol;
        } catch (CDKException e) {
            // not V2000 format
        }
        try {
            MDLV3000Reader parser = new MDLV3000Reader(new ByteArrayInputStream(molstr.getBytes()),
                    IChemObjectReader.Mode.RELAXED);
            IAtomContainer mol = parser.read(SilentChemObjectBuilder.getInstance().newAtomContainer());
            return mol;
        } catch (CDKException e) {
            // not V3000 format
        }
        throw new RuntimeException("Not a V2000 or V3000 format molfile");
    }

    /**
     * Prepares the molecule for MCS determination.
     * The input molecule is modified. Clone it first if you don't want this to happen.
     *
     * @param mol
     * @throws CDKException
     */
    public static void prepareForMCS(IAtomContainer mol) throws CDKException {
        ExtAtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        ExtAtomContainerManipulator.aromatizeMolecule(mol);
    }

//    This is the external SMSD code that has problems
//    /**
//     * Determine the maximum common substructure (MCS) of the querey and target molecules.
//     * Note: the query and target molecules should already have been prepared using the {@link #prepareForMCS(IAtomContainer)}
//     * method.
//     *
//     * @param query
//     * @param target
//     * @return A mapping of the common atoms in the query to the target structures.
//     * @throws CDKException
//     */
//    public static AtomAtomMapping determineMCS(IAtomContainer query, IAtomContainer target) throws CDKException {
//
//        boolean bondSensitive = true;
//        boolean ringMatch = false;
//        boolean stereoMatch = true;
//        boolean fragmentMinimization = true;
//        boolean energyMinimization = true;
//
//        Isomorphism comparison = new Isomorphism(query, target, Algorithm.DEFAULT, bondSensitive, ringMatch, false);
//        //comparison.setChemFilters(stereoMatch, fragmentMinimization, energyMinimization);
//        AtomAtomMapping firstAtomMapping = comparison.getFirstAtomMapping();
//
//        return firstAtomMapping;
//    }

//    This uses the CDK legacy SMSD code that is deprecated but avoids buts present in the external SMSD code (see above).
    /**
     * Determine the maximum common substructure (MCS) of the querey and target molecules.
     * Note: the query and target molecules should already have been prepared using the {@link #prepareForMCS(IAtomContainer)}
     * method.
     *
     * @param query
     * @param target
     * @return A mapping of the common atoms in the query to the target structures.
     * @throws CDKException
     */
    public static Map<IAtom, IAtom> determineMCS(IAtomContainer query, IAtomContainer target) throws CDKException {

        boolean bondSensitive = true;
        boolean ringMatch = false;
        boolean stereoMatch = true;
        boolean fragmentMinimization = true;
        boolean energyMinimization = true;

        Isomorphism comparison = new Isomorphism(Algorithm.DEFAULT, true);
        comparison.init(query, target, false, false);
        //comparison.setChemFilters(stereoMatch, fragmentMinimization, energyMinimization);
        Map<IAtom, IAtom> firstAtomMapping = comparison.getFirstAtomMapping();

        return firstAtomMapping;
    }


    /**
     * Align the molecule given the specified atom-atom mapping.
     * The mapped atoms in the target are given the 2D coordinates from their counterparts in the query and
     * then the structure is layed out but with the mapped atoms fixed so that only the other atoms are layed out.
     * The atom numbers of the molecule must be exactly the same as those in the target molecule from the mapping.
     * e.g. don't remove hydrogens from one and not the other.
     * If there is no mapping then the molecule is unchanged.
     *
     * @param query     The molecule to align to..
     * @param target    The molecule to align to the query. The coordinates of this molecule will be updated.
     * @param mapping The mapping of atoms in the query to those in the target.
     * @throws CDKException
     * @throws CloneNotSupportedException
     */
    public static void alignMolecule(IAtomContainer query, IAtomContainer target, Map<IAtom, IAtom> mapping) throws CDKException, CloneNotSupportedException {

        if (mapping == null || mapping.size() == 0) {
            LOG.fine("No MCS found");
            return;
        } else {
            Set<IAtom> fixedAtoms = new HashSet<>();
            for (Map.Entry<IAtom, IAtom> e : mapping.entrySet()) {
                IAtom qAtom = e.getKey();
                IAtom tAtom = e.getValue();
                int qIndex = query.indexOf(qAtom);
                int tIndex = target.indexOf(tAtom);
                if (qIndex < 0) {
                    throw new IllegalStateException("Atom not found in query molecule. Strange!");
                } else if (tIndex < 0) {
                    throw new IllegalStateException("Atom not found in target molecule. Strange!");
                } else {
                    IAtom atom = target.getAtom(tIndex);
                    fixedAtoms.add(atom);
                    Point2d point = qAtom.getPoint2d();
                    LOG.finer("Setting atom " + qIndex + "->" + tIndex + " " + qAtom.getSymbol() + "->" + tAtom.getSymbol() + " to " + point);
                    atom.setPoint2d(point);
                }
            }
            StructureDiagramGenerator g = new StructureDiagramGenerator();
            g.setMolecule(target, false, fixedAtoms, null);
            g.generateCoordinates();
        }
    }

    /** Generate 2D coordinates for the molecule
     *
     * @param mol The molecule
     * @param always Generate even if the molecule already has 2D coordinates
     * @throws CDKException
     * returns The molecule
     */
    public static IAtomContainer layoutMoleculeIn2D(IAtomContainer mol, boolean always) throws CDKException {

        StructureDiagramGenerator g = new StructureDiagramGenerator();
        g.generateCoordinates(mol);

        return mol;
    }

    public static IAtomContainer layoutMoleculeIn2D(IAtomContainer mol) throws CDKException {
        return layoutMoleculeIn2D(mol, false);
    }

    public static String convertToMolfile(IAtomContainer mol) throws IOException, CDKException {
        if (!GeometryUtil.has2DCoordinates(mol)) {
            mol = layoutMoleculeIn2D(mol);
        }
        StringWriter writer = new StringWriter();
        try (MDLV2000Writer mdl = new MDLV2000Writer(writer)) {
            mdl.write(mol);
        }
        String source = writer.toString();
        return source;
    }

    public static String convertSmilesToMolfile(String smiles) throws IOException, CDKException {
        IAtomContainer mol = readSmiles(smiles);
        return convertToMolfile(mol);
    }

    public static IteratingSDFReader readSDF(String filename) throws IOException {
        IteratingSDFReader reader = new IteratingSDFReader(
                new FileInputStream(new File(filename)), SilentChemObjectBuilder.getInstance()
        );
        return reader;
    }

    public static void createDirs(String filepath) throws IOException {
        Path path = Paths.get(filepath);
        Path dir = path.getParent();
        if (dir != null) {
            LOG.info("Creating dir " + dir.getFileName());
            Files.createDirectories(dir);
        }
    }
}
