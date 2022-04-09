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
package org.squonk.fragnet.search.queries.v2

import org.neo4j.driver.v1.Session
import org.squonk.fragnet.AbstractGraphDBSpec
import org.squonk.fragnet.search.model.v2.ExpansionResults
import spock.lang.IgnoreIf

@IgnoreIf({!env.RDBASE})
class ExpansionQuerySpec extends AbstractGraphDBSpec {

    static {
        Runtime.getRuntime().loadLibrary0(groovy.lang.GroovyClassLoader.class, "GraphMolWrap")
    }

    void "simple search"() {
        Session session = graphDB.getSession()
        ExpansionQuery query = new ExpansionQuery(session, null)

        when:
        ExpansionResults results = query.executeQuery("Cc1cc(Cl)c(C(=O)CN)s1", "chemical/x-daylight-smiles",
                1, 10, 10, 3, 3, null)
        println "Found ${results.getSize()} items"

        then:
        results.getSize() > 0

        cleanup:
        session?.close()
    }

}
