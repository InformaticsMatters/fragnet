package org.squonk.fragnet.depict

import org.squonk.cdk.depict.ChemUtils
import org.squonk.fragnet.depict.CDKMolDepictServlet
import spock.lang.Specification

class CDKMolDepictServletTest extends Specification {

    void "smiles2svg"() {

        when:
        CDKMolDepictServlet servlet = new CDKMolDepictServlet()
        def mol = ChemUtils.readSmiles("[H]C1NCC(C)C(N)=C1")
        def svg = servlet.test(mol)

        then:
        svg != null
        svg.length() > 0
        svg.contains("width=")
        svg.contains("height=")
        svg.contains("id='mol1bnd1")
    }

    void "molfile2svg"() {

        when:
        CDKMolDepictServlet servlet = new CDKMolDepictServlet()
        def molfile = new File('../test-data/ligand.mol').getText('UTF-8')
        print(molfile)
        def mol = ChemUtils.readMolfile(molfile)
        def svg = servlet.test(mol)

        then:
        svg != null
        svg.length() > 0
        svg.contains("width=")
        svg.contains("height=")
        svg.contains("id='mol1bnd1")
    }

}
