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

package org.squonk.cdk.depict

import org.openscience.cdk.CDKConstants
import org.openscience.cdk.interfaces.IAtomContainer
import spock.lang.Specification

import java.awt.*
import java.nio.file.Files

class CDKMolDepictSpec extends Specification {

    void "smiles2svg"() {

        CDKMolDepict depict = new CDKMolDepict()
        def mol = ChemUtils.readSmiles("[H]C1NCC(C)C(N)=C1")

        when:
        def d = depict.depict(mol)
        def svg = d.toSvgStr()

        then:
        svg != null
        svg.length() > 0
        svg.contains("width='250.0'")
        svg.contains("height='250.0'")
    }

    void "smiles2svggrid"() {

        CDKMolDepict depict = new CDKMolDepict()
        def mols = [
                ChemUtils.readSmiles("[H]C1NCC(C)C(N)=C1"),
                ChemUtils.readSmiles("C1N=CC=CC1")
        ]

        when:
        def d = depict.depict(mols)
        def svg = d.toSvgStr()

        then:
        svg != null
        svg.length() > 0
        svg.count("class='mol'") == 2
    }

    void "smiles2svg highlight"() {

        CDKMolDepict depict = new CDKMolDepict(true, false)
        IAtomContainer mol = ChemUtils.readSmiles("[H]C1NCC(C)C(N)=C1")

        when:
        def d = depict.depict(mol, Color.ORANGE, [1, 2, 3])
        def svg = d.toSvgStr()

        then:
        svg != null
        svg.length() > 0
        svg.contains("outerglow")
    }

    void "smiles2png"() {

        CDKMolDepict depict = new CDKMolDepict()
        IAtomContainer mol = ChemUtils.readSmiles("[H]C1NCC(C)C(N)=C1")

        when:
        def d = depict.depict(mol)
        def img = d.toImg()
        byte[] png = depict.writeImage(img, 'png')
        //println png.length
        Files.write(java.nio.file.Paths.get("/tmp/myimage0.png"), png)

        then:
        png != null
        png.length > 0
    }

    void "smiles2png glow highlight"() {

        IAtomContainer mol = ChemUtils.readSmiles("[H]C1NCC(C)C(N)=C1")

        when:
        // img1 has highlights
        CDKMolDepict depict1 = new CDKMolDepict(true, false)
        def d1 = depict1.depict(mol, Color.ORANGE, [1, 2, 3])
        def img1 = d1.toImg()
        byte[] png1 = depict1.writeImage(img1, 'png')
        Files.write(java.nio.file.Paths.get("/tmp/myimage1.png"), png1)

        // img2 is not highlighted so will be smaller (?)
        CDKMolDepict depict2 = new CDKMolDepict(false, false)
        def d2 = depict2.depict(mol)
        def img2 = d2.toImg()
        byte[] png2 = depict2.writeImage(img2, 'png')
        Files.write(java.nio.file.Paths.get("/tmp/myimage2.png"), png2)

        then:
        png1 != null
        png1.length > 0
        png1.length > png2.length
    }

    void "smiles2png mcs"() {

        CDKMolDepict depict = new CDKMolDepict()
        IAtomContainer mol = ChemUtils.readSmiles("CC[C@@H](C(=O)O)N")
        IAtomContainer query = ChemUtils.readSmiles("C[C@@H](C(=O)O)N")
        depict.setMCSAlignment(query, Color.ORANGE)

        when:
        def d = depict.depict(mol)
        def img = d.toImg()
        byte[] png = depict.writeImage(img, 'png')
        Files.write(java.nio.file.Paths.get("/tmp/myimage3.png"), png)

        then:
        png != null
        png.length > 0
    }

    void "smiles2png highlight + mcs"() {

        CDKMolDepict depict = new CDKMolDepict(true, false)
        IAtomContainer mol = ChemUtils.readSmiles("CC[C@@H](C(=O)O)N")
        IAtomContainer query = ChemUtils.readSmiles("[C@@H](C(=O)O)")
        depict.setMCSAlignment(query, Color.ORANGE)

        when:
        def d = depict.depict(mol, Color.CYAN, [0,1])
        def img = d.toImg()
        byte[] png = depict.writeImage(img, 'png')
        Files.write(java.nio.file.Paths.get("/tmp/myimage4.png"), png)

        then:
        png != null
        png.length > 0
    }

    void "no stereo"() {

        IAtomContainer mol = ChemUtils.readSmiles("C[C@@H](C(=O)O)N")
        CDKMolDepict depict = new CDKMolDepict(false, true)

        when:
        def d = depict.depict(mol)
        def img = d.toImg()
        byte[] png = depict.writeImage(img, 'png')
        Files.write(java.nio.file.Paths.get("/tmp/myimage5.png"), png)

        then:
        png != null
        png.length > 0
    }

    void "alignment error"() {

        // this alignment causes an error in CDK (probably bug).
        // this test checks that the error is ignored and an image is still generated.

        CDKMolDepict depict = new CDKMolDepict()
        IAtomContainer mol = ChemUtils.readSmiles("BrC1CCC(Cc2ccccc2)C1")
        IAtomContainer query = ChemUtils.readSmiles("ClC1CCC(Cc2ccccc2)C1")
        depict.setMCSAlignment(query, Color.ORANGE)

        when:
        def d = depict.depict(mol)
        def img = d.toImg()
        byte[] png = depict.writeImage(img, 'png')
        Files.write(java.nio.file.Paths.get("/tmp/myimage6.png"), png)

        then:
        png != null
        png.length > 0
    }

    void "smiles2pngWithAtomLabels"() {

        CDKMolDepict depict = new CDKMolDepict()
        IAtomContainer mol = ChemUtils.readSmiles("[H]C1NCC(C)C(N)=C1")
        mol.getAtom(0).setProperty(CDKConstants.COMMENT, "H0 label");
        mol.getAtom(1).setProperty(CDKConstants.COMMENT, "C1 label");
        mol.getAtom(2).setProperty(CDKConstants.COMMENT, "N2 label");

        when:
        def d = depict.depict(mol)
        def img = d.toImg()
        byte[] png = depict.writeImage(img, 'png')
        Files.write(java.nio.file.Paths.get("/tmp/myimage7.png"), png)

        then:
        png != null
        png.length > 0
    }

}
