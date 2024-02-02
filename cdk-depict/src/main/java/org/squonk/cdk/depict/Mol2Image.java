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

package org.squonk.cdk.depict;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.cli.*;

import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.depict.Depiction;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.iterator.IteratingSDFReader;
import org.openscience.cdk.renderer.generators.standard.StandardGenerator;


/** Commandline interface for molecule SVG generation.
 * The image generation is implemented in the CDKMolDepict class.
 * This class makes most of the image generation options available through CLI options.
 * See the main() method for details of those options.
 *
 * Example executions:
 * java org.squonk.cdk.depict.Mol2Image --help
 * java org.squonk.cdk.depict.Mol2Image -i input.sdf -o output.svg --background-color WHITE
 *
 * Some notes:
 *
 * 1. The output format is determined from the extension of the output file. Options include .svg, .png, .jpg, .pdf.
 *
 * 2. Various of the parameters are for specifying colours. These need to the String values of the color constants in
 *    the java.awt.Color class. (see https://docs.oracle.com/javase/8/docs/api/java/awt/Color.html)
 *
 * 3. Atoms to highlight can be provided in a SD-file field. The format for that field's value is multi-line text, each
 *    line being an atom to highlight, with the format "atom_index value" e.g. "3 7.4". That will render the atom with
 *    index 3 (starting from zero) with the value "7.4". To specify this use the "highlight-fields" and "highlight-colors"
 *    options which can take multiple values. e.g. options like this:
 *    "-highlight-fields APKA BPKA --highlight-colors CYAN PINK"
 *    That will highlight that atom data in the field called APKA with the color cyan and the data in the field called
 *    BPKA with the color pink.
 *
 * 4. Alignment to a molecule (using MCS) can be done using the mcs-color and mcs-smiles options. If a color is not
 *    specified then just alignment is performed. In principle MCS highlighting and filed specified atom highlights
 *    (see above) can both be done (MCS is done first) but might give unexpected results.
 *
 */
public class Mol2Image {

    private static final Logger LOG = Logger.getLogger(Mol2Image.class.getName());
    private static final DMLogger DMLOG = new DMLogger();

    private final String input;
    private final String output;

    private String[] highlightFields;
    private Color[] highlightColors;

    private final CDKMolDepict depicter;

    protected Mol2Image(String input, String output,
                        int width, int height, double padding,
                        Color backgroundColor, Color labelColor, Color titleColor, Boolean outerGlow,
                        Double labelScale, Double labelDistance, String titleField, Double titleScale) {
        this.input = input;
        this.output = output;

        Map<Class, Object> extraParams = new HashMap<>();
        if (labelDistance != null) {
            extraParams.put(StandardGenerator.AnnotationDistance.class, labelDistance);
        }

        depicter = new CDKMolDepict(width, height, 2.5d,
                null,
                backgroundColor, labelColor, titleField, titleColor, outerGlow,
                labelScale, titleScale, false, false, padding, extraParams);
    }

    public void setMCS(String smiles, Color color) throws CDKException, IOException {
        IAtomContainer mol = ChemUtils.readSmiles(smiles);
        setMCS(mol, color);
    }

    public void setMCS(IAtomContainer mol, Color color) throws CDKException {
        depicter.setMCSAlignment(mol, color);
    }

    public void setHighlights(String[] highlightFields, Color[] highlightColors) {
        assert highlightFields.length == highlightColors.length;
        this.highlightFields = highlightFields;
        this.highlightColors = highlightColors;
    }

