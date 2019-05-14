package org.squonk.fragnet.service;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.squonk.fragnet.Utils;
import org.squonk.fragnet.chem.Calculator;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractFragnetSearchRouteBuilder extends RouteBuilder {

    private static final Logger LOG = Logger.getLogger(AbstractFragnetSearchRouteBuilder.class.getName());

    private final File queryLogFile;

    public AbstractFragnetSearchRouteBuilder(String queryLogFileName) {
        if (queryLogFileName != null) {
            queryLogFile = Utils.createLogfile(queryLogFileName);
        } else {
            queryLogFile = null;
        }
    }

    protected String getUsername(Exchange exch) {
        HttpServletRequest request = exch.getIn().getBody(HttpServletRequest.class);
        Principal principal = request.getUserPrincipal();
        if (principal != null) {
            return principal.getName();
        } else {
            return "unauthenticated";
        }
    }

    protected List<Calculator.Calculation> parseCalculations(String calcs) {
        List<Calculator.Calculation> calculations = new ArrayList<>();
        if (calcs != null) {
            String[] names = calcs.split(",");
            for (String name : names) {
                name = name.trim();
                if (!name.isEmpty()) {
                    LOG.finer("Adding calculation " + name);
                    calculations.add(Calculator.Calculation.valueOf(name));
                }
            }
        }
        return calculations;
    }

    /** Parse the comma separated list of suppliers into a List
     *
     * @param suppliers
     * @return
     */
    protected List<String> parseSuppliers(String suppliers) {
        if (suppliers == null || suppliers.isEmpty()) {
            return Collections.emptyList();
        } else {
            String[] values = suppliers.trim().split(" *, *");
            return Arrays.asList(values);
        }
    }

    protected void writeToQueryLog(String user, String searchType, long executionTime, int nodes, int edges, int groups) {
        String date = Utils.getCurrentTime();
        String txt = String.format("%s\t%s\t%s\t%s\tnodes=%s,edges=%s,groups=%s\n", user, date, searchType, executionTime, nodes, edges, groups);
        writeToQueryLog(txt);
    }

    protected void writeErrorToQueryLog(String user, String searchType, long executionTime, String msg) {
        String date = Utils.getCurrentTime();
        String txt = String.format("%s\t%s\t%s\t%s\terror=%s\n", user, date, searchType, executionTime, msg);
        writeToQueryLog(txt);
    }

    protected void writeToQueryLog(String txt) {
        if (queryLogFile != null) {
            try {
                Utils.appendToFile(queryLogFile, txt);
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Failed to write to query log", e);
            }
        } else {
            LOG.info(txt);
        }
    }
}
