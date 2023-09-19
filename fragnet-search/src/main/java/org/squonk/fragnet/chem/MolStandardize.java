/*
 * Copyright (c) 2023 Informatics Matters Ltd.
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

import org.RDKit.*;
import org.squonk.fragnet.Constants;
import org.squonk.fragnet.search.model.v2.ConvertedSmilesMols;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class MolStandardize {

    static {
        System.loadLibrary("GraphMolWrap");
    }

    private static final Logger LOG = Logger.getLogger(MolStandardize.class.getName());

    private static CleanupParameters DEFAULT_CLEANUP_PARAMS = RDKFuncs.getDefaultCleanupParameters();


    public static String prepareNonisoMol(@NotNull String molecule, @NotNull String mimeType) {

        RWMol mol;
        if (Constants.MIME_TYPE_SMILES.equals(mimeType)) {
            mol = RWMol.MolFromSmiles(molecule);
        } else if (Constants.MIME_TYPE_MOLFILE.equals(mimeType)) {
            LOG.fine("MOL: |" + molecule + "|");
            mol = RWMol.MolFromMolBlock(molecule, true);
        } else {
            throw new IllegalArgumentException("Unexpected molecule format: " + mimeType);
        }

        if (mol == null) {
            throw new RuntimeException("Invalid molecule: " + molecule);
        }

        mol = defaultStandardize(mol);
        if (mol == null) {
            throw new RuntimeException("Unable to standardize: " + molecule);
        }

        String canon = mol.MolToSmiles(false, false);
        if (canon == null) {
            throw new RuntimeException("Unable to generate canonical SMILES: " + mol);
        }
        LOG.finer("Canonical SMILES: " + canon);
        return canon;
    }


    /**
     * Create a molecule with charges neutralised (as far as possible)
     *
     * @param mol A clone of the mol with charges neutralised
     * @return
     */
    public static RWMol uncharge(RWMol mol, boolean standardize) {
        return RDKFuncs.chargeParent(mol, DEFAULT_CLEANUP_PARAMS, standardize);
    }

    /**
     * Create a molecule with all isotope information removed.
     * NOTE: this modifies the original molecules
     *
     * @param mol
     * @return The mol with all isotopes set to the default
     */
    public static void removeIsotopes(RWMol mol) {
        Atom_Vect vect = mol.getAtoms();
        for (int i = 0; i < vect.size(); i++) {
            Atom atom = vect.get(i);
            atom.setIsotope(0);
        }
    }

    /**
     * Create a molecule which has been cleaned up.
     *
     * @param mol
     * @return A clone of the mol which has been cleaned
     */
    public static RWMol cleanup(RWMol mol) {
        // Note that there is also a RDKFuncs.cleanUp() method that has a void return type.
        return RDKFuncs.cleanup(mol, DEFAULT_CLEANUP_PARAMS);
    }

    /**
     * Run default standardization needed for the fragment network
     *
     * @param mol The mol to standardize
     * @return The standardized mol
     */
    public static RWMol defaultStandardize(RWMol mol) {
        mol = cleanup(mol);
        mol = uncharge(mol, false);
        removeIsotopes(mol);
        return mol;
    }


    /**
     * Generate the non-isomeric SMILES needed for the fragment network
     *
     * @param mol
     * @param standardize Whether to standardize first.
     * @return
     */
    public static String nonisoSmiles(RWMol mol, boolean standardize) {
        if (standardize) {
            mol = defaultStandardize(mol);
        }
        String smiles = mol.MolToSmiles(false, false);
        return smiles;
    }

    /**
     * Generate the non-isomeric SMILES needed for the fragment network
     *
     * @param smiles
     * @param standardize Whether to standardize first.
     * @return
     */
    public static String nonisoSmiles(String smiles, boolean standardize) {
        RWMol mol = ChemUtils.molFromSmiles(smiles);
        if (mol == null) {
            return null;
        } else {
            return nonisoSmiles(mol, standardize);
        }
    }

    public static ConvertedSmilesMols readStdNonisoSmilesFromSDFFile(
            String file,
            String idPropName
    ) {
        SDMolSupplier molSupplier = new SDMolSupplier(file);
//        SDMolSupplier molSupplier = new SDMolSupplier(file, true, true);
        return readSDF(molSupplier, idPropName);
    }


    public static ConvertedSmilesMols readStdNonisoSmilesFromSDFData(
            String data,
            String idPropName) {

        SDMolSupplier molSupplier = new SDMolSupplier();
        molSupplier.setData(data, true, true);
        return readSDF(molSupplier, idPropName);
    }

    private static ConvertedSmilesMols readSDF(SDMolSupplier molSupplier, String idPropName) {

        int count = 0;
        ConvertedSmilesMols mols = new ConvertedSmilesMols(Constants.MIME_TYPE_MOLFILE);
        while (!molSupplier.atEnd()) {
            count++;
            ROMol mol = molSupplier.next();
            // TODO - this in ineficient and might change the mol
            String molBlock = mol.MolToMolBlock();

            mol.canonicalizeMol();

            String id = null;
            if (idPropName != null) {
                id = mol.getProp(idPropName);
            }
            if (id == null) {
                id = "" + count;
            }

            String smiles = nonisoSmiles(new RWMol(mol), true);
            mols.addMol(molBlock, smiles, id);
        }
        return mols;
    }

    public static ConvertedSmilesMols readStdNonisoSmilesFromSmilesData(String data) throws IOException {
        Stream<String> stream = Pattern.compile("\\r?\\n").splitAsStream(data);
        return readStdNonisoSmilesLines(stream);
    }

    public static ConvertedSmilesMols readStdNonisoSmilesFromSmilesFile(String file) throws IOException {
        Path path = FileSystems.getDefault().getPath(file);
        Stream<String> stream = Files.lines(path);
        return readStdNonisoSmilesLines(stream);
    }

    public static ConvertedSmilesMols readStdNonisoSmilesLines(Stream<String> lines) throws IOException {
        AtomicInteger count = new AtomicInteger(0);
        ConvertedSmilesMols mols = new ConvertedSmilesMols(Constants.MIME_TYPE_SMILES);
        lines.sequential().forEachOrdered((l) -> {
            count.incrementAndGet();
            l = l.trim();
            String[] tokens = l.split("\\s+");
            String smiles = nonisoSmiles(tokens[0], true);

            if (tokens.length > 1) {
                mols.addMol(tokens[0], smiles, tokens[1]);
            } else {
                mols.addMol(l, smiles, "" + count);
            }
        });
        return mols;
    }

    public static void main(String[] args) {

        String[][] inputs = {
                new String[]{"COC=1C=CC(NC=2N=CN=C3NC=NC23)=CC1", "COc1ccc(Nc2ncnc3[nH]cnc23)cc1"},
                new String[]{"O[C@@H]1CCN(CC=2C=CC(Cl)=CC2)C1", "OC1CCN(Cc2ccc(Cl)cc2)C1"},
                new String[]{"[Na+].[O-]C(=O)C=1C=CC=CC1OC(F)(F)F", "O=C(O)c1ccccc1OC(F)(F)F"},
                new String[]{"CNCCC1CCCCC1", "CNCCC1CCCCC1"},
                new String[]{"Cl.Cl.O=C(N1CCNCC1)C=2C=CN=CC2", "O=C(c1ccncc1)N1CCNCC1"},
                new String[]{"CC[C@H](CO)NC(=O)NC=1C=CC(Cl)=CC1", "CCC(CO)NC(=O)Nc1ccc(Cl)cc1"},
                new String[]{"CC(=O)N1CCC(CC1)C#N", "CC(=O)N1CCC(C#N)CC1"},
//                new String[]{""},
        };
        int errors = 0;
        int count = 0;
        for (int i = 0; i< inputs.length; i++) {
            count++;

            RWMol mol = RWMol.MolFromSmiles(inputs[i][0]);

            String smiles = nonisoSmiles(mol, true);

            if (!smiles.equals(inputs[i][1])) {
                System.out.println(String.format("Incorrect result for %s: expected %s found %s", inputs[i][0], inputs[i][1], smiles));
                errors++;
            }
        }

        System.out.println(count + " processed, " + errors + " errors");
    }
}
