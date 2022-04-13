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
import org.squonk.fragnet.search.model.v2.Availability
import spock.lang.IgnoreIf

@IgnoreIf({!env.RDBASE})
class AvailabilityQuerySpec extends AbstractGraphDBSpec {

    void "simple search"() {

        Session session = graphDB.getSession()
        AvailabilityQuery query = new AvailabilityQuery(session)

        when:
        Availability a = query.getAvailability("N#Cc1cccc(N(CC(=O)O)C2CC2)n1")
        println "Found ${a.items.size()} items"

        then:
        a.items.size() > 0

        cleanup:
        session?.close()
    }
}
