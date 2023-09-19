/*
 * Copyright (c) 2023  Informatics Matters Ltd.
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
 *
 */

package org.squonk.cdk.depict;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;


public class DMLogger {

    public enum Level { WARNING, INFO, DEBUG }

    private AtomicInteger costCounter = new AtomicInteger(0);

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'+00:00'", Locale.UK);
    private final String template = "%s # %s -%s- %s";

    public DMLogger() {
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public void logEvent(Level level, String msg) {
        System.out.println(createEventMessage(level, new Date(), msg));
    }

    protected String createEventMessage(Level level, Date date, String msg) {
        return createMessage(level, date, "EVENT", msg);
    }

    public void logCost(float num, boolean incremental) {
        System.out.println(createCostMessage(new Date(), num, incremental));
    }

    protected String createCostMessage(Date date, float num, boolean incremental) {
        if (incremental) {
            return createMessage(Level.INFO, date, "COST", "+" + num + " " + costCounter.incrementAndGet());
        } else {
            return createMessage(Level.INFO, date, "COST", "" + num + " " + costCounter.incrementAndGet());
        }
    }

    protected String createMessage(Level level, Date now, String type, String msg) {
        String d = dateFormat.format(now);
        return String.format(template, d, level, type, msg);
    }
}
