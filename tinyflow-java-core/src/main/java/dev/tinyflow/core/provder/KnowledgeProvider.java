package dev.tinyflow.core.provder;

import dev.tinyflow.core.knowledge.Knowledge;

public interface KnowledgeProvider {
    Knowledge getKnowledge(Object id);
}