    private static void deleteMysteryDir() throws IOException {
        Path myst = FileSystems.getDefault().getPath("?");
        if (Files.exists(myst)) {
            Log.info("Mystery dir exists - deleting it");
            Files.walk(myst)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    public static void main(String[] args) throws Exception {

        Options options = new Options();
        options.addOption("h", "help", false, "Display help");
        options.addOption(Option.builder("i")
                .longOpt("input")
                .hasArg()
                .argName("file")
                .desc("Input file with molecules (.sdf)")
                .required()
                .build());
        options.addOption(Option.builder("o")
                .longOpt("output")
                .hasArg()
                .argName("file")
                .desc("Output file for images (.png, .svg)")
                .required()
                .build());
        options.addOption(Option.builder(null)
                .longOpt("width")
                .hasArg()
                .argName("number")
                .desc("Image width")
                .type(Integer.class)
                .build());
        options.addOption(Option.builder(null)
                .longOpt("height")
                .hasArg()
                .argName("number")
                .desc("Image height")
                .type(Integer.class)
                .build());
        options.addOption(Option.builder(null)
                .longOpt("padding")
                .hasArg()
                .argName("number")
                .desc("Padding around molecule")
                .type(Integer.class)
                .build());
        options.addOption(Option.builder(null)
                .longOpt("background-color")
                .hasArg()
                .argName("color")
                .desc("Background color")
                .build());
        options.addOption(Option.builder(null)
                .longOpt("title-field")
                .hasArg()
                .argName("name")
                .desc("Title field")
                .build());
        options.addOption(Option.builder(null)
                .longOpt("title-scale")
                .hasArg()
                .argName("scale")
                .desc("Title scale (default 1)")
                .type(Double.class)
                .build());
        options.addOption(Option.builder(null)
                .longOpt("title-color")
                .hasArg()
                .argName("color")
                .desc("Title color")
                .build());
        options.addOption(Option.builder(null)
                .longOpt("mcs-smiles")
                .hasArg()
                .argName("smiles")
                .desc("MCS highlight (SMILES)")
                .build());
        options.addOption(Option.builder(null)
                .longOpt("mcs-color")
                .hasArg()
                .argName("color")
                .desc("MCS highlight color")
                .build());
        options.addOption(Option.builder(null)
                .longOpt("highlight-fields")
                .desc("Field names for atom highlighting")
                .numberOfArgs(Option.UNLIMITED_VALUES)
                //.required(false)
                .build());
        options.addOption(Option.builder(null)
                .longOpt("highlight-colors")
                .desc("Colours for atom highlighting")
                .numberOfArgs(Option.UNLIMITED_VALUES)
                //.required(false)
                .build());
        options.addOption(Option.builder(null)
                .longOpt("label-color")
                .hasArg()
                .argName("color")
                .desc("Atom label color")
                .build());
        options.addOption(Option.builder(null)
                .longOpt("label-scale")
                .hasArg()
                .argName("number")
                .desc("Label scale (default 1)")
                .type(Double.class)
                .build());
        options.addOption(Option.builder(null)
                .longOpt("label-distance")
                .hasArg()
                .argName("distance")
                .desc("Label distance (default 0.25)")
                .type(Double.class)
                .build());
        options.addOption(Option.builder(null)
                .longOpt("outer-glow")
                .hasArg(false)
                .desc("Highlight using outer glow")
                .build());

        if (args.length == 0 | (args.length == 1 && ("-h".equals(args[0]) | "--help".equals(args[0])))) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.setOptionComparator(null);
            formatter.printHelp("app", options);
        } else {

            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);
            StringBuilder builder = new StringBuilder(Mol2Image.class.getName());
            for (String arg : args) {
                builder.append(" ").append(arg);
            }
            DMLOG.logEvent(DMLogger.Level.INFO, builder.toString());

            String inputFile = cmd.getOptionValue("input");
            String outputFile = cmd.getOptionValue("output");
            int width = Integer.valueOf(cmd.getOptionValue("width", "500"));
            int height = Integer.valueOf(cmd.getOptionValue("height", "500"));
            double padding = Double.valueOf(cmd.getOptionValue("padding", "5"));
            String bgColorStr = cmd.getOptionValue("background-color");
            Color bgColor = (bgColorStr == null ? null : Colors.getColorByName(bgColorStr));
            String lblColorStr = cmd.getOptionValue("label-color");
            Color labelColor = (lblColorStr == null ? null : Colors.getColorByName(lblColorStr));
            double labelScale = Double.valueOf(cmd.getOptionValue("label-scale", "1"));
            String labelDistanceStr = cmd.getOptionValue("label-distance");
            Double labelDistance = (labelDistanceStr == null ? null : Double.valueOf(labelDistanceStr));

            String mcsColorStr = cmd.getOptionValue("mcs-color");
            Color mcsColor = (mcsColorStr == null ? null : Colors.getColorByName(mcsColorStr));
            String titleField = cmd.getOptionValue("title-field");
            String titleColorStr = cmd.getOptionValue("title-color");
            Color titleColor = (titleColorStr == null ? null : Colors.getColorByName(titleColorStr));
            double titleScale = Double.valueOf(cmd.getOptionValue("title-scale", "1"));
            String mcsSmiles = cmd.getOptionValue("mcs-smiles");
            boolean outerGlow = cmd.hasOption("outer-glow");

            String[] highlightFields = cmd.getOptionValues("highlight-fields");
            String[] highlightColorsStr = cmd.getOptionValues("highlight-colors");

            Mol2Image m2i = new Mol2Image(inputFile, outputFile, width, height, padding,
                    bgColor, labelColor, titleColor, outerGlow,
                    labelScale, labelDistance, titleField, titleScale);

            if (mcsSmiles != null) {
                m2i.setMCS(mcsSmiles, mcsColor);
            }

            Color[] highlightColors = null;
            if (highlightFields != null && highlightColorsStr != null) {
                if (highlightFields.length != highlightColorsStr.length) {
                    throw new IllegalArgumentException("highlight-fields and highlight-colors must have same number of arguments");
                }
                highlightColors = new Color[highlightColorsStr.length];
                for (int i = 0; i < highlightColorsStr.length; i++) {
                    highlightColors[i] = Colors.getColorByName(highlightColorsStr[i]);
                }
                m2i.setHighlights(highlightFields, highlightColors);
            }

            m2i.generate();
        }

        // delete the strange dir named "?" that is created by Java for font info
        deleteMysteryDir();
    }

