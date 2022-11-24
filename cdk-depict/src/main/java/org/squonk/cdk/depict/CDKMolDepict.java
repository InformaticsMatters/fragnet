/*
 * Copyright (c) 2022 Informatics Matters Ltd.
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

package org.squonk.cdk.depict;

import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.depict.Depiction;
import org.openscience.cdk.depict.DepictionGenerator;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.renderer.SymbolVisibility;
import org.openscience.cdk.renderer.color.*;
import org.openscience.cdk.renderer.generators.standard.StandardGenerator;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mol depiction using CDK. Can be used to generate SVG or image files.
 * <p>
 * This uses the CDK DepictionGenerator class
 * (http://cdk.github.io/cdk/latest/docs/api/org/openscience/cdk/depict/DepictionGenerator.html)
 * to do the heavy lifting.
 * <p>
 * Options include:
 * <ul>
 * <li>image width, height and margin</li>
 * <li>various atom colour schemes</li>
 * <li>specify background colour (including transparency)</li>
 * <li>expand molecule to fit the available space</li>
 * <li>highlight a substructure (defined as MSC)</li>
 * <li>highlight user specified atoms</li>
 * <li>show atom labels</li>
 * </ul>
 * <p>
 * See the depict() methods for more details. The Depiction object returned by those methods can then be converted
 * to several graphic formats. See the tests for examples.
 * <p>
 * Created by timbo on 01/08/2019.
 */

public class CDKMolDepict {

    private static final Logger LOG = Logger.getLogger(CDKMolDepict.class.getName());

    private static final Map<String, IAtomColorer> COLORERS = new HashMap<>();

    static {
        COLORERS.put("jmol", new JmolColors());
        COLORERS.put("cdk2d", new CDK2DAtomColors());
        COLORERS.put("partialAtomicCharge", new PartialAtomicChargeColors());
        COLORERS.put("rasmol", new RasmolColors());
        COLORERS.put("white", new UniColor(Color.WHITE));
        COLORERS.put("black", new UniColor(Color.BLACK));
    }

    public static final IAtomColorer DEFAULT_COLORER = COLORERS.get("cdk2d");
    public static final Color DEFAULT_BACKGROUND = new Color(255, 255, 255, 0);
    public static final int DEFAULT_SIZE = 50;

    private final DepictionGenerator generator;
    private boolean showOnlyExplicitH = false;
    private IAtomContainer alignTo;
    private Color mcsColor = null;
    private final boolean removeStereo;

    private final String titleFieldName;

