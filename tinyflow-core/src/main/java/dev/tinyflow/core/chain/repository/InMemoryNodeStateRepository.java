package dev.tinyflow.core.chain.repository;

import dev.tinyflow.core.chain.NodeState;
import dev.tinyflow.core.util.MapUtil;

import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryNodeStateRepository implements NodeStateRepository {

    private static final Map<String, NodeState> chainStateMap = new ConcurrentHashMap<>();

    @Override
    public NodeState load(String instanceId, String nodeId) {
        String key = instanceId + "." + nodeId;
        return MapUtil.computeIfAbsent(chainStateMap, key, k -> {
            NodeState nodeState = new NodeState();
            nodeState.setChainInstanceId(instanceId);
            nodeState.setNodeId(nodeId);
            return nodeState;
        });
    }

    @Override
    public boolean tryUpdate(NodeState newState, EnumSet<NodeStateField> fields, long version) {
        chainStateMap.put(newState.getChainInstanceId() + "." + newState.getNodeId(), newState);
        return true;
    }
}