    protected void generate() throws Exception {
        LOG.info(String.format("Using input %s to generate output %s", input, output));

        ChemUtils.createDirs(output);

        IteratingSDFReader reader = ChemUtils.readSDF(input);
        List<IAtomContainer> mols = new ArrayList<>();
        Map<Color, List<List<Integer>>> atomHighlights = new HashMap<>();

        if (highlightColors != null) {
            for (int i = 0; i < highlightColors.length; i++) {
                atomHighlights.put(highlightColors[i], new ArrayList<>());
            }
        }

        while (reader.hasNext()) {
            IAtomContainer mol = reader.next();
            mols.add(mol);

            if (highlightColors != null) {
                for (int i = 0; i < highlightColors.length; i++) {
                    String propName = highlightFields[i];
                    Color propColor = highlightColors[i];
                    List<Integer> atomIdxs = findAtomIndexes(mol, propName, true);
                    atomHighlights.get(propColor).add(atomIdxs);
                }
            }
        }

        Depiction depiction = depicter.depict(mols, atomHighlights);
        depiction.writeTo(output);
    }

    private List<Integer> findAtomIndexes(IAtomContainer mol, String propName, boolean addAtomLabel) {
        List<Integer> atomIdxs = new ArrayList<>();
        String prop = mol.getProperty(propName);
        if (prop != null) {
            String[] lines = prop.split("\n");
            for (String line : lines) {
                LOG.fine("Checking " + line);
                String[] tokens = line.split(" ");
                if (tokens.length == 2) {
                    int atno = Integer.valueOf(tokens[0]);
                    atomIdxs.add(atno);
                    String label = tokens[1];
                    LOG.fine(propName + " " + atno + " " + label);
                    if (addAtomLabel) {
                        mol.getAtom(atno).setProperty(CDKConstants.COMMENT, label);
                    }
                } else {
                    LOG.warning("Unable to handle " + line);
                }
            }
        }
        return atomIdxs;
    }
}
