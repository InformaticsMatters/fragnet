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

import org.RDKit.ExplicitBitVect;
import org.RDKit.RDKFuncs;
import org.RDKit.RWMol;
import org.RDKit.SparseIntVectu32;

import javax.validation.constraints.NotNull;

public class Calculator {

    public enum Calculation {
        MW("molw", "Mol weight", "number"),
        LOGP("logp", "cLogP", "number"),
        ROTB("rotb", "RotBonds", "integer"),
        HBD("hbd", "HBond donors", "integer"),
        HBA("hba", "HBond acceptors", "integer"),
        TPSA("tpsa", "TPSA", "number"),
        SIM_RDKIT_TANIMOTO("sim_rdkit_tanimoto", "Tanimoto sim (RDKit)", "number"),
        SIM_MORGAN2_TANIMOTO("sim_morgan2_tanimoto", "Tanimoto sim (Morgan2)", "number"),
        SIM_MORGAN3_TANIMOTO("sim_morgan3_tanimoto", "Tanimoto sim (Morgan3)", "number");

        public final String propname;
        public final String description;
        public final String type;


        Calculation(String propname, String description, String type) {
            this.propname = propname;
            this.description = description;
            this.type = type;
        }
    }

    public static RWMol createMol(@NotNull String smiles) {
        return RWMol.MolFromSmiles(smiles);
    }

    public static Float calcLogP(@NotNull RWMol mol) {
        return cooerceFloat(RDKFuncs.calcMolLogP(mol));
    }

    public static Float calcMolWeight(@NotNull RWMol mol) {
        return cooerceFloat(RDKFuncs.calcExactMW(mol));
    }

    public static Integer calcHydrogenBondDonors(@NotNull RWMol mol) {
        return (int)RDKFuncs.calcNumHBD(mol);
    }

    public static Integer calcHydrogenBondAcceptors(@NotNull RWMol mol) {
        return (int)RDKFuncs.calcNumHBA(mol);
    }

    public static Float calcTPSA(@NotNull RWMol mol) {
        return cooerceFloat(RDKFuncs.calcTPSA(mol));
    }

    public static Integer calcRotatableBonds(@NotNull RWMol mol) {
        return (int)RDKFuncs.calcNumRotatableBonds(mol);
    }

    public static ExplicitBitVect calcRDKitFingerprint(@NotNull RWMol mol) {
        return RDKFuncs.RDKFingerprintMol(mol);
    }

    public static SparseIntVectu32 calcMorganFingerprint(@NotNull RWMol mol, int radius) {
        return RDKFuncs.MorganFingerprintMol(mol, (long)radius);
    }

    public static Float calcTanimotoSimilarity(@NotNull ExplicitBitVect bv1, @NotNull ExplicitBitVect bv2) {
        return cooerceFloat(RDKFuncs.TanimotoSimilarity(bv1, bv2));
    }

    public static Float calcTanimotoSimilarity(@NotNull SparseIntVectu32 siv1, @NotNull SparseIntVectu32 siv2) {
        return cooerceFloat(RDKFuncs.TanimotoSimilaritySIVu32(siv1, siv2));
    }

//AllBitSimilarity(ExplicitBitVect bv1, ExplicitBitVect bv2)


    private static Float cooerceFloat(Number n) {
        if (n == null) {
            return null;
        } else {
            return n.floatValue();
        }
    }

}
