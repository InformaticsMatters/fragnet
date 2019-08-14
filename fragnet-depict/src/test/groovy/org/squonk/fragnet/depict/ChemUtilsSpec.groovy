package org.squonk.fragnet.depict

import org.openscience.cdk.interfaces.IAtomContainer
import org.openscience.cdk.silent.SilentChemObjectBuilder
import org.openscience.cdk.smiles.SmilesParser
import spock.lang.Specification

class ChemUtilsSpec extends Specification {

    private static final SmilesParser smilesParser = new SmilesParser(SilentChemObjectBuilder.getInstance());

    void "generate2D"() {

        IAtomContainer mol = smilesParser.parseSmiles('NC1=CC=CC=C1')

        when:
        def result = ChemUtils.generate2D(mol)

        then:
        result.getAtom(0).getPoint2d() != null
    }

    void "determineMCS"() {

        IAtomContainer query = ChemUtils.generate2D(smilesParser.parseSmiles('NC1=CC=CC=C1'))
        IAtomContainer target = ChemUtils.generate2D(smilesParser.parseSmiles('NC1=CC(=CC=C1)C(O)=O'))

        when:
        def result = ChemUtils.determineMCS(query, target)
        println result.getCount()

        then:
        result != null


    }
}
