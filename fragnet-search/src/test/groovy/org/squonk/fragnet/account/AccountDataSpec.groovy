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
package org.squonk.fragnet.account

import spock.lang.Specification

class AccountDataSpec extends Specification {

    void "test read file no prune"() {

        when:
        def input = new BufferedReader(new FileReader('src/test/data/fragnet-queries-v2'))
        def account = new AccountData()
        account.readLogfile(input)
        def tdudgeon = account.getQueryCount('tdudgeon', false)
        def odudgeon = account.getQueryCount('odudgeon', false)
        def pbrunclik = account.getQueryCount('pbrunclik', false)
        def nobody = account.getQueryCount('nobody', false)

        then:
        tdudgeon == 30
        odudgeon == 85
        pbrunclik == 1
        nobody == 0
    }

    void "test read file prune"() {

        when:
        def input = new BufferedReader(new FileReader('src/test/data/fragnet-queries-v2'))
        def account = new AccountData()
        account.readLogfile(input)
        account.incrementQueryCount('tdudgeon')
        account.incrementQueryCount('tdudgeon')
        account.incrementQueryCount('odudgeon')
        def tdudgeon = account.getQueryCount('tdudgeon')
        def odudgeon = account.getQueryCount('odudgeon')
        def pbrunclik = account.getQueryCount('pbrunclik')
        def nobody = account.getQueryCount('nobody')

        then:
        tdudgeon == 2
        odudgeon == 1
        pbrunclik == 0
        nobody == 0
    }
}
