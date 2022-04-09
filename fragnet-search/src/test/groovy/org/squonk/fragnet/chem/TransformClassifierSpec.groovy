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
package org.squonk.fragnet.chem

import org.RDKit.RWMol
import org.squonk.fragnet.search.model.v2.GroupingType
import org.squonk.fragnet.search.model.v2.MolTransform
import org.squonk.fragnet.search.model.v2.TransformData
import spock.lang.IgnoreIf
import spock.lang.Specification

@IgnoreIf({!env.RDBASE})
class TransformClassifierSpec extends Specification {

    static {
        // This is necessary to load the RDKit libraries.
        // Needs to be done once somewhere before any RDKit code is called.
//        println "LD_LIBRARY_PATH=" + System.getenv("LD_LIBRARY_PATH")
//        String p = System.getProperty("java.library.path")
//        println "java.library.path=" + p
//        println "java.class.path=" + System.getProperty("java.class.path")
        Runtime.getRuntime().loadLibrary0(groovy.lang.GroovyClassLoader.class, "GraphMolWrap")
    }

    void "FG addition 1"() {

        when:
        def result = TransformClassifierUtils.generateMolTransform(
                "Oc1ccc(-c2ccccc2)cc1",
                "FG|C[Xe]|C[100Xe]|RING|Oc1ccc(-c2ccccc2)cc1[Xe]|OC1CCC(C2CCCCC2)CC1[100Xe]", true,
                "Cc1cc(-c2ccccc2)ccc1O")

        then:
        result.scaffold == "Oc1ccc(-c2ccccc2)cc1[Xe]"
        result.classification == GroupingType.FG_ADDITION
    }

    void "FG addition 2"() {

        when:
        def result = TransformClassifierUtils.generateMolTransform(
                "COc1ccccc1CN1CCCC1",
                "FG|OC[Xe]|OC[103Xe]|RING|COc1ccccc1CN1CCCC1[Xe]|COC1CCCCC1CC1CCCC1[103Xe]", true,
                "COc1ccccc1CN1CCCC1CO")

        then:
        result.scaffold == "COc1ccccc1CN1CCCC1[Xe]"
        result.classification == GroupingType.FG_ADDITION
    }


    void "FG deletion"() {

        when:
        def result = TransformClassifierUtils.generateMolTransform(
                "Cc1cc(-c2ccccc2)ccc1O",
                "FG|C[Xe]|C[100Xe]|RING|Oc1ccc(-c2ccccc2)cc1[Xe]|OC1CCC(C2CCCCC2)CC1[100Xe]", false,
                "Oc1ccc(-c2ccccc2)cc1")

        then:
        result.scaffold == "Oc1ccc(-c2ccccc2)cc1"
        result.classification == GroupingType.FG_DELETION
    }

    void "FG deletion deletion"() {

        when:
        def result = TransformClassifierUtils.generateMolTransform(
                "CCc1cccc(-c2ccc(O)c(C)c2)c1",
                "FG|CC[Xe]|CC[100Xe]|RING|Cc1cc(-c2cccc([Xe])c2)ccc1O|CC1CC(C2CCCC([100Xe])C2)CCC1O", false,
                "Cc1cc(-c2ccccc2)ccc1O",
                "FG|C[Xe]|C[100Xe]|RING|Oc1ccc(-c2ccccc2)cc1[Xe]|OC1CCC(C2CCCCC2)CC1[100Xe]", false,
                "Oc1ccc(-c2ccccc2)cc1"
        )

        then:
        result.scaffold == "Oc1ccc(-c2ccccc2)cc1"
        result.classification == GroupingType.FG_DELETIONS
    }

    void "FG addition addition 1"() {

        // transform involves addition of 2 FGs
        // MATCH p=(m:F2)-[:FRAG*1..2]-(e:F2) WHERE m.smiles='Oc1ccc(-c2ccccc2)cc1' AND e.smiles='CCc1cccc(-c2ccc(O)c(C)c2)c1'
        // see fragnet-transforms/addition-addition-1.png

        when:
        def txdata1 = new TransformData("Oc1ccc(-c2ccccc2)cc1",
                "FG|C[Xe]|C[100Xe]|RING|Oc1ccc(-c2ccccc2)cc1[Xe]|OC1CCC(C2CCCCC2)CC1[100Xe]", true,
                "Cc1cc(-c2ccccc2)ccc1O",
                "FG|CC[Xe]|CC[100Xe]|RING|Cc1cc(-c2cccc([Xe])c2)ccc1O|CC1CC(C2CCCC([100Xe])C2)CCC1O", true,
                "CCc1cccc(-c2ccc(O)c(C)c2)c1")

        def txdata2 = new TransformData(
                "Oc1ccc(-c2ccccc2)cc1",
                "FG|CC[Xe]|CC[100Xe]|RING|Oc1ccc(-c2cccc([Xe])c2)cc1|OC1CCC(C2CCCC([100Xe])C2)CC1", true,
                "CCc1cccc(-c2ccc(O)cc2)c1",
                "FG|C[Xe]|C[103Xe]|RING|CCc1cccc(-c2ccc(O)c([Xe])c2)c1|CCC1CCCC(C2CCC(O)C([103Xe])C2)C1", true,
                "CCc1cccc(-c2ccc(O)c(C)c2)c1")

        def result1 = TransformClassifierUtils.generateMolTransform(txdata1)
        def result2 = TransformClassifierUtils.generateMolTransform(txdata2)
        def best = TransformClassifierUtils.determineSimplestTransform([txdata1, txdata2])

        then:
        result1.scaffold == "Oc1ccc(-c2cccc([Xe])c2)cc1[Xe]"
        result1.classification == GroupingType.FG_ADDITIONS
        result1 == result2
        best.equals(txdata1)
    }

