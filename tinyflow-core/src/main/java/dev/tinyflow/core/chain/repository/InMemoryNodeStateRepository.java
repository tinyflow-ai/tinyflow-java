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
