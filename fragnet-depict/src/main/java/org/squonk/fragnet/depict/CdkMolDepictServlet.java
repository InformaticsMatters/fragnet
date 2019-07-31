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
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.renderer.color.*;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mol depiction servlet using CDK.
 * Example URL: /moldepict?&w=_width_&h=_height_&bg=_rgba_&expand=_expand_&mol=_smiles_
 * where:
 * <ul>
 * <li>_width_ is the image width</li>
 * <li>_height_ is the image height</li>
 * <li>_rgba_ is the background color as RGBA integer (#AARRGGBB)</li>
 * <li>_expand_ is whether to expand the rendering to fit the image size (true/false)</li>
 * <li>_smiles_ is the molecule in smiles format</li>
 * </ul>
 * Only the smiles parameter is required. Defaults will be used for the others if not specified.<br>
 * For example, this renders caffeine as SVG with a partly transparent yellow background (# is encoded as %23):<br>
 * http://localhost:8080/context/moldepict?&w=75&h=75&bg=0x33FFFF00&mol=CN1C%3DNC2%3DC1C(%3DO)N(C)C(%3DO)N2C
 * <p>
 * Created by timbo on 31/07/2019.
 */
@WebServlet(
        name = "CDKMolDepictServlet",
        description = "Molecule depiction using CDK",
        urlPatterns = {"/moldepict"}
)
public class CdkMolDepictServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(CdkMolDepictServlet.class.getName());

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

    private static final IAtomColorer DEFAULT_COLORER = COLORERS.get("cdk2d");
    private static final Color DEFAULT_BACKGROUND = new Color(255, 255, 255, 0);


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String smiles = req.getParameter("smiles");
        if (smiles == null) {
            LOG.info("No smiles specified. Cannot render");
            return;
        }
        generateSVG(req, resp, smiles);
    }


    protected void generateSVG(
            HttpServletRequest req,
            HttpServletResponse resp,
            String smiles) throws IOException {

        IAtomContainer mol = smilesToIAtomContainer(smiles);

        Map<String, String[]> params = req.getParameterMap();

        int width = getIntegerHttpParameter("w", params, 50);
        int height = getIntegerHttpParameter("h", params, 50);
        double margin = getDoubleHttpParameter("m", params, 0d);

        String colorer = getStringHttpParameter("colorscheme", params, null);
        IAtomColorer colorScheme = COLORERS.get(colorer);
        if (colorScheme == null) {
            colorScheme = DEFAULT_COLORER;
        }

        Color backgroundColor = getColorHttpParameter("bg", params, DEFAULT_BACKGROUND);
        boolean expand = getBooleanHttpParameter("e", params, true); // expandToFit


        String svg = null;
        try {
            svg = moleculeToSVG(mol, width, height, margin, colorScheme, backgroundColor, expand);

        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error in svg depiction", e);
            return;
        }
        if (svg != null) {
            resp.setHeader("Content-Type", "image/svg+xml");
            resp.setHeader("Content-Length", "" + svg.getBytes().length);

            resp.getWriter().print(svg);
            resp.getWriter().flush();
            resp.getWriter().close();
        }
    }

    protected IAtomContainer smilesToIAtomContainer(String smiles) throws IOException {
        try {
            return smilesParser.parseSmiles(smiles);
        } catch (InvalidSmilesException ise) {
            throw new IOException("Invalid SMILES", ise);
        }
    }

    protected String moleculeToSVG(IAtomContainer mol,
                                   int width,
                                   int height,
                                   double margin,
                                   IAtomColorer colorScheme,
                                   Color backgroundColor,
                                   boolean expandToFit) throws CDKException {
        if (mol == null) {
            return null;
        }

        DepictionGenerator dg = createDepictionGenerator(width, height, margin, colorScheme, backgroundColor, expandToFit);
        Depiction depiction = dg.depict(mol);
        return depiction.toSvgStr();
    }

    DepictionGenerator createDepictionGenerator(int width,
                                                int height,
                                                double margin,
                                                IAtomColorer colorScheme,
                                                Color backgroundColor,
                                                boolean expandToFit) {

        DepictionGenerator dg = new DepictionGenerator()
                .withTerminalCarbons()
                //.withParam(BasicAtomGenerator.ShowExplicitHydrogens.class, true)
                .withBackgroundColor(backgroundColor)
                .withSize(width, height)
                .withAtomColors(colorScheme)
                .withMargin(margin > 0 ? margin : 0);

        if (expandToFit) {
            dg = dg.withFillToFit();
        }
        return dg;
    }

    private static String getStringHttpParameter(String name, Map<String, String[]> params, String defaultValue) {
        String[] values = params.get(name);
        if (values == null) {
            return null;
        }
        if (values.length == 1) {
            return values[0];
        } else {
            return defaultValue;
        }
    }

    private static Integer getIntegerHttpParameter(String name, Map<String, String[]> params, Integer defaultValue) {
        String str = getStringHttpParameter(name, params, null);
        if (str == null) {
            return defaultValue;
        } else {
            return new Integer(str);
        }
    }

    private static Double getDoubleHttpParameter(String name, Map<String, String[]> params, Double defaultValue) {
        String str = getStringHttpParameter(name, params, null);
        if (str == null) {
            return defaultValue;
        } else {
            return new Double(str);
        }
    }

    private static Boolean getBooleanHttpParameter(String name, Map<String, String[]> params, Boolean defaultValue) {
        String str = getStringHttpParameter(name, params, null);
        if (str == null) {
            return defaultValue;
        } else {
            return new Boolean(str);
        }
    }

    private static Color getColorHttpParameter(String name, Map<String, String[]> params, Color defaultValue) {
        String str = getStringHttpParameter(name, params, null);
        if (str == null) {
            return defaultValue;
        } else {
            try {
                Color col = Colors.rgbaHexToColor(str);
                return col;
            } catch (NumberFormatException ex) {
                LOG.log(Level.INFO, "Can't interpret color parameter: " + str, ex);
                return defaultValue;
            }
        }
    }


}
