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

import org.RDKit.*;
import org.squonk.fragnet.Constants;
import org.squonk.fragnet.search.FragmentUtils;
import org.squonk.fragnet.search.model.v2.GroupingType;
import org.squonk.fragnet.search.model.v2.MolTransform;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

class TransformClassifier2Hops implements Constants {

    private static final Logger LOG = Logger.getLogger(TransformClassifier2Hops.class.getName());

    private final String fromSmiles;
    private final String edge1;
    private final String[] parts1;
    boolean isAddition1;
    private final String midSmiles;
    private final String edge2;
    private final String[] parts2;
    boolean isAddition2;
    private final String toSmiles;
    private final int numMiddleComponents;

    private RWMol mol0 = null; // fromSmiles
    private RWMol mol1 = null; // parts1[4]
    private RWMol mol2 = null; // midSmiles
    private RWMol mol3 = null; // parts2[4]
    private RWMol mol4 = null; // toSmiles
    private MCSResult mcs123 = null;
    private RWMol mcs123Mol = null;
    private MCSResult mcs12 = null;
    private RWMol mcs12Mol = null;
    private MCSResult mcs32 = null;
    private RWMol mcs32Mol = null;
    private MCSResult mcs04 = null;
    private RWMol mcs04Mol = null;
    private Match_Vect_Vect match1 = null;
    private Match_Vect_Vect match2 = null;
    private Match_Vect_Vect match3 = null;
    private Match_Vect_Vect match12 = null;
    private Match_Vect_Vect match32 = null;
    private Match_Vect_Vect match0vs04 = null;
    private Match_Vect_Vect match4vs04 = null;

    TransformClassifier2Hops(
            String fromSmiles,
            String edge1,
            boolean isAddition1,
            String midSmiles,
            String edge2,
            boolean isAddition2,
            String toSmiles) {
        this.fromSmiles = fromSmiles;
        this.edge1 = edge1;
        this.parts1 = FragmentUtils.splitLabel(edge1);
        this.isAddition1 = isAddition1;
        this.midSmiles = midSmiles;
        this.edge2 = edge2;
        this.parts2 = FragmentUtils.splitLabel(edge2);
        this.isAddition2 = isAddition2;
        this.toSmiles = toSmiles;
        // count the dots in the middle smiles to get the number of disconnected mols
        this.numMiddleComponents = midSmiles.length() - midSmiles.replace(".", "").length() + 1;
    }

    RWMol getMol0() {
        if (mol0 == null) {
            mol0 = RWMol.MolFromSmiles(fromSmiles);
        }
        return mol0;
    }

    RWMol getMol1() {
        if (mol1 == null) {
            mol1 = RWMol.MolFromSmiles(parts1[4]);
        }
        return mol1;
    }

    RWMol getMol2() {
        if (mol2 == null) {
            mol2 = RWMol.MolFromSmiles(midSmiles);
        }
        return mol2;
    }

    RWMol getMol3() {
        if (mol3 == null) {
            mol3 = RWMol.MolFromSmiles(parts2[4]);
        }
        return mol3;
    }

    RWMol getMol4() {
        if (mol4 == null) {
            mol4 = RWMol.MolFromSmiles(toSmiles);
        }
        return mol4;
    }

    long generateMCS123() {
        // generate MCS for all 3 mols
        mcs123 = generateMCS(getMol1(), getMol2(), getMol3());
        return mcs123.getNumAtoms();
    }

    long generateMCS12() {
        // generate MCS for all mols 1 and 2
        mcs12 = generateMCS(getMol1(), getMol2());
        return mcs12.getNumAtoms();
    }

    long generateMCS32() {
        // generate MCS for all mols 3 and 2
        mcs32 = generateMCS(getMol3(), getMol2());
        return mcs32.getNumAtoms();
    }

    long generateMCS04() {
        // generate MCS for start and end mols
        mcs04 = generateMCS(getMol0(), getMol4());
        return mcs04.getNumAtoms();
    }

