package dev.tinyflow.core.chain.repository;

import dev.tinyflow.core.chain.ChainState;
import dev.tinyflow.core.util.MapUtil;

import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryChainStateRepository implements ChainStateRepository {
    private static final Map<String, ChainState> chainStateMap = new ConcurrentHashMap<>();

    @Override
    public ChainState load(String instanceId) {
        return MapUtil.computeIfAbsent(chainStateMap, instanceId, k -> new ChainState());
    }

    @Override
    public boolean tryUpdate(ChainState chainState, EnumSet<ChainStateField> fields) {
        chainStateMap.put(chainState.getInstanceId(), chainState);
        return true;
    }
}
