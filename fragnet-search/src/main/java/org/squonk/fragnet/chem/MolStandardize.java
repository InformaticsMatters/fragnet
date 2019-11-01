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

import javax.validation.constraints.NotNull;
import java.util.logging.Logger;

public class MolStandardize {

    private static final Logger LOG = Logger.getLogger(MolStandardize.class.getName());


    public static String prepareMol(@NotNull String smiles, boolean includeStereo, boolean kekulise) {
        RWMol mol = RWMol.MolFromSmiles(smiles);
        if (mol == null) {
            throw new RuntimeException("Invalid molecule: " + smiles);
        }
        String canon = mol.MolToSmiles(includeStereo, kekulise);
        LOG.finer("Smiles: " + smiles + " Canon: " + canon);
        if (canon == null) {
            throw new RuntimeException("Unable to canonicalize molecule: " + smiles);
        }
        return canon;
    }
}
