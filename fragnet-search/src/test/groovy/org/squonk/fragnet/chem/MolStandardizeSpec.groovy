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
package org.squonk.fragnet.chem

import org.RDKit.RDKFuncs
import org.RDKit.RWMol
import spock.lang.IgnoreIf
import spock.lang.Specification

@IgnoreIf({!env.RDBASE})
class MolStandardizeSpec extends Specification {

    static {
        Runtime.getRuntime().loadLibrary0(groovy.lang.GroovyClassLoader.class, "GraphMolWrap")
    }

    void "rdkit version"() {

        when:
        String v = org.RDKit.RDKFuncs.getRdkitVersion()
        println v

        then:
        v != null

    }

    void "cleanup vanilla rdkit"() {

        when:
        def mol1 = RWMol.MolFromSmiles("[Na]OC(=O)c1ccccc1")
        def mol2 = RDKFuncs.cleanup(mol1)
        def smiles = mol2.MolToSmiles()

        then:
        smiles == "O=C([O-])c1ccccc1.[Na+]"
    }

    void "default cleanup"() {

        when:
        def mol1 = RWMol.MolFromSmiles("[Na]OC(=O)c1ccccc1")
        def mol2 = MolStandardize.cleanup(mol1)
        def smiles = mol2.MolToSmiles()

        then:
        smiles == "O=C([O-])c1ccccc1.[Na+]"
    }

    String standardize(smiles) {
        def mol = RWMol.MolFromSmiles(smiles)
        mol = MolStandardize.defaultStandardize(mol)
        return mol.MolToSmiles()
    }

    void "default standardize"() {

        expect:
        standardize(i) == o

        where:
        i | o
        "O=C([O-])c1ccccc1"  | "O=C(O)c1ccccc1"
        "[Na]OC(=O)c1ccccc1" | "O=C(O)c1ccccc1"
        "O=C(O)c1ccccc1.O"   | "O=C(O)c1ccccc1"
        "OC(Cn1ccnn1)C1CC1"  | "OC(Cn1ccnn1)C1CC1"
    }

    void "remove isotopes"() {

        when:
        def mol = ChemUtils.molFromSmiles("O=[13C](O)c1ccccc1")
        MolStandardize.removeIsotopes(mol)
        def smiles = mol.MolToSmiles()

        then:
        smiles == "O=C(O)c1ccccc1"
    }

    void "uncharge vanilla rdkit"() {

        when:
        def mol1 = RWMol.MolFromSmiles("O=C([O-])c1ccccc1")
        println("mol1: " + mol1)
        def mol2 =  RDKFuncs.chargeParent(mol1, RDKFuncs.getDefaultCleanupParameters(), true)
        println("mol2: " + mol2)
        def smiles = mol2.MolToSmiles()

        then:
        smiles == "O=C(O)c1ccccc1"
    }

    void "uncharge"() {

        when:
        def mol1 = ChemUtils.molFromSmiles("O=C([O-])c1ccccc1")
        println("mol1: " + mol1)
        def mol2 = MolStandardize.uncharge(mol1, true)
        println("mol2: " + mol2)
        def smiles = mol2.MolToSmiles()

        then:
        smiles == "O=C(O)c1ccccc1"
    }

    void "noniso smiles"() {

        expect:
        MolStandardize.nonisoSmiles(x, true) == y

        where:
        x | y
        'COC=1C=CC(NC=2N=CN=C3NC=NC23)=CC1' | 'COc1ccc(Nc2ncnc3[nH]cnc23)cc1'
        'CC(=O)NC1=CC(C)=NN1' | 'CC(=O)Nc1cc(C)n[nH]1'
        'NC(=O)C=1C=CC(NC(=O)[C@@H]2CCCO2)=CC1' | 'NC(=O)c1ccc(NC(=O)C2CCCO2)cc1'
        'CS(=O)(=O)N1CCC[C@@H]1CN' | 'CS(=O)(=O)N1CCCC1CN'
        'NC(=O)C=1C=CC=CC1NC(=O)C=2C=CN=CC2' | 'NC(=O)c1ccccc1NC(=O)c1ccncc1'

    }

    void "read from text"() {
        def text = '''\
CCOc1ccccc1CN1CCC(O)CC1  1
COCC(=O)Nc1cccc(NC(C)=O)c1  2
'''
        when:
        def mols = MolStandardize.readStdNonisoSmilesFromSmilesData(text)

        then:
        mols.getMolecules().size() == 2
    }

    void "read smiles from file"() {

        when:
        def mols = MolStandardize.readStdNonisoSmilesFromSmilesFile('src/test/data/mols.smi')

        then:
        mols.getMolecules().size() == 2
    }

    void "read sdf from file"() {

        String path = "src/test/data/expand.sdf";

        when:
        File f = new File(path);
        System.out.println("File " + path + " exists? " + f.exists());
        def mols = MolStandardize.readStdNonisoSmilesFromSDFFile(path, null)

        then:
        mols.getMolecules().size() == 5
    }


}
