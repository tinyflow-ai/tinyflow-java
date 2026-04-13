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
package dev.tinyflow.core.chain;

import dev.tinyflow.core.util.JsCodeException;
import dev.tinyflow.core.util.JsConditionUtil;
import dev.tinyflow.core.util.Maps;

import java.util.Map;

public class JsCodeCondition implements NodeCondition, EdgeCondition {
    private String code;

    public JsCodeCondition() {
    }

    public JsCodeCondition(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public boolean check(Chain chain, Edge edge, Map<String, Object> executeResult) {
        try {
            Maps map = Maps.of("_edge", edge).set("_chain", chain);
            if (executeResult != null) {
                map.putAll(executeResult);
            }
            return JsConditionUtil.eval(code, chain, map);
        } catch (Exception e) {
            throw new JsCodeException("edge check failed: " + e.getMessage(), e);
        }

    }

    @Override
    public boolean check(Chain chain, NodeState state, Map<String, Object> executeResult) {
        try {
            Maps map = Maps.of("_state", state).set("_chain", chain);
            if (executeResult != null) {
                map.putAll(executeResult);
            }
            return JsConditionUtil.eval(code, chain, map);
        } catch (Exception e) {
            throw new JsCodeException("node check failed: " + e.getMessage(), e);
        }
    }
}
