package dev.tinyflow.core.chain.repository;

import dev.tinyflow.core.chain.ChainState;

import java.util.EnumSet;

public interface ChainStateModifier {

    EnumSet<ChainStateField> modify(ChainState state);
}
