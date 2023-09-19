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

package org.squonk.fragnet.depict;

import org.openscience.cdk.exception.CDKException;
import org.squonk.cdk.depict.ChemUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Mol conversion servlet using CDK.
 * Example URL: /smiles2molfile?smiles=_smiles_
 * where:
 * <ul>
 * <li>_smiles_ is the molecule in smiles format</li>
 * </ul>
 * For example, this renders caffeine as SVG with a partly transparent yellow background (# is encoded as %23):<br>
 * http://localhost:8080/fragnet-depict/smiles2molfile?smiles=CN1C%3DNC2%3DC1C(%3DO)N(C)C(%3DO)N2C
 * <p>
 * Created by timbo on 31/07/2019.
 */
@WebServlet(
        name = "CDKSmiles2MolfileServlet",
        description = "Molecule format conversion using CDK",
        urlPatterns = {"/smiles2molfile"}
)
public class CDKSmiles2MolfileServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(CDKSmiles2MolfileServlet.class.getName());


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String smiles = req.getParameter("smiles");
        if (smiles == null) {
            LOG.info("No smiles specified. Cannot render");
            return;
        }
        try {
            String molfile = ChemUtils.convertSmilesToMolfile(smiles);
            resp.setHeader("Content-Type", "chemical/x-mdl-molfile");
            resp.setHeader("Content-Length", "" + molfile.getBytes().length);
            addCorsHeaders(req, resp);
            resp.getWriter().print(molfile);
            resp.getWriter().flush();
            resp.getWriter().close();
        } catch (CDKException e) {
            throw new IOException("Failed to generate Molfile for " + smiles, e);
        }
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doOptions(req, resp);
        addCorsHeaders(req, resp);
    }

    private void addCorsHeaders(HttpServletRequest req, HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
    }
}