    MCSResult generateMCS(RWMol... mols) {
        ROMol_Vect mcsmols = new ROMol_Vect();
        for (RWMol mol : mols) {
            mcsmols.add(mol);
        }

        // generate MCS for the mols
        mcs123 = RDKFuncs.findMCS(mcsmols);
        LOG.info("MCS SMARTS = " + mcs123.getSmartsString());
        return mcs123;
    }


    String getMCS123AsSmarts() {
        if (mcs123 == null) {
            generateMCS123();
        }
        return mcs123.getSmartsString();
    }

    RWMol getMCS123AsMol() {
        if (mcs123Mol == null) {
            mcs123Mol = RWMol.MolFromSmarts(getMCS123AsSmarts());
        }
        return mcs123Mol;
    }

    String getMCS12AsSmarts() {
        if (mcs12 == null) {
            generateMCS12();
        }
        return mcs12.getSmartsString();
    }

    RWMol getMCS12AsMol() {
        if (mcs12Mol == null) {
            mcs12Mol = RWMol.MolFromSmarts(getMCS12AsSmarts());
        }
        return mcs12Mol;
    }

    String getMCS32AsSmarts() {
        if (mcs32 == null) {
            generateMCS32();
        }
        return mcs32.getSmartsString();
    }

    RWMol getMCS32AsMol() {
        if (mcs32Mol == null) {
            mcs32Mol = RWMol.MolFromSmarts(getMCS32AsSmarts());
        }
        return mcs32Mol;
    }

    String getMCS04AsSmarts() {
        if (mcs04 == null) {
            generateMCS04();
        }
        return mcs04.getSmartsString();
    }

    RWMol getMCS04AsMol() {
        if (mcs04Mol == null) {
            mcs04Mol = RWMol.MolFromSmarts(getMCS04AsSmarts());
        }
        return mcs04Mol;
    }

    Match_Vect_Vect getMatches1vs123() {
        if (match1 == null) {
            match1 = getMatches(getMol1(), getMCS123AsMol());
        }
        return match1;
    }

    Match_Vect_Vect getMatches2vs123() {
        if (match2 == null) {
            match2 = getMatches(getMol2(), getMCS123AsMol());
        }
        return match2;
    }

    Match_Vect_Vect getMatches3vs123() {
        if (match3 == null) {
            match3 = getMatches(getMol3(), getMCS123AsMol());
        }
        return match3;
    }

    Match_Vect_Vect getMatches1vs2() {
        if (match12 == null) {
            match12 = getMatches(getMol1(), getMCS12AsMol());
        }
        return match1;
    }

    Match_Vect_Vect getMatches3vs2() {
        if (match32 == null) {
            match32 = getMatches(getMol3(), getMCS32AsMol());
        }
        return match32;
    }

    Match_Vect_Vect getMatches0vs04() {
        if (match0vs04 == null) {
            match0vs04 = getMatches(getMol0(), getMCS04AsMol());
        }
        return match0vs04;
    }

    Match_Vect_Vect getMatches4vs04() {
        if (match4vs04 == null) {
            match4vs04 = getMatches(getMol4(), getMCS04AsMol());
        }
        return match4vs04;
    }

    private Match_Vect_Vect getMatches(RWMol mol, RWMol smarts) {
        Match_Vect_Vect matches = mol.getSubstructMatches(smarts);
        LOG.info("Num matches: " + matches.size());
        return matches;
    }

    static Atom[] findFirstXenonAndConnectedAtom(RWMol mol) {
        // find the Xe atom
        Atom xe = findFirstAtomOccurence(mol, 54);
        if (xe == null) {
            return null;
        } else {
            // get the atom the Xe is attached to
            Atom attached = xe.getBonds().get(0).getOtherAtom(xe);
            LOG.fine("Xe is " + xe.getSymbol() + xe.getIdx() + "-" + attached.getSymbol() + attached.getIdx());
            return new Atom[]{xe, attached};
        }
    }

    private GroupingType generateGroupingType(boolean isSubstitution) {
        return TransformClassifierUtils.createGroupingType(
                new boolean[]{isAddition1, isAddition2},
                new String[][]{parts1, parts2}, numMiddleComponents, isSubstitution);
    }

