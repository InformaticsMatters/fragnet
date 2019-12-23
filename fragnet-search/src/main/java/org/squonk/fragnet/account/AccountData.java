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
package org.squonk.fragnet.account;

import java.io.BufferedReader;
import java.text.*;
import java.util.*;
import java.util.logging.Logger;


/** Holds query history information allowing query execution to be restricted.
 * After creating an instance you should read the log files using the @{readLogfile} method, and then call the @{pruneAll}
 * method to remove expired queries.
 *
 * The @{getQueryCount(String} method get's the number of queries that user has ran in the configured period which is the
 * current time less @{daysCuttoff} days (default is 30 days). Before counting the queries this method calls the
 * @{pruneData()} method for that username which removes queries that were executed more than daysCuttoff ago.
 *
 * To keep in sync the @{increment(String} method must be called whenever a new query is executed (or to be more precise
 * a query that should be counted e.g. one that found some results). This adds a new query at that point in time.
 *
 */
public class AccountData {

    private static final Logger LOG = Logger.getLogger(AccountData.class.getName());
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    public static final int DEFAULT_DAYS_CUTTOFF = 30;
    private final int daysCuttoff;

    private Map<String, List<Long>> data = Collections.synchronizedMap(new HashMap<>());

    public AccountData() {
        this(DEFAULT_DAYS_CUTTOFF);
    }

    public AccountData(int daysCuttoff) {
        this.daysCuttoff = daysCuttoff;
    }

    public int getDaysCuttoff() {
        return daysCuttoff;
    }

    public int readLogfile(BufferedReader reader) throws Exception {
        String line;
        int total = 0;
        int count = 0;
        int errors = 0;
        while (null != (line = reader.readLine())) {

            // format is
            // [2019-09-04 17:24:29] INFO    | OPENED
            // [2019-09-05 21:43:51.453+0000] INFO    | tdudgeon\tNeighbourhoodQuery\t10887047685\tnodes=836,edges=1030,groups=799

            total++;

            try {

                String[] parts = line.split("\t");

                if (parts.length > 1) {
                    String datePart = parts[0].substring(1, parts[0].indexOf("]"));
                    String[] parts0 = parts[0].split(" ");
                    String username = parts0[parts0.length - 1];
                    Long date = buildDate(datePart);
                    addItem(username, date);
                    count++;
                }

            } catch (Exception ex) {
                LOG.warning("Failed to parse log entry: " + line);
                errors++;
            }

        }

        LOG.info(String.format("Processed %s rows, added %s records, %s errors", total, count, errors));

        return count;
    }

    public void pruneAllData() {
        for (String username : data.keySet()) {
            pruneData(username);
        }
    }

    /** Prune the queries for this user that are older than the number of days specified by the daysCuttoff class property.
     *
     * @param username
     */
    private void pruneData(String username) {
        long now = System.currentTimeMillis();
        Date cuttoff = new Date(now - ((long)daysCuttoff * 86400000));
        List<Long> list = data.get(username);

        if (list != null) {
            Iterator<Long> it = list.iterator();
            int num_pruned = 0;
            int num_kept = 0;
            while (it.hasNext()) {
                long time = it.next();
                Date d = new Date(time);
                if (d.before(cuttoff)) {
                    it.remove();
                    num_pruned++;
                } else {
                    num_kept++;
                }
            }
            LOG.fine(String.format("Pruned %s, kept %s", num_pruned, num_kept));
        }
    }

    private Long buildDate(String dateString) throws ParseException {
        Date date = DATE_FORMAT.parse(dateString);
        return date.getTime();
    }

    private void addItem(String username, Long date) {
        List<Long> list = data.get(username);
        if (list == null) {
            list = new ArrayList<Long>();
            data.put(username, list);
        }
        list.add(date);
    }

    protected int getQueryCount(String username, boolean pruneDays) {
        if (pruneDays) {
            pruneData(username);
        }
        List<Long> items = data.get(username);
        return items == null ? 0 : items.size();
    }

    public int getQueryCount(String username) {
        return getQueryCount(username, true);
    }

    public void incrementQueryCount(String username) {
        addItem(username, System.currentTimeMillis());
    }

    /** Get all the counts.
     * Does NOT perform any pruning. Use the @{pruneAllData()} method first if that is needed.
     *
     * @return Map of counts. Keys are the usernames, values are the counts.
     */
    public Map<String,Integer> getQueryCounts() {
        final Map<String,Integer> map = new HashMap<>();
        data.forEach((k,v) -> map.put(k, v.size()));
        return map;
    }
}
