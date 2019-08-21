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

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.renderer.color.IAtomColorer;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
public class CDKMolDepictServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(CDKMolDepictServlet.class.getName());


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {


        String smiles = req.getParameter("mol");
        if (smiles == null) {
            LOG.info("No smiles specified. Cannot render");
            return;
        }
        try {
            long t0 = new Date().getTime();
            IAtomContainer mol = ChemUtils.readSmiles(smiles);
            generateSVG(req, resp, mol);
            long t1 = new Date().getTime();
            LOG.fine("Depicting " + smiles + " took " + (t1 - t0) + "ms");
        } catch (CDKException e) {
            throw new IOException("Failed to generate SVG", e);
        }
    }


    protected void generateSVG(
            HttpServletRequest req,
            HttpServletResponse resp,
            IAtomContainer mol) throws IOException, CDKException {

        Map<String, String[]> params = req.getParameterMap();
        CDKMolDepict moldepict = createMolDepict(params);

        String svg = null;
        try {
            List<Integer> highlightedAtoms = readHighlightedAtoms(params);
            if (highlightedAtoms == null || highlightedAtoms.isEmpty()) {
                // no atom highlighting
                svg = moldepict.moleculeToSVG(mol);
            } else {
                boolean outerGlow = getBooleanHttpParameter("outerGlow", params, false);
                Color highlightColor = getColorHttpParameter("highlightColor", params, Color.RED);
                svg = moldepict.moleculeToSVG(mol, highlightColor, highlightedAtoms, outerGlow);
            }


        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error in SVG depiction", e);
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

    private List<Integer> readHighlightedAtoms(Map<String, String[]> params) {
        List<Integer> highlights = getIntegerArrayHttpParameter("highlightAtoms",params, null);
        return highlights;
    }

    protected CDKMolDepict createMolDepict(Map<String, String[]> params) throws IOException, CDKException {

        int width = getIntegerHttpParameter("w", params, 50);
        int height = getIntegerHttpParameter("h", params, 50);
        double margin = getDoubleHttpParameter("m", params, 0d);

        String mcs = getStringHttpParameter("mcs", params, null);
        Color mcsColor = getColorHttpParameter("mcsColor", params, null);
        IAtomContainer query = mcs == null ? null : ChemUtils.readSmiles(mcs);

        String colorer = getStringHttpParameter("colorScheme", params, null);
        IAtomColorer colorScheme = CDKMolDepict.getColorer(colorer);
        if (colorScheme == null) {
            colorScheme = CDKMolDepict.DEFAULT_COLORER;
        }

        Color backgroundColor = getColorHttpParameter("bg", params, CDKMolDepict.DEFAULT_BACKGROUND);
        boolean expandToFit = getBooleanHttpParameter("expand", params, true);
        boolean showExplicitHOnly = getBooleanHttpParameter("explicitHOnly", params, false);
        boolean noStereo = getBooleanHttpParameter("noStereo", params, false);

        CDKMolDepict depict = new CDKMolDepict(
                width, height, margin, colorScheme, backgroundColor, expandToFit, noStereo);
        depict.setShowOnlyExplicitH(showExplicitHOnly);
        depict.setMCSAlignment(query, mcsColor);

        return depict;
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

    private static List<Integer> getIntegerArrayHttpParameter(String name, Map<String, String[]> params, List<Integer> defaultValue) {
        String str = getStringHttpParameter(name, params, null);
        if (str == null) {
            return defaultValue;
        } else {
            String[] items = str.trim().split(",");
            List<Integer> results = new ArrayList<>();
            for (String item : items) {
                try {
                    Integer i = new Integer(item.trim());
                    results.add(i);
                } catch (NumberFormatException e) {
                    LOG.log(Level.WARNING, "Invalid integer: " + item, e);
                }
            }
            return results.isEmpty() ? null : results;
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