    private MolTransform generateMolTransform(String scaffold, boolean isSubstitution) {
        GroupingType type = generateGroupingType(isSubstitution);
        String s = (scaffold == null ? TransformClassifierUtils.createUndefinedScaffold(type): scaffold);
        return generateMolTransform(s, type);
    }

    private MolTransform generateMolTransform(String scaffold, GroupingType type) {
        String s = (scaffold == null ? TransformClassifierUtils.createUndefinedScaffold(type): scaffold);
        return new MolTransform(s, type, 2);
    }

    MolTransform classifyTransform() {
        if (isAddition1 && isAddition2) {

            return classifyTransformUsingStartAndEndMCS();

        } else if (!isAddition1 && !isAddition2) {
            // 2 deletions to the scaffold is the final mol
            return new MolTransform(toSmiles, GroupingType.DELETIONS, 2);
        }

        // so we need to use MCS to define this
        return classifyTransformUsingMidSmilesMCS();
    }

    MolTransform classifyTransformUsingStartAndEndMCS() {

        if (isAddition1 && isAddition2) {
            Match_Vect_Vect matches = getMatches4vs04();
            RWMol mol = RWMol.MolFromSmiles(toSmiles);
            AttachmentInfo info = removeNonMappedAtomsWithAttachmentInfo(mol, matches.get(0));
            LOG.info("Number of attachment points = " + info.attachmentPoints.size());
            for (Atom atom : info.attachmentPoints) {
                addXenonAtom(mol, atom);
            }
            String scaffold = mol.MolToSmiles();
            MolTransform tx = generateMolTransform(scaffold, false);
            return tx;
        } else {
            throw new IllegalArgumentException("Only additions are supported");
        }
    }

    MolTransform classifyTransformUsingMidSmilesMCS() {

        log(Level.INFO, "Input.");

        // find the locations on the Xenon atoms and their attachments. First item in array is Xe, second is atom it is attached to
        Atom[] mol1Atoms = findFirstXenonAndConnectedAtom(getMol1());
        Atom[] mol3Atoms = findFirstXenonAndConnectedAtom(getMol3());

        int attachment1InSmarts = mol1Atoms == null ? -1 : findAtomIndexInMatch((int) mol1Atoms[1].getIdx(), getMatches1vs123().get(0));
        int attachment3InSmarts = mol3Atoms == null ? -1 : findAtomIndexInMatch((int) mol3Atoms[1].getIdx(), getMatches3vs123().get(0));

        // We're going to modify this mol so will work with a copy
        RWMol middleMol = RWMol.MolFromSmiles(midSmiles);

        Atom mol1AttachmentInMol2 = null;
        Atom mol3AttachmentInMol2 = null;
        if (attachment1InSmarts >= 0) {
            mol1AttachmentInMol2 = findMappedAtom(mol1Atoms[1], getMol1(), middleMol, getMatches1vs123().get(0), getMatches2vs123().get(0));
        }
        if (attachment3InSmarts >= 0) {
            mol3AttachmentInMol2 = findMappedAtom(mol3Atoms[1], getMol3(), middleMol, getMatches3vs123().get(0), getMatches2vs123().get(0));
        }

        LOG.info("Attached atoms in scaffold are " +
                (mol1AttachmentInMol2 == null ? "none" : mol1AttachmentInMol2.getSymbol() + mol1AttachmentInMol2.getIdx()) +
                " and " +
                (mol3AttachmentInMol2 == null ? "none" : mol3AttachmentInMol2.getSymbol() + mol3AttachmentInMol2.getIdx()));

        GroupingType type = null;
        String scaffold = null;

        if (mol1AttachmentInMol2 == null && mol3AttachmentInMol2 == null) {
            log(Level.WARNING, "Neither attachment mapped. This is an error");
        } else if (mol1AttachmentInMol2 != null && mol3AttachmentInMol2 != null) {
            // remove atoms not in the scaffold
            LOG.info("Before removal " + middleMol.MolToSmiles());
            int numRemoved = removeNonMappedAtoms(middleMol, getMatches2vs123().get(0));
            LOG.info("After removal " + middleMol.MolToSmiles() + SPACE + numRemoved + " atoms removed");

            if (mol1AttachmentInMol2 == null) {
                String msg = "Attached atom not mapped in mol1";
                log(Level.WARNING, msg);
                throw new RuntimeException(msg);
            } else if (isAddition1) {
                LOG.info("Adding Xe to atom " + mol1AttachmentInMol2.getSymbol() + mol1AttachmentInMol2.getIdx());
                addXenonAtom(middleMol, mol1AttachmentInMol2);
            }

            if (mol3AttachmentInMol2 == null) {
                String msg = "Attached atom not mapped in mol3";
                log(Level.WARNING, msg);
                throw new RuntimeException(msg);
            } else if (isAddition2) {
                LOG.info("Adding Xe to atom " + mol3AttachmentInMol2.getSymbol() + mol3AttachmentInMol2.getIdx());
                addXenonAtom(middleMol, mol3AttachmentInMol2);
            }

            if (mol1AttachmentInMol2.getIdx() == mol3AttachmentInMol2.getIdx()) {
                type = TransformClassifierUtils.createGroupingType(
                        new boolean[]{isAddition1, isAddition2},
                        new String[][] {parts1, parts2},
                        numMiddleComponents, true);
            }

            if (!isAddition1 && !isAddition2) { // double deletion
                scaffold = toSmiles;
            } else if (!isAddition1 && isAddition2) { // deletion + addition
                scaffold = parts2[4];
            } else { // addition + deletion or addition + addition
                scaffold = middleMol.MolToSmiles();
            }
        } else if (mol1AttachmentInMol2 != null && mol3AttachmentInMol2 == null) {
            if (isAddition1 && isAddition2) {
                // then we are in the situation where the second addition adds to a position created by the first addition
                scaffold = parts1[4];
            }
        }

        if (type == null) {
            type = TransformClassifierUtils.createGroupingType(
                    new boolean[]{isAddition1, isAddition2},
                    new String[][] {parts1, parts2},
                    numMiddleComponents, false);
        }

        MolTransform tx = generateMolTransform(scaffold, type);
        return tx;
    }

