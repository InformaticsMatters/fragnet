package org.squonk.fragnet.service;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.squonk.fragnet.Utils;
import org.squonk.fragnet.chem.Calculator;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public abstract class AbstractFragnetSearchRouteBuilder extends RouteBuilder {

    private static final Logger LOG = Logger.getLogger(AbstractFragnetSearchRouteBuilder.class.getName());

    // The Q_LOG is used to log query events
    // anonymous events that record queries that are conducted.
    // We expect this to be a size-limited set of files.
    private static final Logger Q_LOG = Logger.getLogger("QueryLog");
    private static final int LOG_FILE_SIZE = 64000;
    private static final int LOG_FILE_COUNT = 10;
    private static final boolean LOG_FILE_APPEND = true;

    public AbstractFragnetSearchRouteBuilder(String queryLogFileName) {
        String utilsLogPath = Utils.getLogPath();
        if (queryLogFileName != null && utilsLogPath != null) {
            String fileAndPath = utilsLogPath + '/' + queryLogFileName;
            FileHandler fh = null;
            try {
                fh = new FileHandler(fileAndPath,
                                     LOG_FILE_SIZE,
                                     LOG_FILE_COUNT,
                                     LOG_FILE_APPEND);
            } catch (IOException e) {
                LOG.severe("Failed to create FileHandler (" + e.getMessage() + ")");
            }
            if (fh != null) {
                LOG.info("Adding file handler (fileAndPath=" + fileAndPath + ")");
                Q_LOG.addHandler(fh);
                Q_LOG.info("OPENED");
            }
        } else {
            LOG.warning("queryLogFileName=" + queryLogFileName +
                        " getLogPath=" + utilsLogPath);
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
        String txt = String.format("%s\t%s\t%s\tnodes=%s,edges=%s,groups=%s\n", user, searchType, executionTime, nodes, edges, groups);
        writeToQueryLog(txt);
    }

    protected void writeErrorToQueryLog(String user, String searchType, long executionTime, String msg) {
        String txt = String.format("%s\t%s\t%s\terror=%s\n", user, searchType, executionTime, msg);
        writeToQueryLog(txt);
    }

    protected void writeToQueryLog(String txt) {
        LOG.info("QUERY | " + txt);
        Q_LOG.info(txt);
    }
}
