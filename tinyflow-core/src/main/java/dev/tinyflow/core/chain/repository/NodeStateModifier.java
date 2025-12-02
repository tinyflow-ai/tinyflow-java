package dev.tinyflow.core.chain.repository;

import dev.tinyflow.core.chain.NodeState;

import java.util.EnumSet;

public interface NodeStateModifier {

    EnumSet<NodeStateField> modify(NodeState state);
}
