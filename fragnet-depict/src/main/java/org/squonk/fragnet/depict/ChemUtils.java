package org.squonk.fragnet.depict;

import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.geometry.GeometryUtil;
import org.openscience.cdk.graph.CycleFinder;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.MDLV2000Writer;
import org.openscience.cdk.isomorphism.UniversalIsomorphismTester;
import org.openscience.cdk.isomorphism.mcss.RMap;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.logging.Logger;

public class ChemUtils {

    private static final Logger LOG = Logger.getLogger(ChemUtils.class.getName());
    private static final SmilesParser smilesParser = new SmilesParser(SilentChemObjectBuilder.getInstance());

    /**
     * Convenience method to generate IAtomContainer from SMILES string
     *
     * @param smiles
     * @return
     * @throws IOException
     */
    public static IAtomContainer readSmiles(String smiles) throws IOException, InvalidSmilesException {
        return smilesParser.parseSmiles(smiles);
    }

    /**
     * Generate 2D coordinates for the given molecucle
     *
     * @param mol
     * @return The layed out molecule
     * @throws CDKException
     */
    public static IAtomContainer generate2D(IAtomContainer mol) throws CDKException {
        StructureDiagramGenerator sdg = new StructureDiagramGenerator();
        sdg.setMolecule(mol);
        sdg.generateCoordinates(new Vector2d(0, 1));
        return sdg.getMolecule();
    }

    /**
     * Prepares the molecule for MCS determination.
     * The input molecule is modified. Clone it first if you don't want this to happen.
     *
     * @param mol
     * @return
     * @throws CDKException
     */
    public static boolean prepareForMCS(IAtomContainer mol) throws CDKException {
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        ElectronDonation model       = ElectronDonation.daylight();
        CycleFinder cycles      = Cycles.or(Cycles.all(), Cycles.all(6));
        Aromaticity aromaticity = new Aromaticity(model, cycles);
        boolean isAtomatic = aromaticity.apply(mol);
        return isAtomatic;
    }


//    This uses the CDK legacy SMSD code that is deprecated
//    public static Map<IAtom, IAtom> determineMCSxxx(IAtomContainer query, IAtomContainer target) throws CDKException {
//
//        boolean bondSensitive = true;
//        boolean ringMatch = false;
//        boolean stereoMatch = true;
//        boolean fragmentMinimization = true;
//        boolean energyMinimization = true;
//
//        Isomorphism comparison = new Isomorphism(Algorithm.DEFAULT, true);
//        comparison.init(query, target, false, false);
//        //comparison.setChemFilters(stereoMatch, fragmentMinimization, energyMinimization);
//        Map<IAtom, IAtom> firstAtomMapping = comparison.getFirstAtomMapping();
//
//        return firstAtomMapping;
//    }

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

        UniversalIsomorphismTester tester = new UniversalIsomorphismTester();
        List<RMap> matches = tester.getIsomorphAtomsMap(query, target);

        Map<IAtom, IAtom> result = new HashMap<>();
        if (matches != null && !matches.isEmpty()) {
            RMap m = matches.get(0);
            result.put(query.getAtom(m.getId1()), target.getAtom(m.getId2()));
        }
        return result;
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
     */
    public static void layoutMoleculeIn2D(IAtomContainer mol, boolean always) throws CDKException {
        if (always || !GeometryUtil.has2DCoordinates(mol)) {
            LOG.fine("Laying out molecule");
            StructureDiagramGenerator g = new StructureDiagramGenerator();
            g.generateCoordinates(mol);
        }
    }

    public static String convertToMolfile(IAtomContainer mol) throws IOException, CDKException {
        if (!GeometryUtil.has2DCoordinates(mol)) {
            mol = generate2D(mol);
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
}
