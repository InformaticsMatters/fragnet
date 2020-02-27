package org.squonk.fragnet.chem;

import org.RDKit.RWMol;

public class ChemUtils {


    public static RWMol molFromSmiles(String smiles) {
        return RWMol.MolFromSmiles(smiles);
    }


}
