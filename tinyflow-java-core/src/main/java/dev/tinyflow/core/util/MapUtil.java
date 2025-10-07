/*
 *  Copyright (c) 2023-2025, Agents-Flex (fuhai999@gmail.com).
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package dev.tinyflow.core.util;


import com.alibaba.fastjson.JSONPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;

public class MapUtil {
    private static final Logger log = LoggerFactory.getLogger(MapUtil.class);

    private static final boolean IS_JDK8 = (8 == getJvmVersion0());

    private MapUtil() {
    }

    private static String tryTrim(String string) {
        return string != null ? string.trim() : "";
    }

    private static int getJvmVersion0() {
        int jvmVersion = -1;
        try {
            String javaSpecVer = tryTrim(System.getProperty("java.specification.version"));
            if (StringUtil.hasText(javaSpecVer)) {
                if (javaSpecVer.startsWith("1.")) {
                    javaSpecVer = javaSpecVer.substring(2);
                }
                if (javaSpecVer.indexOf('.') == -1) {
                    jvmVersion = Integer.parseInt(javaSpecVer);
                }
            }
        } catch (Throwable ignore) {
            // ignore
        }
        // default is jdk8
        if (jvmVersion == -1) {
            jvmVersion = 8;
        }
        return jvmVersion;
    }

    /**
     * A temporary workaround for Java 8 specific performance issue JDK-8161372 .<br>
     * This class should be removed once we drop Java 8 support.
     *
     * @see <a href=
     * "https://bugs.openjdk.java.net/browse/JDK-8161372">https://bugs.openjdk.java.net/browse/JDK-8161372</a>
     */
    public static <K, V> V computeIfAbsent(Map<K, V> map, K key, Function<K, V> mappingFunction) {
        if (IS_JDK8) {
            V value = map.get(key);
            if (value != null) {
                return value;
            }
        }
        return map.computeIfAbsent(key, mappingFunction);
    }

    public static Object getByPath(Map<String, Object> from, String keyOrPath) {
        if (StringUtil.noText(keyOrPath)) {
            return null;
        }

        Object result = from.get(keyOrPath);
        if (result != null) {
            return result;
        }

        List<String> parts = Arrays.asList(keyOrPath.split("\\."));
        if (parts.isEmpty()) {
            return null;
        }

        int matchedLevels = 0;
        for (int i = parts.size(); i > 0; i--) {
            String tryKey = String.join(".", parts.subList(0, i));
            Object tempResult = from.get(tryKey);
            if (tempResult != null) {
                result = tempResult;
                matchedLevels = i;
                break;
            }
        }

        if (result == null) {
            return null;
        }

        if (result instanceof Collection) {
            List<Object> results = new ArrayList<>();
            for (Object item : ((Collection<?>) result)) {
                results.add(getResult(parts, matchedLevels, item));
            }
            return results;
        }

        return getResult(parts, matchedLevels, result);
    }

    private static Object getResult(List<String> parts, int matchedLevels, Object result) {
        List<String> remainingParts = parts.subList(matchedLevels, parts.size());
        String jsonPath = "$." + String.join(".", remainingParts);
        try {
            return JSONPath.eval(result, jsonPath);
        } catch (Exception e) {
            log.error(e.toString(), e);
        }

        return null;
    }

}
