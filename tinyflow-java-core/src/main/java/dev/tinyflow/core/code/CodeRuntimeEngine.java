package dev.tinyflow.core.code;

import dev.tinyflow.core.chain.Chain;
import dev.tinyflow.core.node.CodeNode;

import java.util.Map;

public interface CodeRuntimeEngine {
    Map<String, Object> execute(String code, CodeNode node, Chain chain);
}
