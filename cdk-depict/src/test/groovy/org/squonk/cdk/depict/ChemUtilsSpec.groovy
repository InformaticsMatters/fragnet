package org.squonk.cdk.depict

import org.openscience.cdk.geometry.GeometryUtil
import org.openscience.cdk.interfaces.IAtom
import org.openscience.cdk.interfaces.IAtomContainer
import org.openscience.cdk.silent.SilentChemObjectBuilder
import org.openscience.cdk.smiles.SmilesParser
import spock.lang.Specification

class ChemUtilsSpec extends Specification {

    private static final SmilesParser smilesParser = new SmilesParser(SilentChemObjectBuilder.getInstance());

    void "generate2D"() {

        IAtomContainer mol = smilesParser.parseSmiles('NC1=CC=CC=C1')

        when:
        ChemUtils.layoutMoleculeIn2D(mol)

        then:
        mol.getAtom(0).getPoint2d() != null
    }

    void "determineMCS"() {

        IAtomContainer query = ChemUtils.layoutMoleculeIn2D(smilesParser.parseSmiles('NC1=CC=CC=C1'))
        IAtomContainer target = ChemUtils.layoutMoleculeIn2D(smilesParser.parseSmiles('NC1=CC(=CC=C1)C(O)=O'))

        when:
        ChemUtils.prepareForMCS(query)
        ChemUtils.prepareForMCS(target)
        def result = ChemUtils.determineMCS(query, target)

        then:
        result.size() == 7
    }

    void "alignMolecule"() {

        IAtomContainer query = ChemUtils.layoutMoleculeIn2D(smilesParser.parseSmiles('NC1=CC=CC=C1'))
        IAtomContainer target = smilesParser.parseSmiles('NC1=CC(=CC=C1)C(O)=O')

        ChemUtils.prepareForMCS(query)
        ChemUtils.prepareForMCS(target)

        Map<IAtom,IAtom> mapping = ChemUtils.determineMCS(query, target)

        when:
        ChemUtils.alignMolecule(query, target, mapping)

        then:
        GeometryUtil.has2DCoordinates(target)
    }

//    void "alignMolecule fail"() {
//
//        IAtomContainer query = ChemUtils.generate2D(smilesParser.parseSmiles('BrC1CCC(Cc2ccccc2)C1'))
//        IAtomContainer target = smilesParser.parseSmiles('ClC1CCC(Cc2ccccc2)C1')
//
//        ChemUtils.prepareForMCS(query)
//        ChemUtils.prepareForMCS(target)
//
//        AtomAtomMapping mapping = ChemUtils.determineMCS(query, target)
//
//        when:
//        ChemUtils.alignMolecule(target, mapping)
//
//        then:
//        GeometryUtil.has2DCoordinates(target)
//    }
}
