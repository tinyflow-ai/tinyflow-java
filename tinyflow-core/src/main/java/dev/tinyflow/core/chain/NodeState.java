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
package dev.tinyflow.core.chain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class NodeState implements Serializable {

    private String nodeId;
    private String chainInstanceId;

    protected ConcurrentHashMap<String, Object> memory = new ConcurrentHashMap<>();
    protected NodeStatus status = NodeStatus.READY;

    private int retryCount = 0;
    private int loopCount = 0;

    private AtomicInteger triggerCount = new AtomicInteger(0);
    private List<String> triggerEdgeIds = new ArrayList<>();

    private AtomicInteger executeCount = new AtomicInteger(0);
    private List<String> executeEdgeIds = new ArrayList<>();

    ExceptionSummary error;

    private long version;

    public NodeState() {
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getChainInstanceId() {
        return chainInstanceId;
    }

    public void setChainInstanceId(String chainInstanceId) {
        this.chainInstanceId = chainInstanceId;
    }

    public ConcurrentHashMap<String, Object> getMemory() {
        return memory;
    }

    public void setMemory(ConcurrentHashMap<String, Object> memory) {
        this.memory = memory;
    }

    public void addMemory(String key, Object value) {
        memory.put(key, value);
    }

    public <T> T getMemoryOrDefault(String key, T defaultValue) {
        Object value = memory.get(key);
        if (value == null) {
            return defaultValue;
        }
        //noinspection unchecked
        return (T) value;
    }

    public NodeStatus getStatus() {
        return status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public int getLoopCount() {
        return loopCount;
    }

    public void setLoopCount(int loopCount) {
        this.loopCount = loopCount;
    }

    public AtomicInteger getTriggerCount() {
        return triggerCount;
    }

    public void setTriggerCount(AtomicInteger triggerCount) {
        this.triggerCount = triggerCount;
    }

    public List<String> getTriggerEdgeIds() {
        return triggerEdgeIds;
    }

    public void setTriggerEdgeIds(List<String> triggerEdgeIds) {
        this.triggerEdgeIds = triggerEdgeIds;
    }

    public AtomicInteger getExecuteCount() {
        return executeCount;
    }

    public void setExecuteCount(AtomicInteger executeCount) {
        this.executeCount = executeCount;
    }

    public List<String> getExecuteEdgeIds() {
        return executeEdgeIds;
    }

    public void setExecuteEdgeIds(List<String> executeEdgeIds) {
        this.executeEdgeIds = executeEdgeIds;
    }

    public ExceptionSummary getError() {
        return error;
    }

    public void setError(ExceptionSummary error) {
        this.error = error;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public boolean isUpstreamFullyExecuted() {
        ChainDefinition definition = Chain.currentChain().getDefinition();
        List<Edge> inwardEdges = definition.getInwardEdge(nodeId);
        if (inwardEdges == null || inwardEdges.isEmpty()) {
            return true;
        }

        List<String> shouldBeTriggerIds = inwardEdges.stream().map(Edge::getId).collect(Collectors.toList());
        List<String> triggerEdgeIds = this.triggerEdgeIds;
        return triggerEdgeIds.size() >= shouldBeTriggerIds.size()
                && shouldBeTriggerIds.parallelStream().allMatch(triggerEdgeIds::contains);
    }

    public void recordTrigger(String fromEdgeId) {
        triggerCount.incrementAndGet();
        if (fromEdgeId == null) {
            fromEdgeId = "none";
        }
        triggerEdgeIds.add(fromEdgeId);
    }

    public void recordExecute(String fromEdgeId) {
        executeCount.incrementAndGet();
        if (fromEdgeId == null) {
            fromEdgeId = "none";
        }
        executeEdgeIds.add(fromEdgeId);
    }

    public void setStatus(NodeStatus status) {
        this.status = status;
    }

    public String getLastExecuteEdgeId() {
        if (!executeEdgeIds.isEmpty()) {
            return executeEdgeIds.get(executeEdgeIds.size() - 1);
        }
        return null;
    }
}
