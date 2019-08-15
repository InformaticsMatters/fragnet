package org.squonk.fragnet.depict;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.smsd.AtomAtomMapping;
import org.openscience.smsd.Isomorphism;
import org.openscience.smsd.interfaces.Algorithm;
import org.openscience.smsd.tools.ExtAtomContainerManipulator;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
    public static void prepareForMCS(IAtomContainer mol) throws CDKException {
        ExtAtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        ExtAtomContainerManipulator.aromatizeMolecule(mol);
    }

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
    public static AtomAtomMapping determineMCS(IAtomContainer query, IAtomContainer target) throws CDKException {

        boolean bondSensitive = true;
        boolean ringMatch = false;
        boolean stereoMatch = true;
        boolean fragmentMinimization = true;
        boolean energyMinimization = true;

        Isomorphism comparison = new Isomorphism(query, target, Algorithm.DEFAULT, bondSensitive, ringMatch, false);
        //comparison.setChemFilters(stereoMatch, fragmentMinimization, energyMinimization);
        AtomAtomMapping firstAtomMapping = comparison.getFirstAtomMapping();

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
     * @param mol     The molecule to align. The coordinates of this molecule will be updated.
     * @param mapping The mapping of atoms in the query to those in the target.
     * @throws CDKException
     * @throws CloneNotSupportedException
     */
    public static void alignMolecule(IAtomContainer mol, AtomAtomMapping mapping) throws CDKException, CloneNotSupportedException {

        if (mapping == null || mapping.getCount() == 0) {
            LOG.fine("No MCS found");
            return;
        } else {
            String mcsSmiles = mapping.getCommonFragmentAsSMILES();
            LOG.fine("MCS: " + mcsSmiles);
            Set<IAtom> fixedAtoms = new HashSet<>();
            for (Map.Entry<IAtom, IAtom> e : mapping.getMappingsByAtoms().entrySet()) {
                IAtom qAtom = e.getKey();
                IAtom tAtom = e.getValue();
                int qIndex = mapping.getQuery().indexOf(qAtom);
                int tIndex = mapping.getTarget().indexOf(tAtom);
                if (qIndex < 0) {
                    throw new IllegalStateException("Atom not found in query molecule. Strange!");
                } else if (tIndex < 0) {
                    throw new IllegalStateException("Atom not found in target molecule. Strange!");
                } else {
                    IAtom atom = mol.getAtom(tIndex);
                    fixedAtoms.add(atom);
                    Point2d point = qAtom.getPoint2d();
                    LOG.finer("Setting atom " + qIndex + "->" + tIndex + " " + qAtom.getSymbol() + "->" + tAtom.getSymbol() + " to " + point);
                    atom.setPoint2d(point);
                }
            }
            StructureDiagramGenerator g = new StructureDiagramGenerator();
            g.setMolecule(mol, false, fixedAtoms, null);
            g.generateCoordinates(mol);
        }
    }
}
