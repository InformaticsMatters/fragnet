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

package org.squonk.fragnet.depict;

import org.openscience.cdk.depict.Depiction;
import org.openscience.cdk.depict.DepictionGenerator;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.geometry.GeometryUtil;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.renderer.SymbolVisibility;
import org.openscience.cdk.renderer.color.*;
import org.openscience.cdk.renderer.generators.standard.StandardGenerator;
import org.openscience.smsd.AtomAtomMapping;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Mol depiction using CDK. Can be used to generate SVG or image files.
 * <p>
 * Options include:
 * <ul>
 *     <li>image width, height and margin</li>
 *     <li>various atom colour schemes</li>
 *     <li>specify background colour (including transparency)</li>
 *     <li>expand molecule to fit the available space</li>
 *     <li>highlight a substructure (defined as MSC)</li>
 *     <li>highlight user specified atoms</li>
 * </ul>
 * <p>
 * See the moleculeToSVG and moleculeToImage methods for more details.
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

    /**
     * Create with default parameters
     */
    public CDKMolDepict() {
        this(null, null, null, null, null, null);
    }

    /**
     * Constructor allows to define custom parameters.
     */
    public CDKMolDepict(Integer width,
                        Integer height,
                        Double margin,
                        IAtomColorer colorScheme,
                        Color backgroundColor,
                        Boolean expandToFit) {

        DepictionGenerator dg = new DepictionGenerator()
                .withTerminalCarbons()
                .withBackgroundColor(backgroundColor == null ? DEFAULT_BACKGROUND : backgroundColor)
                .withSize(width == null ? DEFAULT_SIZE : width, height == null ? DEFAULT_SIZE : height)
                .withAtomColors(colorScheme == null ? DEFAULT_COLORER : colorScheme)
                .withMargin(margin == null ? 0d : margin)
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


        if (expandToFit == null || expandToFit.booleanValue()) {
            dg = dg.withFillToFit();
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
     * Set a molecucle to align the rendered molecules to (using MCS).
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
            if (!GeometryUtil.has2DCoordinates(alignTo)) {
                StructureDiagramGenerator g = new StructureDiagramGenerator();
                g.generateCoordinates(alignTo);
            }
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

    /**
     * Generate SVG from the provided SMILES
     *
     * @param smiles
     * @return
     * @throws IOException
     * @throws CDKException
     */
    public String smilesToSVG(String smiles) throws IOException, CDKException, CloneNotSupportedException {
        if (smiles == null || smiles.length() == 0) {
            throw new IllegalArgumentException("Smiles must be defined");
        }

        IAtomContainer mol = ChemUtils.readSmiles(smiles);
        return moleculeToSVG(mol);
    }

    /**
     * Generate SVG from the provided CDK IAtomContainer
     *
     * @param mol
     * @return
     * @throws CDKException
     */
    public String moleculeToSVG(IAtomContainer mol) throws CDKException, CloneNotSupportedException {
        return moleculeToSVG(mol, null, null, null);
    }

    /**
     * Create a SVG for the molecule.
     *
     * @param mol            The molecule to depict
     * @param highlightColor The colour to highlight atoms and bonds with
     * @param atomHighlights The indexes of the atoms to highlight. Bonds between these atoms are also highlighted.
     * @param outerGlow      Whether to use 'outer glow' highlighting instead of colouring the atoms and bonds directly.
     *                       If null then defaults to false.
     * @return A string with the SVG content (XML)
     * @throws CDKException
     * @throws CloneNotSupportedException
     */
    public String moleculeToSVG(IAtomContainer mol, Color highlightColor, List<Integer> atomHighlights, Boolean outerGlow)
            throws CDKException, CloneNotSupportedException {

        if (mol == null) {
            throw new IllegalArgumentException("Molecule must be defined");
        }
        Depiction depiction = depict(mol, highlightColor, atomHighlights, outerGlow);
        return depiction.toSvgStr();
    }

    /**
     * Depict molecule with no highlighting.
     *
     * @param mol
     * @return
     * @throws CDKException
     * @throws CloneNotSupportedException
     */
    public BufferedImage moleculeToImage(IAtomContainer mol) throws CDKException, CloneNotSupportedException {
        return moleculeToImage(mol, null, null, null);
    }

    /**
     * Create an image for the molecule. The images can then be written in a number of formats such as PNG.
     *
     * @param mol            The molecule to depict
     * @param highlightColor The colour to highlight atoms and bonds with
     * @param atomHighlights The indexes of the atoms to highlight. Bonds between these atoms are also highlighted.
     * @param outerGlow      Whether to use 'outer glow' highlighting instead of colouring the atoms and bonds directly.
     *                       If null then defaults to false.
     * @return
     * @throws CDKException
     * @throws CloneNotSupportedException
     */
    public BufferedImage moleculeToImage(
            IAtomContainer mol,
            Color highlightColor, List<Integer> atomHighlights,
            Boolean outerGlow)
            throws CDKException, CloneNotSupportedException {

        if (mol == null) {
            throw new IllegalArgumentException("Molecule must be defined");
        }

        Depiction depiction = depict(mol, highlightColor, atomHighlights, outerGlow);
        return depiction.toImg();
    }

    private Depiction depict(IAtomContainer mol, Color highlightColor, List<Integer> atomHighlights, Boolean outerGlow)
            throws CDKException, CloneNotSupportedException {
        IAtomContainer mol2 = fixMolecule(mol, showOnlyExplicitH);
        List<Integer> mcsAtomIndexes = alignAndLayoutMolecule(mol2);
        DepictionGenerator g = fixHighlights(mol2, highlightColor, atomHighlights, mcsAtomIndexes, outerGlow);
        Depiction depiction = g.depict(mol2);
        return depiction;
    }

    private IAtomContainer fixMolecule(IAtomContainer mol, boolean showOnlyExplicitH) {
        if (showOnlyExplicitH) {
            mol = fixExplicitHOnly(mol);
        }
        return mol;
    }

    private List<Integer> alignAndLayoutMolecule(IAtomContainer mol) throws CDKException, CloneNotSupportedException {
        List<Integer> atoms = new ArrayList<>();
        if (alignTo != null) {
            AtomAtomMapping mapping = alignMolecule(mol);
            if (mapping != null) {
                ChemUtils.alignMolecule(mol, mapping);
                Map<Integer, Integer> indexMappings = mapping.getMappingsByIndex();
                atoms.addAll(indexMappings.values());
            }
        }
        LOG.fine("MCS atoms to highlight: " + atoms);
        return atoms;
    }

    private IAtomContainer fixExplicitHOnly(IAtomContainer mol) {
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

    private AtomAtomMapping alignMolecule(IAtomContainer mol) throws CDKException, CloneNotSupportedException {
        if (alignTo == null) {
            LOG.warning("No query molecule to align to");
            return null;
        }
        ChemUtils.prepareForMCS(mol);
        AtomAtomMapping mapping = ChemUtils.determineMCS(alignTo, mol);
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

    private DepictionGenerator highlight(DepictionGenerator generator, List<IAtom> atoms, List<IBond> bonds, Color color) {
        DepictionGenerator g = generator;
        if (bonds.size() > 0) {
            List<IChemObject> highlightList = new ArrayList<>();
            highlightList.addAll(atoms);
            highlightList.addAll(bonds);
            g = g.withHighlight(highlightList, color);
        } else {
            g = g.withHighlight(atoms, color);
        }
        return g;
    }

    private DepictionGenerator fixHighlights(
            IAtomContainer mol,
            Color highlightColor,
            List<Integer> atomHighlights,
            List<Integer> mcsHighlights,
            Boolean outerGlow) {

        DepictionGenerator g = generator;

        if (mcsColor != null) {
            List<IAtom> mcsAtomHighlightList = findAtomHighlights(mol, mcsHighlights);
            List<IBond> mcsBondHighlightList = findBondHighlights(mol, mcsAtomHighlightList);
            g = highlight(g, mcsAtomHighlightList, mcsBondHighlightList, mcsColor);
        }

        List<IAtom> atomHighlightList = findAtomHighlights(mol, atomHighlights);
        List<IBond> bondHighlightList = findBondHighlights(mol, atomHighlightList);

        g = highlight(g, atomHighlightList, bondHighlightList, highlightColor);

        if (outerGlow != null && outerGlow.booleanValue()) {
            g = g.withOuterGlowHighlight();
        }
        return g;
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
