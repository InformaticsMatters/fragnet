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
package org.squonk.fragnet.search.queries.v2

import org.neo4j.driver.v1.Session
import org.squonk.fragnet.AbstractGraphDBSpec
import org.squonk.fragnet.Constants
import org.squonk.fragnet.search.model.v2.ConvertedSmilesMols

class HitExpanderSpec extends AbstractGraphDBSpec {

    static {
        Runtime.getRuntime().loadLibrary0(groovy.lang.GroovyClassLoader.class, "GraphMolWrap")
    }

    void "simple expand two"() {

        Session session = graphDB.getSession()

        HitExpander hitExpander = new HitExpander(session)
        ConvertedSmilesMols mols = new ConvertedSmilesMols(Constants.MIME_TYPE_SMILES)
        mols.addMol(null, "CCOc1ccccc1CN1CCC(O)CC1", "1")
        mols.addMol(null, "COCC(=O)Nc1cccc(NC(C)=O)c1", "2")

        when:
        def results = hitExpander.processMolecules(mols, 2, 5, 5, 2, 2, null)
//        println "Found ${results.getResultCount()} items"

        then:
        results.getResultCount() > 0

        cleanup:
        session?.close()
    }

}
