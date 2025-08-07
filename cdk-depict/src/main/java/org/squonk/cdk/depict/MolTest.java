package org.squonk.cdk.depict;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.io.IChemObjectReader;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.silent.AtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import java.io.FileInputStream;

public class MolTest {

    public static void main(String[] args) throws Exception {

        MDLV2000Reader parser = new MDLV2000Reader(
                new FileInputStream("../data/example.mol"),
                IChemObjectReader.Mode.RELAXED);
        IAtomContainer mol = parser.read(SilentChemObjectBuilder.getInstance().newAtomContainer());
        IAtom atom = mol.getAtom(0);
        Iterable<IBond> bonds = atom.bonds();

    }
}
