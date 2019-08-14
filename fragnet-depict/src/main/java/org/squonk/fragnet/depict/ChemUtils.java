package org.squonk.fragnet.depict;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.layout.StructureDiagramGenerator;


import org.openscience.cdk.silent.AtomContainer;
import org.openscience.smsd.AtomAtomMapping;
import org.openscience.smsd.Isomorphism;
import org.openscience.smsd.interfaces.Algorithm;
import org.openscience.smsd.tools.ExtAtomContainerManipulator;


import javax.vecmath.Vector2d;
import java.util.Map;
import java.util.logging.Logger;

public class ChemUtils {

    private static final Logger LOG = Logger.getLogger(ChemUtils.class.getName());

    public static IAtomContainer generate2D(IAtomContainer mol) throws CDKException {
        StructureDiagramGenerator sdg = new StructureDiagramGenerator();
        sdg.setMolecule(mol);
        sdg.generateCoordinates(new Vector2d(0, 1));
        return sdg.getMolecule();
    }

    public static AtomAtomMapping determineMCS(IAtomContainer query, IAtomContainer target) throws CDKException {
        ExtAtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(query);
        ExtAtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(target);

        query = ExtAtomContainerManipulator.removeHydrogens(query);
        target = ExtAtomContainerManipulator.removeHydrogens(target);

        ExtAtomContainerManipulator.aromatizeMolecule(query);
        ExtAtomContainerManipulator.aromatizeMolecule(target);

        query = new AtomContainer(query);
        target = new AtomContainer(target);

        boolean bondSensitive = true;
        boolean ringMatch = false;
        boolean stereoMatch = true;
        boolean fragmentMinimization = true;
        boolean energyMinimization = true;

        Isomorphism comparison = new Isomorphism(query, target, Algorithm.DEFAULT, bondSensitive, ringMatch, false);
        //comparison.setChemFilters(stereoMatch, fragmentMinimization, energyMinimization);
        AtomAtomMapping firstAtomMapping = comparison.getFirstAtomMapping();
        for (Map.Entry<IAtom, IAtom> mapping : firstAtomMapping.getMappingsByAtoms().entrySet()) {
            LOG.info("Mapping: " + mapping);
        }

        return firstAtomMapping;
    }
}