    void "FG addition addition 2"() {

        // transform involves addition of 2 FGs
        // MATCH p=(m:F2)-[:FRAG*1..2]-(e:F2) WHERE m.smiles='COc1ccccc1CN1CCCC1' AND e.smiles='COc1ccccc1CN1CC(OC)CC1CO' RETURN p
        // see fragnet-transforms/addition-addition-2.png

        when:
        def result1 = TransformClassifierUtils.generateMolTransform(
                "COc1ccccc1CN1CCCC1",
                "FG|CO[Xe]|CO[103Xe]|RING|COc1ccccc1CN1CCC([Xe])C1|COC1CCCCC1CC1CCC([103Xe])C1", true,
                "COc1ccccc1CN1CCC(OC)C1",
                "FG|OC[Xe]|OC[104Xe]|RING|COc1ccccc1CN1CC(OC)CC1[Xe]|COC1CC([104Xe])C(CC2CCCCC2OC)C1", true,
                "COc1ccccc1CN1CC(OC)CC1CO"
        )

        def result2 = TransformClassifierUtils.generateMolTransform(
                "COc1ccccc1CN1CCCC1",
                "FG|OC[Xe]|OC[103Xe]|RING|COc1ccccc1CN1CCCC1[Xe]|COC1CCCCC1CC1CCCC1[103Xe]", true,
                "COc1ccccc1CN1CCCC1CO",
                "FG|CO[Xe]|CO[103Xe]|RING|COc1ccccc1CN1CC([Xe])CC1CO|COC1CCCCC1CC1CC([103Xe])CC1CO", true,
                "COc1ccccc1CN1CC(OC)CC1CO"
        )

        then:
        result1.scaffold == "COc1ccccc1CN1CC([Xe])CC1[Xe]"
        result1.classification == GroupingType.FG_ADDITIONS
        result2.scaffold == "COc1ccccc1CN1CC([Xe])CC1[Xe]"
        result2.classification == GroupingType.FG_ADDITIONS

    }

    void "FG addition deletion 2 paths"() {

        // transform involves deletion of one FG and addition of a FG at a different site, or addition followed by deletion
        // MATCH p=(m:F2)-[:FRAG*1..2]-(e:F2) WHERE m.smiles='Oc1ccc(-c2ccccc2)cc1' AND e.smiles='Ic1cccc(-c2ccccc2)c1' RETURN p
        // see fragnet-transforms/addition-deletion-1.png

        when:
        // deletion followed by addition
        def txdata1 = new TransformData(
                "Oc1ccc(-c2ccccc2)cc1",
                "FG|O[Xe]|O[100Xe]|RING|[Xe]c1ccc(-c2ccccc2)cc1|[100Xe]C1CCC(C2CCCCC2)CC1", false,
                "c1ccc(-c2ccccc2)cc1",
                "FG|I[Xe]|I[100Xe]|RING|[Xe]c1cccc(-c2ccccc2)c1|[100Xe]C1CCCC(C2CCCCC2)C1", true,
                "Ic1cccc(-c2ccccc2)c1")
        // addition followed by deletion
        def txdata2 = new TransformData(
                "Oc1ccc(-c2ccccc2)cc1",
                "FG|I[Xe]|I[102Xe]|RING|Oc1ccc(-c2ccccc2)cc1[Xe]|OC1CCC(C2CCCCC2)CC1[102Xe]", true,
                "Oc1ccc(-c2ccccc2)cc1I",
                "FG|O[Xe]|O[100Xe]|RING|Ic1cc(-c2ccccc2)ccc1[Xe]|IC1CC(C2CCCCC2)CCC1[100Xe]", false,
                "Ic1cccc(-c2ccccc2)c1")

        def result1 = TransformClassifierUtils.generateMolTransform(txdata1)
        def result2 = TransformClassifierUtils.generateMolTransform(txdata2)
        def best = TransformClassifierUtils.determineSimplestTransform([txdata1, txdata2])


        then:
        result1.scaffold == "[Xe]c1cccc(-c2ccccc2)c1"
        result1.classification == GroupingType.FG_ADDITION_DELETION
        result1 == result2
        best.equals(txdata1) // deletion+addition is better than addition+deletion
    }

