/**
 * Copyright (c) 2025-2026, Michael Yang 杨福海 (fuhai999@gmail.com).
 * <p>
 * Licensed under the GNU Lesser General Public License (LGPL) ,Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl-3.0.txt
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.tinyflow.core.util;

public class StringUtil {

    public static boolean noText(String string) {
        return !hasText(string);
    }

    public static boolean hasText(String string) {
        return string != null && !string.isEmpty() && containsText(string);
    }

    public static boolean hasText(String... strings) {
        for (String string : strings) {
            if (!hasText(string)) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsText(CharSequence str) {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    public static String getFirstWithText(String... strings) {
        if (strings == null) {
            return null;
        }
        for (String str : strings) {
            if (hasText(str)) {
                return str;
            }
        }
        return null;
    }

    /**
     * 判断字符串是否是数字
     *
     * @param string 需要判断的字符串
     * @return boolean 是数字返回 true，否则返回 false
     */
    public static boolean isNumeric(String string) {
        if (string == null || string.isEmpty()) {
            return false;
        }
        char[] chars = string.trim().toCharArray();
        for (char c : chars) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }


}
