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
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.renderer.SymbolVisibility;
import org.openscience.cdk.renderer.color.*;
import org.openscience.cdk.renderer.generators.standard.StandardGenerator;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

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
 * Mol depiction using CDK.
 * <p>
 * Created by timbo on 01/08/2019.
 */

public class CDKMolDepict {

    private static final Logger LOG = Logger.getLogger(CDKMolDepict.class.getName());

    private static final SmilesParser smilesParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
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
    private final boolean showOnlyExplicitH;

    /**
     * Create with default parameters
     */
    public CDKMolDepict() {
        this(null, null, null, null, null, null, null);
    }

    /**
     * Allows to define custom parameters.
     * You can use the {#link createDepictionGenerator} to create the generator with suitable values
     *
     */
    public CDKMolDepict(Integer width,
                        Integer height,
                        Double margin,
                        IAtomColorer colorScheme,
                        Color backgroundColor,
                        Boolean expandToFit,
                        Boolean showOnlyExplicitH) {

        this.showOnlyExplicitH = showOnlyExplicitH == null ? false : showOnlyExplicitH.booleanValue();

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
                                    int numHeavy = 0;
                                    for (IBond bond : atom.bonds()) {
                                        IAtom other = bond.getOther(atom);
                                        if (other.getAtomicNumber() > 1) {
                                            numHeavy++;
                                        }
                                    }
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
    public String smilesToSVG(String smiles) throws IOException, CDKException {
        if (smiles == null || smiles.length() == 0) {
            throw new IllegalArgumentException("Smiles must be defined");
        }

        IAtomContainer mol = readSmiles(smiles);
        return moleculeToSVG(mol);
    }

    /**
     * Generate SVG from the provided CDK IAtomContainer
     *
     * @param mol
     * @return
     * @throws CDKException
     */
    public String moleculeToSVG(IAtomContainer mol) throws CDKException {
        return moleculeToSVG(mol, null, null, null);
    }

    public String moleculeToSVG(IAtomContainer mol, Color highlightColor, Boolean outerGlow, Integer... atomHighlights)
            throws CDKException {

        if (mol == null) {
            throw new IllegalArgumentException("Molecule must be defined");
        }

        DepictionGenerator g = fixHighlights(mol, highlightColor, outerGlow, atomHighlights);
        Depiction depiction = g.depict(mol);
        return depiction.toSvgStr();
    }

    public BufferedImage moleculeToImage(IAtomContainer mol) throws CDKException {
        return moleculeToImage(mol, null, null, null);
    }

    public BufferedImage moleculeToImage(IAtomContainer mol, Color highlightColor, Boolean outerGlow, Integer... atomHighlights)
            throws CDKException {

        if (mol == null) {
            throw new IllegalArgumentException("Molecule must be defined");
        }

        if (showOnlyExplicitH) {
            for (IAtom atom : mol.atoms()) {
                if (atom.getAtomicNumber() == 6) {
                    // count the number of connections that are heavy atoms
                    int numHeavy = 0;
                    for (IBond bond : atom.bonds()) {
                        IAtom other = bond.getOther(atom);
                        if (other.getAtomicNumber() > 1) {
                            numHeavy++;
                        }
                    }
                    // if only one then this is a terminal carbon so we need to leave the Hs in place
                    if (numHeavy < 2) {
                        atom.setImplicitHydrogenCount(0);
                    }
                } else { // non-carbon atoms
                    atom.setImplicitHydrogenCount(0);
                }
            }
        }

        DepictionGenerator g = fixHighlights(mol, highlightColor, outerGlow, atomHighlights);
        Depiction depiction = g.depict(mol);
        return depiction.toImg();
    }

    private DepictionGenerator fixHighlights(
            IAtomContainer mol,
            Color highlightColor,
            Boolean outerGlow,
            Integer... atomHighlights) {

        if (atomHighlights == null || highlightColor == null) {
            return generator;
        }

        List<IAtom> atomHighlightList = new ArrayList<>();
        for (int index : atomHighlights) {
            IAtom atom = mol.getAtom(index);
            if (atom != null) {
                atomHighlightList.add(atom);
                LOG.fine("Highlighting atom " + index);
            }
        }
        int count = atomHighlightList.size();
        if (count == 0) {
            LOG.info("No atoms to highlight");
            return generator;
        }

        // find any connected bonds as we highlight those too
        List<IBond> bondHighlightList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            for (int j = i + 1; j < count; j++) {
                IBond bond = mol.getBond(atomHighlightList.get(i), atomHighlightList.get(j));
                if (bond != null) {
                    LOG.fine("Highlighting bond between atom " + i + " and " + j);
                    bondHighlightList.add(bond);
                }
            }
        }

        if (bondHighlightList.size() > 0) {
            List<IChemObject> highlightList = new ArrayList<>();
            highlightList.addAll(atomHighlightList);
            highlightList.addAll(bondHighlightList);
            DepictionGenerator g = generator.withHighlight(highlightList, highlightColor);
            if (outerGlow) {
                g = g.withOuterGlowHighlight();
            }
            return g;
        } else {
            DepictionGenerator g = generator.withHighlight(atomHighlightList, highlightColor);
            if (outerGlow) {
                g = g.withOuterGlowHighlight();
            }
            return g;
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

    /**
     * Convenience method to generate IAtomContainer from SMILES string
     *
     * @param smiles
     * @return
     * @throws IOException
     */
    public static IAtomContainer readSmiles(String smiles) throws IOException {
        try {
            return smilesParser.parseSmiles(smiles);
        } catch (InvalidSmilesException ise) {
            throw new IOException("Invalid SMILES", ise);
        }
    }

//    public static DepictionGenerator createDepictionGenerator() {
//        return createDepictionGenerator(null, null, null, null, null, null, null);
//    }

//    public static DepictionGenerator createDepictionGenerator(
//            Integer width,
//            Integer height,
//            Double margin,
//            IAtomColorer colorScheme,
//            Color backgroundColor,
//            Boolean expandToFit,
//            Boolean showImplicitH) {
//
//        DepictionGenerator dg = new DepictionGenerator()
//                .withTerminalCarbons()
//                .withBackgroundColor(backgroundColor == null ? DEFAULT_BACKGROUND : backgroundColor)
//                .withSize(width == null ? DEFAULT_SIZE : width, height == null ? DEFAULT_SIZE : height)
//                .withAtomColors(colorScheme == null ? DEFAULT_COLORER : colorScheme)
//                .withMargin(margin == null ? 0d : margin);
//
//
//        if (expandToFit == null || expandToFit.booleanValue()) {
//            dg = dg.withFillToFit();
//        }
////        if (showImplicitH == null || showImplicitH.booleanValue()) {
//        if (true) {
//            dg = dg.withParam(StandardGenerator.Visibility.class,
//                    new SymbolVisibility() {
//                        @Override
//                        public boolean visible(IAtom atom, List<IBond> neighbors, RendererModel model) {
//                            return atom.getAtomicNumber() != 6;
//                        }
//                    });
//        }
//        return dg;
//    }

}