    void "FG addition deletion 3 paths 1"() {

        // transform involves deletion of one FG and addition of a FG at a different site, or addition followed by deletion
        // MATCH p=(m:F2)-[:FRAG*1..2]-(e:F2) WHERE m.smiles='Oc1ccc(-c2ccccc2)cc1' AND e.smiles='Cc1cccc(-c2ccccc2)c1' RETURN p
        // see fragnet-transforms/addition-deletion-2.png

        when:
        // addition followed by deletion
        def txdata1 = new TransformData(
                "Oc1ccc(-c2ccccc2)cc1",
                "FG|C[Xe]|C[100Xe]|RING|Oc1ccc(-c2cccc([Xe])c2)cc1|OC1CCC(C2CCCC([100Xe])C2)CC1", true,
                "Cc1cccc(-c2ccc(O)cc2)c1",
                "FG|O[Xe]|O[102Xe]|RING|Cc1cccc(-c2ccc([Xe])cc2)c1|CC1CCCC(C2CCC([102Xe])CC2)C1", false,
                "Cc1cccc(-c2ccccc2)c1")
        // deletion followed by addition
        def txdata2 = new TransformData(
                "Oc1ccc(-c2ccccc2)cc1",
                "FG|O[Xe]|O[100Xe]|RING|[Xe]c1ccc(-c2ccccc2)cc1|[100Xe]C1CCC(C2CCCCC2)CC1", false,
                "c1ccc(-c2ccccc2)cc1",
                "FG|C[Xe]|C[100Xe]|RING|[Xe]c1cccc(-c2ccccc2)c1|[100Xe]C1CCCC(C2CCCCC2)C1", true,
                "Cc1cccc(-c2ccccc2)c1")
        // addition followed by deletion
        def txdata3 = new TransformData(
                "Oc1ccc(-c2ccccc2)cc1",
                "FG|C[Xe]|C[100Xe]|RING|Oc1ccc(-c2ccccc2)cc1[Xe]|OC1CCC(C2CCCCC2)CC1[100Xe]", true,
                "Cc1cccc(-c2ccc(O)cc2)c1",
                "FG|O[Xe]|O[102Xe]|RING|Cc1cc(-c2ccccc2)ccc1[Xe]|CC1CC(C2CCCCC2)CCC1[102Xe]", false,
                "Cc1cccc(-c2ccccc2)c1")

        def result1 = TransformClassifierUtils.generateMolTransform(txdata1)
        def result2 = TransformClassifierUtils.generateMolTransform(txdata2)
        def result3 = TransformClassifierUtils.generateMolTransform(txdata3)
        def best = TransformClassifierUtils.determineSimplestTransform([txdata1, txdata2, txdata3])

        then:
        result1.scaffold == "[Xe]c1cccc(-c2ccccc2)c1"
        result1.classification == GroupingType.FG_ADDITION_DELETION
        result1 == result2
        result1 == result3
        result2 == result3
        best.equals(txdata2)
    }

    void "FG addition deltion 3 paths 2"() {

        // MATCH p=(m:F2)-[:FRAG*1..2]-(e:F2) WHERE m.smiles='Oc1ccc(-c2ccccc2)cc1' AND e.smiles='OCc1cccc(-c2ccccc2)c1' RETURN p
        // see fragnet-transforms/addition-deletion-3.png

        when:
        def txdata1 = new TransformData(
                "Oc1ccc(-c2ccccc2)cc1",
                "FG|OC[Xe]|OC[100Xe]|RING|Oc1ccc(-c2cccc([Xe])c2)cc1|OC1CCC(C2CCCC([100Xe])C2)CC1", true,
                "OCc1cccc(-c2ccc(O)cc2)c1",
                "FG|O[Xe]|O[102Xe]|RING|OCc1cccc(-c2ccc([Xe])cc2)c1|OCC1CCCC(C2CCC([102Xe])CC2)C1", false,
                "OCc1cccc(-c2ccccc2)c1")

        def txdata2 = new TransformData(
                "Oc1ccc(-c2ccccc2)cc1",
                "FG|O[Xe]|O[100Xe]|RING|[Xe]c1ccc(-c2ccccc2)cc1|[100Xe]C1CCC(C2CCCCC2)CC1", false,
                "c1ccc(-c2ccccc2)cc1",
                "FG|OC[Xe]|OC[100Xe]|RING|[Xe]c1cccc(-c2ccccc2)c1|[100Xe]C1CCCC(C2CCCCC2)C1", true,
                "OCc1cccc(-c2ccccc2)c1")

        def txdata3 = new TransformData(
                "Oc1ccc(-c2ccccc2)cc1",
                "FG|OC[Xe]|OC[100Xe]|RING|Oc1ccc(-c2ccccc2)cc1[Xe]|OC1CCC(C2CCCCC2)CC1[100Xe]", true,
                "OCc1cc(-c2ccccc2)ccc1O",
                "FG|O[Xe]|O[102Xe]|RING|OCc1cc(-c2ccccc2)ccc1[Xe]|OCC1CC(C2CCCCC2)CCC1[102Xe]", false,
                "OCc1cccc(-c2ccccc2)c1")

        def result1 = TransformClassifierUtils.generateMolTransform(txdata1)
        def result2 = TransformClassifierUtils.generateMolTransform(txdata2)
        def result3 = TransformClassifierUtils.generateMolTransform(txdata3)
        def best = TransformClassifierUtils.determineSimplestTransform([txdata1, txdata2, txdata3])

        then:
        result1.scaffold == '[Xe]c1cccc(-c2ccccc2)c1'
        result1.classification == GroupingType.FG_ADDITION_DELETION

        result2.scaffold == '[Xe]c1cccc(-c2ccccc2)c1'
        result2.classification == GroupingType.FG_ADDITION_DELETION

        result3.scaffold == '[Xe]c1cccc(-c2ccccc2)c1'
        result3.classification == GroupingType.FG_ADDITION_DELETION

        best.equals(txdata2)
    }