    private static int findAtomIndexInMatch(int matchIndex, Match_Vect match) {
        for (int i1 = 0; i1 < match.size(); i1++) {
            Int_Pair atomPair = match.get(i1);
            if (matchIndex == atomPair.getSecond()) {
                return atomPair.getFirst();
            }
        }
        return -1;
    }

    private static Atom findMappedAtom(Atom atom, ROMol mol1, ROMol mol2, Match_Vect match1, Match_Vect match2) {
        int atomIndex = (int) atom.getIdx();
        int smartsAtom = -1;
        Atom mappedAtom = null;
        // find the atom in the smarts that corresponds to the atom
        for (int i = 0; i < match1.size(); i++) {
            Int_Pair atomPair = match1.get(i);
            if (atomIndex == atomPair.getSecond()) {
                smartsAtom = atomPair.getFirst();
                break;
            }
        }
        LOG.fine("SmartsAtom: " + smartsAtom);
        // for that atom in the smarts find the corresponding atom in the second mapping
        if (smartsAtom >= 0) {
            for (int i = 0; i < match2.size(); i++) {
                Int_Pair atomPair = match2.get(i);
                if (smartsAtom == atomPair.getFirst()) {
                    mappedAtom = mol2.getAtomWithIdx((long) atomPair.getSecond());
                    LOG.fine("Mapped Atom for " + atomPair.getFirst() + "->" + atomPair.getSecond() + " is " + mappedAtom.getSymbol() + mappedAtom.getIdx());
                    break;
                }
            }
        }
        return mappedAtom;
    }

    private void log(Level level, String msg) {
        if (LOG.isLoggable(level)) {
            StringBuilder b = new StringBuilder(msg)
                    .append(" Data: ")
                    .append(fromSmiles).append(SPACE)
                    .append(edge1).append(SPACE)
                    .append(isAddition1).append(SPACE)
                    .append(midSmiles).append(SPACE)
                    .append(edge2).append(SPACE)
                    .append(isAddition2).append(SPACE)
                    .append(toSmiles);

            LOG.log(level, b.toString());
        }
    }

