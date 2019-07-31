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

package org.squonk.fragnet.chem;

import org.RDKit.RWMol;

/** DO NOT USE.
 * This class does not work properly as the SWIG wrappers do not seem to work correctly.
 *
*/
public class Depict {

    static {
        System.loadLibrary("GraphMolWrap");
    }

    public static String smilesToSVG(String smiles) {
        return smilesToSVG(smiles, true);
    }

    public static String smilesToSVG(String smiles, boolean kekulize) {
        RWMol mol = RWMol.MolFromSmiles(smiles);
        if (kekulize) {
            mol.Kekulize();
        }
        mol.compute2DCoords();
        String svg = mol.ToSVG();
        return svg;
    }
}
