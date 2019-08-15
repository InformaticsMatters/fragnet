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

package org.squonk.fragnet.depict

import org.openscience.cdk.interfaces.IAtomContainer
import org.openscience.cdk.layout.StructureDiagramGenerator
import spock.lang.Specification

import javax.vecmath.Vector2d
import java.awt.*
import java.nio.file.Files

class CDKMolDepictSpec extends Specification {

    void "smiles2svg"() {


        CDKMolDepict depict = new CDKMolDepict()

        when:
        def svg = depict.smilesToSVG("[H]C1NCC(C)C(N)=C1")
        //println svg

        then:
        svg != null
        svg.length() > 0
        svg.contains("width='50.0'")
        svg.contains("height='50.0'")
    }

    void "smiles2svg highlight"() {


        CDKMolDepict depict = new CDKMolDepict()
        IAtomContainer mol = ChemUtils.readSmiles("[H]C1NCC(C)C(N)=C1")

        when:
        def svg = depict.moleculeToSVG(mol, Color.ORANGE, [1, 2, 3], true)
        //println svg

        then:
        svg != null
        svg.length() > 0
        svg.contains("outerglow")
    }

    void "smiles2png"() {


        CDKMolDepict depict = new CDKMolDepict(
                250, 250, 5, null, Color.WHITE, true)
        IAtomContainer mol = ChemUtils.readSmiles("[H]C1NCC(C)C(N)=C1")

        when:
        def img = depict.moleculeToImage(mol)
        byte[] png = depict.writeImage(img, 'png')
        //println png.length
        //Files.write(java.nio.file.Paths.get("/tmp/myimage.png"), png)


        then:
        png != null
        png.length > 0
    }

    void "smiles2png glow highlight"() {


        CDKMolDepict depict = new CDKMolDepict(
                250, 250, 5, null, Color.WHITE, true)
        IAtomContainer mol = ChemUtils.readSmiles("[H]C1NCC(C)C(N)=C1")


        when:
        // img1 has highlights
        def img1 = depict.moleculeToImage(mol, Color.ORANGE, [1, 2, 3], true)
        byte[] png1 = depict.writeImage(img1, 'png')
        //Files.write(java.nio.file.Paths.get("/tmp/myimage1.png"), png1)
        // img2 is not highlighted so will be smaller (?)
        def img2 = depict.moleculeToImage(mol)
        byte[] png2 = depict.writeImage(img2, 'png')
        //Files.write(java.nio.file.Paths.get("/tmp/myimage2.png"), png2)

        then:
        png1 != null
        png1.length > 0
        png1.length > png2.length
    }

    void "smiles2png mcs"() {

        CDKMolDepict depict = new CDKMolDepict(
                250, 250, 5, null, Color.WHITE, true)
        IAtomContainer mol = ChemUtils.readSmiles("[H]C1NCC(C)C(N)=C1")
        IAtomContainer query = ChemUtils.readSmiles("C1NCCC=C1")
        depict.setMCSAlignment(query, Color.ORANGE)

        when:
        def img = depict.moleculeToImage(mol)
        byte[] png = depict.writeImage(img, 'png')
        //Files.write(java.nio.file.Paths.get("/tmp/myimage3.png"), png)

        then:
        png != null
        png.length > 0
    }

    void "smiles2png highlight + mcs"() {

        CDKMolDepict depict = new CDKMolDepict(
                250, 250, 5, null, Color.WHITE, true)
        IAtomContainer mol = ChemUtils.readSmiles("[H]C1NCC(C)C(N)=C1")
        IAtomContainer query = ChemUtils.readSmiles("C1NCCC=C1")
        depict.setMCSAlignment(query, Color.ORANGE)

        when:
        def img = depict.moleculeToImage(mol, Color.CYAN, [6,7,8], false)
        byte[] png = depict.writeImage(img, 'png')
        //Files.write(java.nio.file.Paths.get("/tmp/myimage4.png"), png)

        then:
        png != null
        png.length > 0
    }
}