    void "FG subsitute"() {

        // transform involves deletion of one FG and addition of a FG at the same site (or a symmetrical site)
        // MATCH p=(m:F2)-[:FRAG*1..2]-(e:F2) WHERE m.smiles='Oc1ccc(-c2ccccc2)cc1' AND e.smiles='CCc1ccc(-c2ccccc2)cc1' RETURN p
        // see fragnet-transforms/replace-fg-1.png

        when:
        // deletion followed by addition
        def txdata1 = new TransformData(
                "Oc1ccc(-c2ccccc2)cc1",
                "FG|O[Xe]|O[100Xe]|RING|[Xe]c1ccc(-c2ccccc2)cc1|[100Xe]C1CCC(C2CCCCC2)CC1", false,
                "c1ccc(-c2ccccc2)cc1",
                "FG|CC[Xe]|CC[100Xe]|RING|[Xe]c1ccc(-c2ccccc2)cc1|[100Xe]C1CCC(C2CCCCC2)CC1", true,
                "CCc1ccc(-c2ccccc2)cc1")

        // addition at a symmetrical site followed by deletion
        def txdata2 = new TransformData(
                "Oc1ccc(-c2ccccc2)cc1",
                "FG|CC[Xe]|CC[100Xe]|RING|Oc1ccc(-c2ccc([Xe])cc2)cc1|OC1CCC(C2CCC([100Xe])CC2)CC1", true,
                "CCc1ccc(-c2ccc(O)cc2)cc1",
                "FG|O[Xe]|O[102Xe]|RING|CCc1ccc(-c2ccc([Xe])cc2)cc1|CCC1CCC(C2CCC([102Xe])CC2)CC1", false,
                "CCc1ccc(-c2ccccc2)cc1")

        def result1 = TransformClassifierUtils.generateMolTransform(txdata1)
        def result2 = TransformClassifierUtils.generateMolTransform(txdata2)
        def best = TransformClassifierUtils.determineSimplestTransform([txdata1, txdata2])

        then:
        result1.scaffold == "[Xe]c1ccc(-c2ccccc2)cc1"
        result1.classification == GroupingType.SUBSTITUTE_FG
        result1 == result2
        best.equals(txdata1)
    }

    void "RING addition deletion 1"() {

        // transform involves addition of 2 FGs
        // MATCH p=(m:F2)-[:FRAG*1..2]-(e:F2) WHERE m.smiles='Oc1ccc(-c2ccccc2)cc1' AND e.smiles='Oc1cccc(-n2cncn2)c1' RETURN p

        when:
        def result1 = TransformClassifierUtils.generateMolTransform(
                "Oc1ccc(-c2ccccc2)cc1",
                "RING|[Xe]c1ccccc1|[101Xe]C1CCCCC1|RING|Oc1ccc([Xe])cc1|OC1CCC([101Xe])CC1", false,
                "Oc1ccccc1",
                "RING|[Xe]n1cncn1|[101Xe]C1CCCC1|RING|Oc1cccc([Xe])c1|OC1CCCC([101Xe])C1", true,
                "Oc1cccc(-n2cncn2)c1"
        )

        then:
        result1.scaffold == "Oc1cccc([Xe])c1"
        result1.classification == GroupingType.RING_ADDITION_DELETION
    }


