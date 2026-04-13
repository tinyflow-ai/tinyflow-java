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
package dev.tinyflow.core.code;

import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.InitOptions;
import com.alibaba.qlexpress4.QLOptions;
import dev.tinyflow.core.chain.Chain;
import dev.tinyflow.core.node.CodeNode;

import java.util.HashMap;
import java.util.Map;

public class QLExpressRuntimeEngine implements CodeRuntimeEngine {
    @Override
    public Map<String, Object> execute(String code, CodeNode node, Chain chain) {
        Express4Runner runner = new Express4Runner(InitOptions.DEFAULT_OPTIONS);
        Map<String, Object> context = new HashMap<>();

        Map<String, Object> parameterValues = chain.getState().resolveParameters(node);
        if (parameterValues != null) context.putAll(parameterValues);

        Map<String, Object> result = new HashMap<>();
        context.put("_result", result);
        context.put("_chain", chain);
        context.put("_state", chain.getNodeState(node.getId()));

        try {
            runner.execute(code, context, QLOptions.DEFAULT_OPTIONS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}
