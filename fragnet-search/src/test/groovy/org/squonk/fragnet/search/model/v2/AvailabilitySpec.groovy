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
package org.squonk.fragnet.search.model.v2

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification

class AvailabilitySpec extends Specification {

    static final ObjectMapper mapper = new ObjectMapper()

    void "to and from json"() {

        def a = new Availability("smiles")
        a.addItem("supplier1", "code1", "smiles1")

        when:
        def json = mapper.writeValueAsString(a)
        println json
        def b = mapper.readValue(json, Availability.class)

        then:
        json != null
        b != null
        b.smiles == "smiles"
        b.items.size() == 1
        b.items[0].supplier == "supplier1"
        b.items[0].code == "code1"
        b.items[0].smiles == "smiles1"

    }
}