    static int removeNonMappedAtoms(RWMol mol, Match_Vect match) {
        Atom_Vect atoms = mol.getAtoms();
        List<Atom> atomsToRemove = new ArrayList<>();
        for (int i = 0; i < atoms.size(); i++) {
            Atom atom = atoms.get(i);
            if (!isAtomInMatch(i, match)) {
                atomsToRemove.add(atoms.get(i));
            }
        }

        for (int i = atomsToRemove.size() - 1; i >= 0; i--) {
            Atom a = atomsToRemove.get(i);
            LOG.info("Deleting atom " + a.getSymbol() + a.getIdx());
            mol.removeAtom(a);
        }
        return atomsToRemove.size();
    }

    static AttachmentInfo removeNonMappedAtomsWithAttachmentInfo(RWMol mol, Match_Vect match) {
        AttachmentInfo info = new AttachmentInfo();
        Atom_Vect atoms = mol.getAtoms();
        List<Atom> atomsToRemove = new ArrayList<>();
        for (int i = 0; i < atoms.size(); i++) {
            if (!isAtomInMatch(i, match)) {
                Atom atom = atoms.get(i);
                atomsToRemove.add(atom);
                int attachmentAtomIdx = findConnectedAtomInMatch(mol, atom, match);
                if (attachmentAtomIdx > -0) {
                    info.attachmentPoints.add(mol.getAtomWithIdx(attachmentAtomIdx));
                }
            }
        }

        for (int i = atomsToRemove.size() - 1; i >= 0; i--) {
            Atom a = atomsToRemove.get(i);
            LOG.info("Deleting atom " + a.getSymbol() + a.getIdx());
            mol.removeAtom(a);
        }
        info.numAtomsDeleted = atomsToRemove.size();
        return info;
    }

    static int findConnectedAtomInMatch(RWMol mol, Atom atom, Match_Vect match) {
        Bond_Vect bonds = atom.getBonds();
        for (int i = 0; i < bonds.size(); i++) {
            Bond bond = bonds.get(i);
            Atom other = bond.getOtherAtom(atom);
            if (isAtomInMatch((int) other.getIdx(), match)) {
                return (int) other.getIdx();
            }
        }
        return -1;
    }

    static boolean isAtomInMatch(int atomNo, Match_Vect match) {
        for (int j = 0; j < match.size(); j++) {
            Int_Pair atomPair = match.get(j);
            if (atomPair.getSecond() == atomNo) {
                return true;
            }
        }
        return false;
    }

    static void addXenonAtom(RWMol mol, Atom atomToAttachTo) {
        Atom xe = new Atom(54);
        // setNoImplicit(true) is needed otherwise the Xenon is assume to have 2 bonds and gets a hydrogen added to it.
        xe.setNoImplicit(true);
        long index = mol.addAtom(xe, false);
        mol.addBond(atomToAttachTo.getIdx(), index, Bond.BondType.SINGLE);
    }

    static void addAnyAtom(RWMol mol, Atom atomToAttachTo) {
        RWMol m = RWMol.MolFromSmarts("*");
        Atom any = m.getAtomWithIdx(0);
        long index = mol.addAtom(any, false);
        mol.addBond(atomToAttachTo.getIdx(), index, Bond.BondType.SINGLE);

    }

    static Atom findFirstAtomOccurence(ROMol mol, int atomicNo) {
        Atom_Vect vect = mol.getAtoms();
        for (int i = 0; i < vect.size(); i++) {
            Atom atom = vect.get(i);
            if (atom.getAtomicNum() == atomicNo) {
                return atom;
            }
        }
        return null;
    }

    private static String dumpMatch_Vect(Match_Vect vect) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < vect.size(); i++) {
            if (i > 0) {
                buf.append(SPACE);
            }
            Int_Pair pair = vect.get(i);
            buf.append(pair.getFirst()).append(":").append(pair.getSecond());
        }
        return buf.toString();
    }

    static class AttachmentInfo {
        int numAtomsDeleted = 0;
        List<Atom> attachmentPoints = new ArrayList<>();
        List<Boolean> attachmentIsRing = new ArrayList<>();
    }
}
