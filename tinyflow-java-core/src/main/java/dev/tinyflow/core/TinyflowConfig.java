package dev.tinyflow.core;

import dev.tinyflow.core.parser.NodeParser;
import dev.tinyflow.core.parser.impl.*;

import java.util.HashMap;
import java.util.Map;

public class TinyflowConfig {

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
