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

import org.openscience.cdk.renderer.color.CDK2DAtomColors
import spock.lang.Specification

import java.awt.Color


class CdkMolDepictServletSpec extends Specification {

    void "smiles2svg"() {


        CdkMolDepictServlet servlet = new CdkMolDepictServlet()

        when:
        def mol = servlet.smilesToIAtomContainer("C1=CC=CC=C1N")
        def svg = servlet.moleculeToSVG(mol, 50, 50, 2,
                new CDK2DAtomColors(), Color.WHITE, true)
        println svg

        then:
        svg != null
        svg.length() > 0
        svg.contains("width='50.0'")
        svg.contains("height='50.0'")
    }
}
