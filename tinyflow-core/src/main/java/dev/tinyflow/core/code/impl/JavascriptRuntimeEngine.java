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
package dev.tinyflow.core.code.impl;

import dev.tinyflow.core.chain.Chain;
import dev.tinyflow.core.code.CodeRuntimeEngine;
import dev.tinyflow.core.node.CodeNode;
import dev.tinyflow.core.util.graalvm.JsInteropUtils;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

import java.util.Map;

public class JavascriptRuntimeEngine implements CodeRuntimeEngine {

    // 使用 Context.Builder 构建上下文，线程安全
    private static final Context.Builder CONTEXT_BUILDER = Context.newBuilder("js")
            .option("engine.WarnInterpreterOnly", "false")
            .allowHostAccess(HostAccess.ALL)       // 允许访问 Java 对象的方法和字段
            .allowHostClassLookup(className -> false) // 禁止动态加载任意 Java 类
            .option("js.ecmascript-version", "2021");  // 使用较新的 ECMAScript 版本


    @Override
    public Map<String, Object> execute(String code, CodeNode node, Chain chain) {
        try (Context context = CONTEXT_BUILDER.build()) {
            Value bindings = context.getBindings("js");

            Map<String, Object> all = chain.getMemory();
            all.forEach((key, value) -> {
                if (!key.contains(".")) {
                    bindings.putMember(key, JsInteropUtils.wrapJavaValueForJS(context, value));
                }
            });

            // 注入参数
            Map<String, Object> parameterValues = chain.getParameterValues(node);
            if (parameterValues != null) {
                for (Map.Entry<String, Object> entry : parameterValues.entrySet()) {
                    bindings.putMember(entry.getKey(), JsInteropUtils.wrapJavaValueForJS(context, entry.getValue()));
                }
            }

            bindings.putMember("_chain", chain);
            bindings.putMember("_context", chain.getNodeContext(node.getId()));


            // 在 JS 中创建 _result 对象
            context.eval("js", "var _result = {};");

            // 注入 _chain 和 _context
            bindings.putMember("_chain", chain);
            bindings.putMember("_context", chain.getNodeContext(node.getId()));

            // 执行用户脚本
            context.eval("js", code);

            Value resultValue = bindings.getMember("_result");

            return GraalvmToFastJSONUtils.toJSONObject(resultValue);

        } catch (Exception e) {
            throw new RuntimeException("Polyglot JS 脚本执行失败: " + e.getMessage(), e);
        }
    }

}