    void "RING addition deletion  2"() {

        // see fragnet-transforms/ring-addition-deletion-1.png

        when:
        // deletion followed by addition
        // MATCH p=(m:F2)-[:FRAG*1..2]-(e:F2) WHERE m.smiles='Oc1ccc(-c2ccccc2)cc1' AND e.smiles='Oc1ccccc1C1CC=NO1' RETURN p
        def txdata1 = new TransformData(
                "Oc1ccc(-c2ccccc2)cc1",
                "RING|[Xe]c1ccccc1|[101Xe]C1CCCCC1|RING|Oc1ccc([Xe])cc1|OC1CCC([101Xe])CC1", false,
                "Oc1ccccc1",
                "RING|[Xe]C1CC=NO1|[101Xe]C1CCCC1|RING|Oc1ccccc1[Xe]|OC1CCCCC1[101Xe]", true,
                "Oc1ccccc1C1CC=NO1")

        // the next two are 2 paths to a different molecule but with the same transform
        // MATCH p=(m:F2)-[:FRAG*1..2]-(e:F2) WHERE m.smiles='Oc1ccc(-c2ccccc2)cc1' AND e.smiles='Oc1ccccc1-n1cnnc1' RETURN p
        def txdata2 = new TransformData(
                "Oc1ccc(-c2ccccc2)cc1",
                "RING|[Xe]c1ccccc1|[101Xe]C1CCCCC1|RING|Oc1ccc([Xe])cc1|OC1CCC([101Xe])CC1", false,
                "Oc1ccccc1",
                "RING|[Xe]n1cnnc1|[101Xe]C1CCCC1|RING|Oc1ccccc1[Xe]|OC1CCCCC1[101Xe]", true,
                "Oc1ccccc1-n1cnnc1")

        def txdata3 = new TransformData(
                "Oc1ccc(-c2ccccc2)cc1",
                "RING|[Xe]n1cnnc1|[102Xe]C1CCCC1|RING|Oc1ccc(-c2ccccc2)cc1[Xe]|OC1CCC(C2CCCCC2)CC1[102Xe]", true,
                "Oc1ccc(-c2ccccc2)cc1-n1cnnc1",
                "RING|[Xe]c1ccccc1|[101Xe]C1CCCCC1|RING|Oc1ccc([Xe])cc1-n1cnnc1|OC1CCC([101Xe])CC1C1CCCC1", false,
                "Oc1ccccc1-n1cnnc1")

        def result1 = TransformClassifierUtils.generateMolTransform(txdata1)
        def result2 = TransformClassifierUtils.generateMolTransform(txdata2)
        def result3 = TransformClassifierUtils.generateMolTransform(txdata3)
        def best = TransformClassifierUtils.determineSimplestTransform([txdata2, txdata3])


        then:
        result1.scaffold == "Oc1ccccc1[Xe]"
        result1.classification == GroupingType.RING_ADDITION_DELETION
        result1 == result2
        result1 == result3
        result2 == result3
        best.equals(txdata2)

    }

    void "RING substitution 1"() {

        // transform involves removal of a ring and addition of another ring at the same site

        when:
        def result1 = TransformClassifierUtils.generateMolTransform(
                "Oc1ccc(-c2ccccc2)cc1",
                "RING|[Xe]c1ccccc1|[101Xe]C1CCCCC1|RING|Oc1ccc([Xe])cc1|OC1CCC([101Xe])CC1", false,
                "Oc1ccccc1",
                "RING|O=C1CC([Xe])CN1|OC1CCC([100Xe])C1|RING|Oc1ccc([Xe])cc1|OC1CCC([100Xe])CC1", true,
                "O=C1CC(c2ccc(O)cc2)CN1"
        )

        then:
        result1.scaffold == "Oc1ccc([Xe])cc1"
        result1.classification == GroupingType.SUBSTITUTE_RING
    }


    void "replace linker"() {

        // see fragnet-transforms/replace-linker-1.png

        when:
        // MATCH p=(m:F2)-[:FRAG*1..2]-(e:F2) WHERE m.smiles='Oc1ccc(-c2ccccc2)cc1' AND e.smiles='CC(c1ccccc1)c1ccc(O)cc1' RETURN p
        def result1 = TransformClassifierUtils.generateMolTransform(
                "Oc1ccc(-c2ccccc2)cc1",
                "FG|[Xe]|[Xe]|RING|Oc1ccc([Xe])cc1.[Xe]c1ccccc1|OC1CCC([Xe])CC1.[Xe]C1CCCCC1", false,
                "Oc1ccccc1.c1ccccc1",
                "FG|CC([Xe])[Xe]|CC([100Xe])[101Xe]|RING|Oc1ccc([Xe])cc1.[Xe]c1ccccc1|OC1CCC([101Xe])CC1.[100Xe]C1CCCCC1", true,
                "CC(c1ccccc1)c1ccc(O)cc1"
        )

        // MATCH p=(m:F2)-[:FRAG*1..2]-(e:F2) WHERE m.smiles='Oc1ccc(-c2ccccc2)cc1' AND e.smiles='NC(Cc1ccccc1)c1ccc(O)cc1' RETURN p
        def result2 = TransformClassifierUtils.generateMolTransform(
                "Oc1ccc(-c2ccccc2)cc1",
                "FG|[Xe]|[Xe]|RING|Oc1ccc([Xe])cc1.[Xe]c1ccccc1|OC1CCC([Xe])CC1.[Xe]C1CCCCC1", false,
                "Oc1ccccc1.c1ccccc1",
                "FG|NC([Xe])C[Xe]|NC([101Xe])C[100Xe]|RING|Oc1ccc([Xe])cc1.[Xe]c1ccccc1|OC1CCC([101Xe])CC1.[100Xe]C1CCCCC1", true,
                "NC(Cc1ccccc1)c1ccc(O)cc1"
        )

        then:
        result1.scaffold == "Oc1ccc([Xe])cc1.[Xe]c1ccccc1"
        result1.classification == GroupingType.SUBSTITUTE_LINKER
        result1 == result2

    }

