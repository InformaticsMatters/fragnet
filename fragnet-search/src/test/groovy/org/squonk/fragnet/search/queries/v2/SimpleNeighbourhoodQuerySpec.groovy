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

import org.neo4j.driver.Session
import org.squonk.fragnet.AbstractGraphDBSpec
import org.squonk.fragnet.search.model.v2.NeighbourhoodGraph
import org.squonk.fragnet.service.GraphDB
import spock.lang.IgnoreIf

@IgnoreIf({!env.RDBASE})
class SimpleNeighbourhoodQuerySpec extends AbstractGraphDBSpec {

    static {
        Runtime.getRuntime().loadLibrary0(groovy.lang.GroovyClassLoader.class, "GraphMolWrap")
    }


    void "1-hop neighbourhood query"() {
        GraphDB db = new GraphDB()
        Session session = db.getSession()

        when:
        NeighbourhoodQuery query = new NeighbourhoodQuery(session, [:])
        NeighbourhoodGraph result = query.executeNeighbourhoodQuery('Brc1cnc(N2CCCC2)nc1', 1, 3, 1, null, 10)
        print "Results: $result.nodeCount"

        then:
        result.nodeCount > 0
    }

}
