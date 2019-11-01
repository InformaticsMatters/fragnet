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
package org.squonk.fragnet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

public class Utils {

    private static final Logger LOG = Logger.getLogger(Utils.class.getName());
    private static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private static final String LOG_PATH;

    /**
     * Get a value that might be configured externally. Looks first for a system property
     * (a -D option specified to Java), if not present looks for an environment variable
     * and if not present then falls back to the specified default.
     * <p/>
     * If the default is 'null' then a system property or environment variable
     * must be defined otherwise an IllegalStateException exception ius thrown.
     *
     * @param name         The system property or environment variable name
     * @param defaultValue The value to fall back to.
     * @return The configured value
     */
    public static String getConfiguration(String name, String defaultValue) throws IllegalStateException {

        LOG.fine("Looking for " + name);

        //Map<String,String> env = System.getenv();
        //env.entrySet().forEach((e) -> LOG.info("ENV " + e.getKey() + " -> " + e.getValue()));
        //String value = env.get(name);
        //LOG.info("FOUND ENV " + value);

        String s = System.getProperty(name);
        LOG.finer("Found prop " + s);
        if (s != null && s.length() > 0) {
            return s;
        }
        s = System.getenv(name);
        LOG.finer("Found env " + s);
        if (s != null && s.length() > 0) {
            return s;
        }
        if (defaultValue == null) {
            // No system or environment variable and no default.
            // This is an error condition.
            throw new IllegalStateException("Nothing found for '" + name + "'");
        }
        return defaultValue;
    }

    public static DateFormat getDateFormat() {
        return DATE_FORMAT;
    }

    /** Get the current time using the declared DateFormats
     *
     * @return
     */
    public static String getCurrentTime() {
        return DATE_FORMAT.format(new Date());
    }

    /**
     * Returns the configured LOG_PATH,
     * which is either an environment variable (LOG_ROOT)
     * or a property (user.home)
     *
     * @return A String, which may be NULL
     */
    public static String getLogPath() {
        return LOG_PATH;
    }

    /**
     * Creates a logfile. The logfile is placed in the directory indicated
     * by the 'LOG_ROOT' environment variable or 'user.home' if that is not set.
     *
     * @param filename The filename to use
     * @return A File instance, null if the file could not be created.
     */
    public static File createLogfile(String filename) {

        LOG.info("LOG_PATH=" + getLogPath());

        File queryLogFile = null;
        if (LOG_PATH != null) {
            File file = new File(LOG_PATH, filename);
            if (Utils.createFileIfNotPresent(file) == 1) {
                try {
                    // test we can write to it
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        String msg = "NeighbourhoodQuery log created on " + Utils.getCurrentTime() + "\n";
                        fos.write(msg.getBytes());
                    }
                } catch (Exception e) {
                    LOG.warning("Failed to create query log file for writing");
                    file = null;
                }
            }
            queryLogFile = file;
            LOG.info("Writing query log to " + file.getPath());

        } else {
            LOG.warning("No user.home defined. Cannot create " + filename);
        }
        return queryLogFile;

    }

    public static void appendToFile(File file, String text) throws IOException {
        if (createFileIfNotPresent(file) >= 0) {
            FileOutputStream fos = new FileOutputStream(file, true);
            try {
                FileLock lock = fos.getChannel().lock();
                try {
                    fos.write(text.getBytes());
                } finally {
                    lock.release();
                }
            } finally {
                fos.close();
            }
        }
    }

    public static int createFileIfNotPresent(File file) {
        if (!file.exists()) {
            try {
                file.createNewFile();
                LOG.info("File " + file.getPath() + " created");
                return 1;
            } catch (Exception e) {
                LOG.warning("Failed to create file " + file.getPath());
                return -1;
            }
        }
        return 0;
    }

    static {
        String logPath = System.getenv("LOG_ROOT");
        if (logPath == null) {
            logPath = System.getProperty("user.home");
        }
        LOG_PATH = logPath;
    }

}