    void "FG and RING additions 1"() {

        // MATCH p=(m:F2)-[:FRAG*1..2]-(e:F2) WHERE m.smiles='COc1ccccc1CN1CCCC1' AND e.smiles='COc1ccccc1CN1CC(CO)C(c2ccccn2)C1' RETURN p
        // see fragnet-transforms/fg+ring-additions-1.png

        when:
        def result1 = TransformClassifierUtils.generateMolTransform(
                "COc1ccccc1CN1CCCC1",
                "FG|OC[Xe]|OC[103Xe]|RING|COc1ccccc1CN1CCC([Xe])C1|COC1CCCCC1CC1CCC([103Xe])C1", true,
                "COc1ccccc1CN1CCC(CO)C1",
                "RING|[Xe]c1ccccn1|[104Xe]C1CCCCC1|RING|COc1ccccc1CN1CC([Xe])C(CO)C1|COC1CCCCC1CC1CC([104Xe])C(CO)C1", true,
                "COc1ccccc1CN1CC(CO)C(c2ccccn2)C1"
        )

        def result2 = TransformClassifierUtils.generateMolTransform(
                "COc1ccccc1CN1CCCC1",
                "RING|[Xe]c1ccccn1|[103Xe]C1CCCCC1|RING|COc1ccccc1CN1CCC([Xe])C1|COC1CCCCC1CC1CCC([103Xe])C1", true,
                "COc1ccccc1CN1CCC(c2ccccn2)C1",
                "FG|OC[Xe]|OC[103Xe]|RING|COc1ccccc1CN1CC([Xe])C(c2ccccn2)C1|COC1CCCCC1CC1CC([103Xe])C(C2CCCCC2)C1", true,
                "COc1ccccc1CN1CC(CO)C(c2ccccn2)C1"
        )

        //println "result1 $result1"

        then:
        result1.scaffold == "COc1ccccc1CN1CC([Xe])C([Xe])C1"
        result1.classification == GroupingType.ADDITIONS
        result1 == result2
    }


    void "FG addition to added RING 1"() {
        // MATCH p=(m:F2)-[:FRAG*1..2]-(e:F2) WHERE m.smiles='COc1ccccc1CN1CCCC1' AND e.smiles='COc1ccccc1CN1CCC(c2cnn(C)c2)C1' RETURN p
        // see fragnet-transforms/fg-addition-to-added-ring-1.png

        when:
        def txdata1 = new TransformData(
                "COc1ccccc1CN1CCCC1",
                "RING|[Xe]c1cn[nH]c1|[103Xe]C1CCCC1|RING|COc1ccccc1CN1CCC([Xe])C1|COC1CCCCC1CC1CCC([103Xe])C1", true,
                "COc1ccccc1CN1CCC(c2cn[nH]c2)C1",
                "FG|C[Xe]|C[104Xe]|RING|COc1ccccc1CN1CCC(c2cnn([Xe])c2)C1|COC1CCCCC1CC1CCC(C2CCC([104Xe])C2)C1", true,
                "COc1ccccc1CN1CCC(c2cnn(C)c2)C1")

        def txdata2 = new TransformData(
                "COc1ccccc1CN1CCCC1",
                "FG|C|C|RING|COc1ccccc1CN1CCCC1|COC1CCCCC1CC1CCCC1", true,
                "C.COc1ccccc1CN1CCCC1",
                "RING|[Xe]c1cnn([Xe])c1|[103Xe]C1CCC([104Xe])C1|RING|COc1ccccc1CN1CCC([Xe])C1.C[Xe]|COC1CCCCC1CC1CCC([103Xe])C1.C[104Xe]", true,
                "COc1ccccc1CN1CCC(c2cnn(C)c2)C1")

        def result1 = TransformClassifierUtils.generateMolTransform(txdata1)
        def result2 = TransformClassifierUtils.generateMolTransform(txdata2)
        def best = TransformClassifierUtils.determineSimplestTransform([txdata1, txdata2])

        then:
        result1.scaffold == "COc1ccccc1CN1CCC([Xe])C1"
        result1.classification == GroupingType.ADDITIONS
        result1 == result2
        best.equals(txdata1)
    }


