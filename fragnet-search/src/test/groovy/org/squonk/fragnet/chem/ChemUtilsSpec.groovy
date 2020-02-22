/*
 * Copyright (c) 2020 Informatics Matters Ltd.
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

import spock.lang.Specification
import java.util.stream.Collectors

class ChemUtilsSpec extends Specification {

    void "read from text"() {
        def text = '''\
CCOc1ccccc1CN1CCC(O)CC1  1
COCC(=O)Nc1cccc(NC(C)=O)c1  2
'''
        when:
        def mols = ChemUtils.readSmilesData(text)
        def list = mols.collect(Collectors.toList())

        then:
        list.size() == 2
    }

    void "read from file"() {

        when:
        def mols = ChemUtils.readSmilesFile('src/test/data/mols.smi')
        def list = mols.collect(Collectors.toList())

        then:
        list.size() == 2
    }
}
