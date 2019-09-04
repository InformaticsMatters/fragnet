package org.squonk.fragnet.service;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.squonk.fragnet.Utils;
import org.squonk.fragnet.chem.Calculator;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.Principal;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public abstract class AbstractFragnetSearchRouteBuilder extends RouteBuilder {

    private static final Logger LOG = Logger.getLogger(AbstractFragnetSearchRouteBuilder.class.getName());

    // The Q_LOG is used to log query events,
    // anonymous events that record queries that are conducted.
    // We expect this to be a size-limited set of files.
    private Logger Q_LOG;
    private static final int LOG_FILE_SIZE = 1000000;
    private static final int LOG_FILE_COUNT = 10;
    private static final boolean LOG_FILE_APPEND = true;

    public AbstractFragnetSearchRouteBuilder(String queryLogFileName) {

        Q_LOG = Logger.getLogger(queryLogFileName);
        Q_LOG.setUseParentHandlers(false);

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
                // By default files are XML.
                // Set to Simple (like the console) with the format
                // that contains the date/time and the logging level like...
                //
                //   [2019-09-04 15:05:27] INFO    | OPENED
                //
                fh.setFormatter(new SimpleFormatter() {
                    private static final String format = "[%1$tF %1$tT] %2$-7s | %3$s%n";
                    @Override
                    public synchronized String format(LogRecord lr) {
                        return String.format(format,
                                             new Date(lr.getMillis()),
                                             lr.getLevel().getLocalizedName(),
                                             lr.getMessage());
                    }
                });
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
        String txt = String.format("%s\t%s\t%s\tnodes=%s,edges=%s,groups=%s", user, searchType, executionTime, nodes, edges, groups);
        Q_LOG.info(txt);
    }

    protected void writeErrorToQueryLog(String user, String searchType, long executionTime, String msg) {
        String txt = String.format("%s\t%s\t%s\t%s", user, searchType, executionTime, msg);
        Q_LOG.severe(txt);
    }
}