    void "FG addition to added RING 2"() {

        //  MATCH p=(m:F2)-[:FRAG*1..2]-(e:F2) WHERE m.smiles='COc1ccccc1CN1CCCC1' AND e.smiles='COc1ccccc1CN1CCC(c2nc3cc(C)ccc3o2)C1' RETURN p
        // see fragnet-transforms/fg-addition-to-added-ring-2.png

        when:
        def txdata1 = new TransformData(
                "COc1ccccc1CN1CCCC1",
                "FG|C|C|RING|COc1ccccc1CN1CCCC1|COC1CCCCC1CC1CCCC1", true,
                "C.COc1ccccc1CN1CCCC1",
                "RING|[Xe]c1ccc2oc([Xe])nc2c1|[103Xe]C1CC2CCC([104Xe])CC2C1|RING|COc1ccccc1CN1CCC([Xe])C1.C[Xe]|COC1CCCCC1CC1CCC([103Xe])C1.C[104Xe]", true,
                "COc1ccccc1CN1CCC(c2nc3cc(C)ccc3o2)C1")

        def txdata2 = new TransformData(
                "COc1ccccc1CN1CCCC1",
                "RING|[Xe]c1nc2ccccc2o1|[103Xe]C1CC2CCCCC2C1|RING|COc1ccccc1CN1CCC([Xe])C1|COC1CCCCC1CC1CCC([103Xe])C1", true,
                "COc1ccccc1CN1CCC(c2nc3ccccc3o2)C1",
                "FG|C[Xe]|C[104Xe]|RING|COc1ccccc1CN1CCC(c2nc3cc([Xe])ccc3o2)C1|COC1CCCCC1CC1CCC(C2CC3CCC([104Xe])CC3C2)C1", true,
                "COc1ccccc1CN1CCC(c2nc3cc(C)ccc3o2)C1")

        def result1 = TransformClassifierUtils.generateMolTransform(txdata1)
        def result2 = TransformClassifierUtils.generateMolTransform(txdata2)
        def best = TransformClassifierUtils.determineSimplestTransform([txdata1, txdata2])

        then:
        result1.scaffold == 'COc1ccccc1CN1CCC([Xe])C1'
        result1.classification == GroupingType.ADDITIONS
        result1 == result2
        best.equals(txdata2)
    }

    void "RING addition 1 and 2 paths"() {

        // MATCH p=(m:F2)-[:FRAG*1..2]-(e:F2) WHERE m.smiles='COc1ccccc1CN1CCCC1' AND e.smiles='COc1ccccc1CN1CCCC1c1cccnc1' RETURN p
        // see fragnet-transforms/

        when:
        def txdata1 = new TransformData(
                "COc1ccccc1CN1CCCC1",
                "RING|c1ccncc1|C1CCCCC1|RING|COc1ccccc1CN1CCCC1|COC1CCCCC1CC1CCCC1", true,
                "COc1ccccc1CN1CCCC1.c1ccncc1",
                "FG|[Xe]|[Xe]|RING|COc1ccccc1CN1CCCC1[Xe].[Xe]c1cccnc1|COC1CCCCC1CC1CCCC1[Xe].[Xe]C1CCCCC1", true,
                "COc1ccccc1CN1CCCC1c1cccnc1")

        def txdata2 = new TransformData(
                "COc1ccccc1CN1CCCC1",
                "RING|[Xe]c1cccnc1|[103Xe]C1CCCCC1|RING|COc1ccccc1CN1CCCC1[Xe]|COC1CCCCC1CC1CCCC1[103Xe]", true,
                "COc1ccccc1CN1CCCC1c1cccnc1")

        def result1 = TransformClassifierUtils.generateMolTransform(txdata1)
        def result2 = TransformClassifierUtils.generateMolTransform(txdata2)
        def best = TransformClassifierUtils.determineSimplestTransform([txdata1, txdata2])

        then:
        result1.scaffold == 'COc1ccccc1CN1CCCC1[Xe]'
        result1.classification == GroupingType.ADDITIONS

        result2.scaffold == 'COc1ccccc1CN1CCCC1[Xe]'
        result2.classification == GroupingType.RING_ADDITION
        best.equals(txdata2) // 1 hop better than 2

    }


