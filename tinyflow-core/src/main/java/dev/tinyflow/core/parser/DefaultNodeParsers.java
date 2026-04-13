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
package dev.tinyflow.core.parser;

import dev.tinyflow.core.parser.impl.*;

import java.util.HashMap;
import java.util.Map;

public class DefaultNodeParsers {

    private static final Map<String, NodeParser<?>> defaultNodeParsers = new HashMap<>();

    static {
        defaultNodeParsers.put("startNode", new StartNodeParser());
        defaultNodeParsers.put("codeNode", new CodeNodeParser());
        defaultNodeParsers.put("confirmNode", new ConfirmNodeParser());

        defaultNodeParsers.put("httpNode", new HttpNodeParser());
        defaultNodeParsers.put("knowledgeNode", new KnowledgeNodeParser());
        defaultNodeParsers.put("loopNode", new LoopNodeParser());
        defaultNodeParsers.put("searchEngineNode", new SearchEngineNodeParser());
        defaultNodeParsers.put("templateNode", new TemplateNodeParser());

        defaultNodeParsers.put("endNode", new EndNodeParser());
        defaultNodeParsers.put("llmNode", new LlmNodeParser());
    }

    public static Map<String, NodeParser<?>> getDefaultNodeParsers() {
        return defaultNodeParsers;
    }

    public static void registerDefaultNodeParser(String type, NodeParser<?> nodeParser) {
        defaultNodeParsers.put(type, nodeParser);
    }

    public static void unregisterDefaultNodeParser(String type) {
        defaultNodeParsers.remove(type);
    }
}