    /**
     * Create with default parameters
     */
    public CDKMolDepict() {
        this(
                250,
                250,
                null,
                null,
                Color.WHITE,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    /** Minimal set of params used in tests
     *
     * @param outerGlow
     */
    protected CDKMolDepict(Boolean outerGlow, Boolean removeStereo) {
        this(
                250,
                250,
                null,
                DEFAULT_COLORER,
                Color.WHITE,
                null,
                null,
                null,
                outerGlow,
                null,
                null,
                true,
                removeStereo,
                null,
                null);

    }

    /**
     * Constructor allows to define custom parameters.
     */
    public CDKMolDepict(Integer width,
                        Integer height,
                        Double margin,
                        IAtomColorer colorScheme,
                        Color backgroundColor,
                        Color annotationColor,
                        String titleField,
                        Color titleColor,
                        Boolean outerGlow,
                        Double annotationScale,
                        Double titleScale,
                        Boolean expandToFit,
                        Boolean removeStereo,
                        Double padding,
                        Map<Class, Object> extraParams) {

        DepictionGenerator dg = new DepictionGenerator()
                .withTerminalCarbons()
                .withBackgroundColor(backgroundColor == null ? DEFAULT_BACKGROUND : backgroundColor)
                .withAnnotationColor(annotationColor == null ? Color.BLACK : annotationColor)
                .withAnnotationScale(annotationScale == null ? 1d : annotationScale)
                .withTitleColor(titleColor == null ? Color.BLACK : titleColor)
                .withTitleScale(titleScale == null ? 1.0 : titleScale)
                .withSize(width == null ? DEFAULT_SIZE : width, height == null ? DEFAULT_SIZE : height)
                .withAtomColors(colorScheme == null ? DEFAULT_COLORER : colorScheme)
                .withMargin(margin == null ? 0d : margin)
                .withAtomValues()
                .withPadding(padding == null ? 1d : padding)
                .withParam(StandardGenerator.Visibility.class,
                        new SymbolVisibility() {
                            @Override
                            public boolean visible(IAtom atom, List<IBond> neighbors, RendererModel model) {
                                if (atom.getAtomicNumber() == 6) {
                                    // count the number of connections that are heavy atoms
                                    int numHeavy = countHeavyAtomConnections(atom);
                                    // if only one then this is a terminal carbon so we need to leave the Hs in place
                                    return numHeavy < 2;
                                } else { // non-carbon atoms
                                    return true;
                                }
                            }
                        });

        this.removeStereo = removeStereo == null ? false : removeStereo.booleanValue();
        this.titleFieldName = titleField;

        if (expandToFit == null || expandToFit.booleanValue()) {
            dg = dg.withFillToFit();
        }
        if (outerGlow != null && outerGlow) {
            dg = dg.withOuterGlowHighlight();
        }
        if (extraParams != null) {
            for (Class key : extraParams.keySet()) {
                dg = dg.withParam(key, extraParams.get(key));
            }
        }

        this.generator = dg;
    }

    public boolean isShowOnlyExplicitH() {
        return showOnlyExplicitH;
    }

    /**
     * Set the depiction to show only hydrogens that are explicitly defined.
     * e.g. do not display implicit hydrogens.
     *
     * @param showOnlyExplicitH
     */
    public void setShowOnlyExplicitH(boolean showOnlyExplicitH) {
        this.showOnlyExplicitH = showOnlyExplicitH;
    }

    public IAtomContainer getAlignTo() {
        return alignTo;
    }

    public Color getMcsColor() {
        return mcsColor;
    }

    /**
     * Set a molecule to align the rendered molecules to (using MCS).
     * If the molecule does not contain 2D coordinates these are generated which will modify the molecule that is
     * specified as input.
     *
     * @param alignTo  substructure to align to using MCS. Can be null.
     * @param mcsColor Color used to highlight the aligned MCS.
     *                 If this value is null the molecules are aligned, but the common MCS is not highlighted.
     */
    public void setMCSAlignment(IAtomContainer alignTo, Color mcsColor) throws CDKException {
        this.mcsColor = mcsColor;
        if (alignTo != null) {
            ChemUtils.prepareForMCS(alignTo);
            ChemUtils.layoutMoleculeIn2D(alignTo, false);
        }
        this.alignTo = alignTo;
    }

    /**
     * Set the color used to highlight the aligned MCS.
     * If this value is null the molecules are aligned, but the common MCS is not highlighted.
     *
     * @param mcsColor
     */
    public void setMcsColor(Color mcsColor) {
        this.mcsColor = mcsColor;
    }


    /**
     * Get the supported colorers. See the COLORERS Map for the values.
     *
     * @param name
     * @return
     */
    public static IAtomColorer getColorer(String name) {
        return COLORERS.get(name);
    }

    public String getTitleField() {
        return this.titleFieldName;
    }

    public Depiction depict(IAtomContainer mol, Color highlightColor, List<Integer> atomHighlights) throws CDKException, CloneNotSupportedException {
        Map<Color, List<List<Integer>>> map = (highlightColor == null || atomHighlights == null ? null :
                Collections.singletonMap(highlightColor, Collections.singletonList(atomHighlights)));
        return depict(Collections.singletonList(mol), map);
    }

    public Depiction depict(IAtomContainer mol) throws CDKException, CloneNotSupportedException {
        return depict(Collections.singletonList(mol), null);
    }

    public Depiction depict(List<IAtomContainer> mols) throws CDKException, CloneNotSupportedException {
        return depict(mols, null);
    }

    /** Generate a depiction of the molecule with the specified highlights.
     * The Highlights are supplied as a Map, the keys being the color to use for the highlight, the values being a
     * list of lists, the outer list corresponding to each molecule, the inner one being the atom indexes for that
     * molecule.
     *
     * @param mols The molecules to render
     * @param atomHighlights A Map containing the data to highlight
     * @return
     * @throws CDKException
     * @throws CloneNotSupportedException
     */
    public Depiction depict(List<IAtomContainer> mols, Map<Color, List<List<Integer>>> atomHighlights)
            throws CDKException, CloneNotSupportedException {

        List<IAtomContainer> mols2 = new ArrayList<>();
        if (alignTo != null) {
            mols2.add(alignTo);
        }
        Map<Color, List<IChemObject>> allHighlights = new HashMap<>();
        int count = 0;

        // first do all the MCS alignments so that "user defined" ones override
        if (alignTo != null) {
            List<IChemObject> allMcsAtoms = new ArrayList<>();
            if (mcsColor != null) {
                allHighlights.put(mcsColor, allMcsAtoms);
            }

            for (IAtomContainer mol : mols) {
                List<IChemObject> mcsAtoms = alignAndLayoutMolecule(mol);
                if (!mcsAtoms.isEmpty()) {
                    allMcsAtoms.addAll(mcsAtoms);
                }
            }
        }

        // now do the rest of the processing
        for (IAtomContainer mol : mols) {

            IAtomContainer mol2 = fixMolecule(mol, showOnlyExplicitH);

            // the remove stereo trick only works if 2D coordinates are already present so we make sure that is the case
            ChemUtils.layoutMoleculeIn2D(mol, false);
            // fix the molecule so that it displays as required

            if (removeStereo) {
                // we need to display without stereochemistry e.g. no wedge/dash/squiggle bonds
                removeStereoChemistry(mol2);
            }

            Map<Color, List<Integer>> molHighlights = new HashMap<>();
            if (atomHighlights != null) {
                for (Color col : atomHighlights.keySet()) {
                    List<Integer> molAtoms = atomHighlights.get(col).get(count);
                    // TODO - check the color doesn't exist from the mcs phase
                    molHighlights.put(col, molAtoms);
                }
            }
            findHighlights(mol2, allHighlights, molHighlights);

            mols2.add(mol2);
            count++;
        }

        DepictionGenerator dg = generator;
        dg = dg.withMolTitle();
        if (allHighlights.size() > 0) {
            for (Color col : allHighlights.keySet()) {
                dg = dg.withHighlight(allHighlights.get(col), col);
            }
        }

        Depiction depiction = dg.depict(mols);
        return depiction;
    }

    private IAtomContainer fixMolecule(IAtomContainer mol, boolean showOnlyExplicitH) {
        if (showOnlyExplicitH) {
            mol = fixExplicitHOnly(mol);
        }
        if (titleFieldName == null) {
            mol.removeProperty(CDKConstants.TITLE);
        } else {
            String title = mol.getProperty(titleFieldName, String.class);
            mol.setProperty(CDKConstants.TITLE, title);
        }
        return mol;
    }

    private List<IChemObject> alignAndLayoutMolecule(IAtomContainer mol) throws CDKException, CloneNotSupportedException {
        List<IChemObject> atoms = new ArrayList<>();
        if (alignTo != null) {
            LOG.fine("Aligning molecule");
            try {
                Map<IAtom, IAtom> mapping = determineMCS(mol);
                if (mapping != null) {
                    ChemUtils.alignMolecule(alignTo, mol, mapping);
                    for (IAtom a : mapping.values()) {
                        atoms.add(a);
                    }
                }
            } catch (Exception ex) {
                // MCS generation and/or alignment can sometimes fail so make sure we cope with this
                // by representing the molecule as unaligned
                LOG.log(Level.WARNING, "Failed to align molecule", ex);
            }
        }
        LOG.fine("MCS atoms to highlight: " + atoms);
        return atoms;
    }

    private void removeStereoChemistry(IAtomContainer mol) {
        LOG.fine("Removing stereochemistry from bonds");
        for (IBond bond : mol.bonds()) {
            bond.setStereo(IBond.Stereo.NONE);
            bond.setDisplay(IBond.Display.Solid);
        }
    }

    private IAtomContainer fixExplicitHOnly(IAtomContainer mol) {
        LOG.fine("Removing implicit hydrogens");
        for (IAtom atom : mol.atoms()) {
            if (atom.getAtomicNumber() == 6) {
                // count the number of connections that are heavy atoms
                int numHeavy = countHeavyAtomConnections(atom);
                // if only one then this is a terminal carbon so we need to leave the Hs in place
                if (numHeavy < 2) {
                    atom.setImplicitHydrogenCount(0);
                }
            } else { // non-carbon atoms
                atom.setImplicitHydrogenCount(0);
            }
        }
        return mol;
    }

    private Map<IAtom, IAtom> determineMCS(IAtomContainer mol) throws CDKException, CloneNotSupportedException {
        if (alignTo == null) {
            LOG.warning("No query molecule to align to");
            return null;
        }
        ChemUtils.prepareForMCS(mol);
        Map<IAtom, IAtom> mapping = ChemUtils.determineMCS(alignTo, mol);
        return mapping;
    }

    private List<IAtom> findAtomHighlights(IAtomContainer mol, List<Integer> atomIndexes) {
        List<IAtom> atomHighlightList = new ArrayList<>();
        if (atomIndexes != null) {
            for (int index : atomIndexes) {
                IAtom atom = mol.getAtom(index);
                if (atom != null) {
                    atomHighlightList.add(atom);
                    LOG.fine("Highlighting atom " + index);
                }
            }
        }
        return atomHighlightList;
    }

    private List<IBond> findBondHighlights(IAtomContainer mol, List<IAtom> atoms) {
        int count = atoms.size();
        List<IBond> bondHighlightList = new ArrayList<>();
        if (count == 0) {
            LOG.fine("No atoms to highlight");
        } else {
            // find any connected bonds as we highlight those too
            for (int i = 0; i < count; i++) {
                for (int j = i + 1; j < count; j++) {
                    IBond bond = mol.getBond(atoms.get(i), atoms.get(j));
                    if (bond != null) {
                        LOG.fine("Highlighting bond between atom " + i + " and " + j);
                        bondHighlightList.add(bond);
                    }
                }
            }
        }
        return bondHighlightList;
    }

    /**
     * Call this for each molecule to accumulate the atoms and bonds that have to be highlighted
     *
     * @param mol            The molecule
     * @param allHighLights  Map that accumulates all the atom and bond objects that need to be highlighted for each color
     *                       during multiple calls.
     * @param atomHighlights The atom indices of the atoms to highlight with each color
     */
    private void findHighlights(
            IAtomContainer mol,
            Map<Color, List<IChemObject>> allHighLights,
            Map<Color, List<Integer>> atomHighlights) {

        if (atomHighlights != null) {
            for (Color color : atomHighlights.keySet()) {
                List<Integer> atomIndices = atomHighlights.get(color);
                List<IAtom> atomHighlightList = findAtomHighlights(mol, atomIndices);
                List<IBond> bondHighlightList = findBondHighlights(mol, atomHighlightList);

                List<IChemObject> chemObjs;
                if (allHighLights.containsKey(color)) {
                    chemObjs = allHighLights.get(color);
                } else {
                    chemObjs = new ArrayList<>();
                    allHighLights.put(color, chemObjs);
                }
                chemObjs.addAll(atomHighlightList);
                chemObjs.addAll(bondHighlightList);
            }
        }
    }

    /**
     * Write image as bytes in one of the supported formats. The
     *
     * @param img    The image to write
     * @param format e.g. PNG
     * @return
     * @throws IOException
     */
    public byte[] writeImage(BufferedImage img, String format) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, format, out);
        out.close();
        return out.toByteArray();
    }

    private int countHeavyAtomConnections(IAtom atom) {
        int numHeavy = 0;
        for (IBond bond : atom.bonds()) {
            IAtom other = bond.getOther(atom);
            if (other.getAtomicNumber() > 1) {
                numHeavy++;
            }
        }
        return numHeavy;
    }

}