    void "[1,1'‐biphenyl]‐3‐ol"() {

        // MATCH p=(m:F2)-[:FRAG*1..2]-(e:F2) WHERE m.smiles='Oc1ccc(-c2ccccc2)cc1' AND e.smiles = 'Oc1cccc(-c2ccccc2)c1' RETURN p

        when:
        def txdata1 = new TransformData(
                "Oc1ccc(-c2ccccc2)cc1",
                "FG|O[Xe]|O[102Xe]|RING|Oc1ccc(-c2cccc([Xe])c2)cc1|OC1CCC(C2CCCC([102Xe])C2)CC1", true,
                "Oc1ccc(-c2cccc(O)c2)cc1",
                "FG|O[Xe]|O[100Xe]|RING|Oc1cccc(-c2ccc([Xe])cc2)c1|OC1CCCC(C2CCC([100Xe])CC2)C1", false,
                "Oc1cccc(-c2ccccc2)c1")

        def txdata2 = new TransformData(
                "Oc1ccc(-c2ccccc2)cc1",
                "FG|O[Xe]|O[102Xe]|RING|Oc1ccc(-c2ccccc2)cc1[Xe]|OC1CCC(C2CCCCC2)CC1[102Xe]", true,
                "Oc1ccc(-c2ccccc2)cc1O",
                "FG|O[Xe]|O[100Xe]|RING|Oc1cc(-c2ccccc2)ccc1[Xe]|OC1CC(C2CCCCC2)CCC1[100Xe]", false,
                "Oc1cccc(-c2ccccc2)c1")

        def txdata3 = new TransformData(
                "Oc1ccc(-c2ccccc2)cc1",
                "RING|[Xe]c1ccc([Xe])cc1|[100Xe]C1CCC([101Xe])CC1|RING|O[Xe].[Xe]c1ccccc1|O[100Xe].[101Xe]C1CCCCC1", false,
                "O.c1ccccc1",
                "RING|[Xe]c1cccc([Xe])c1|[100Xe]C1CCCC([101Xe])C1|RING|O[Xe].[Xe]c1ccccc1|O[100Xe].[101Xe]C1CCCCC1", true,
                "Oc1cccc(-c2ccccc2)c1")

        def txdata4 = new TransformData(
                "Oc1ccc(-c2ccccc2)cc1",
                "RING|[Xe]c1ccccc1|[101Xe]C1CCCCC1|RING|Oc1ccc([Xe])cc1|OC1CCC([101Xe])CC1", false,
                "Oc1ccccc1",
                "RING|[Xe]c1ccccc1|[101Xe]C1CCCCC1|RING|Oc1cccc([Xe])c1|OC1CCCC([101Xe])C1", true,
                "Oc1cccc(-c2ccccc2)c1")

        def txdata5 = new TransformData(
                "Oc1ccc(-c2ccccc2)cc1",
                "FG|[Xe]|[Xe]|RING|Oc1ccc([Xe])cc1.[Xe]c1ccccc1|OC1CCC([Xe])CC1.[Xe]C1CCCCC1", false,
                "Oc1ccccc1.c1ccccc1",
                "FG|[Xe]|[Xe]|RING|Oc1cccc([Xe])c1.[Xe]c1ccccc1|OC1CCCC([Xe])C1.[Xe]C1CCCCC1", true,
                "Oc1cccc(-c2ccccc2)c1")

        def txdata6 = new TransformData(
                "Oc1ccc(-c2ccccc2)cc1",
                "FG|O[Xe]|O[100Xe]|RING|[Xe]c1ccc(-c2ccccc2)cc1|[100Xe]C1CCC(C2CCCCC2)CC1", false,
                "c1ccc(-c2ccccc2)cc1",
                "FG|O[Xe]|O[100Xe]|RING|[Xe]c1cccc(-c2ccccc2)c1|[100Xe]C1CCCC(C2CCCCC2)C1", true,
                "Oc1cccc(-c2ccccc2)c1")


        def result1 = TransformClassifierUtils.generateMolTransform(txdata1)
        def result2 = TransformClassifierUtils.generateMolTransform(txdata2)
        def result3 = TransformClassifierUtils.generateMolTransform(txdata3)
        def result4 = TransformClassifierUtils.generateMolTransform(txdata4)
        def result5 = TransformClassifierUtils.generateMolTransform(txdata5)
        def result6 = TransformClassifierUtils.generateMolTransform(txdata6)
        def best = TransformClassifierUtils.determineSimplestTransform([txdata1, txdata2, txdata3, txdata4, txdata5, txdata6])

        then:
        result1.scaffold == '[Xe]c1cccc(-c2ccccc2)c1'
        result1.classification == GroupingType.FG_ADDITION_DELETION
        result2 == result1
//        result3 == result1
//        result4 == result1
//        result5 == result1
        result6 == result1
        best.equals(txdata6)
    }

    void "triageMolTransforms remove undefined"() {

        when:
        def items = [new MolTransform("abcd", GroupingType.ADDITION, 1), new MolTransform("undefined", GroupingType.UNDEFINED, 1)]
        def tx = TransformClassifierUtils.triageMolTransforms(items, [true] as boolean[])

        then:
        tx.scaffold == "abcd"
        tx.classification == GroupingType.ADDITION
    }


    void "triageMolTransforms mixed"() {

        when:
        def items = [new MolTransform("abcd", GroupingType.ADDITION, 1), new MolTransform("xyz", GroupingType.DELETIONS, 1)]
        def tx = TransformClassifierUtils.triageMolTransforms(items, [true, true] as boolean[])

        then:
        tx.scaffold == "undefined-additions"
        tx.classification == GroupingType.ADDITIONS
    }

    void "triageMolTransforms mixed length"() {

        // this is the unusual case of there being a length 1 and a length 2 path
        // this is OK. It will be resolved when being triaged with the length 1 taking precedent

        when:
        def items = [new MolTransform("abcd", GroupingType.ADDITIONS, 2), new MolTransform("abcd", GroupingType.ADDITION, 1)]
        def tx = TransformClassifierUtils.triageMolTransforms(items, [true, true] as boolean[])

        then:
        tx.scaffold == "abcd"
        tx.classification == GroupingType.ADDITION
    }


    void "standardize smiles"() {
        when:
        def mol = RWMol.MolFromSmiles('OC1=CC=CC=C1C1CC=NO1')
        def smiles = mol.MolToSmiles()
        println smiles

        then:
        smiles != null
    }

}

