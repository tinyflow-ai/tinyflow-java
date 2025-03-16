package dev.tinyflow.core.provider;

import dev.tinyflow.core.knowledge.Knowledge;

public interface KnowledgeProvider {
    Knowledge getKnowledge(Object id);
}
