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

public class Utils {

    /**
     * Get a value that might be configured externally. Looks first for a system property
     * (a -D option specified to Java), if not present looks for an environment variable
     * and if not present then falls back to the specified default.
     *
     * @param name         The system property or environment variable name
     * @param defaultValue The value to fall back to.
     * @return
     */
    public static String getConfiguration(String name, String defaultValue) {
        String s = System.getProperty(name);
        if (s != null && s.length() > 0) {
            return s;
        }
        s = System.getenv(name);
        if (s != null && s.length() > 0) {
            return s;
        }
        return defaultValue;

    }

}
