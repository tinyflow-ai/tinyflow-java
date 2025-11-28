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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class NodeState {

    private String nodeId;

    protected ConcurrentHashMap<String, Object> memory = new ConcurrentHashMap<>();
    protected NodeStatus nodeStatus = NodeStatus.READY;

    private String fromEdgeId;

    private int retryCount = 0;


    private AtomicInteger triggerCount = new AtomicInteger(0);
    private List<String> triggerEdgeIds = new ArrayList<>();

    private AtomicInteger executeCount = new AtomicInteger(0);
    private List<String> executeEdgeIds = new ArrayList<>();

    ExceptionSummary error;

    public NodeState() {
    }

    public NodeState(String nodeId) {
        this.nodeId = nodeId;
    }


    public String getFromEdgeId() {
        return fromEdgeId;
    }

    public int getTriggerCount() {
        return triggerCount.get();
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public List<String> getTriggerEdgeIds() {
        return triggerEdgeIds;
    }

    public int getExecuteCount() {
        return executeCount.get();
    }

    public List<String> getExecuteEdgeIds() {
        return executeEdgeIds;
    }

    public void setFromEdgeId(String fromEdgeId) {
        this.fromEdgeId = fromEdgeId;
    }

    public void setTriggerCount(AtomicInteger triggerCount) {
        this.triggerCount = triggerCount;
    }

    public void setTriggerEdgeIds(List<String> triggerEdgeIds) {
        this.triggerEdgeIds = triggerEdgeIds;
    }

    public void setExecuteCount(AtomicInteger executeCount) {
        this.executeCount = executeCount;
    }

    public void setExecuteEdgeIds(List<String> executeEdgeIds) {
        this.executeEdgeIds = executeEdgeIds;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public ConcurrentHashMap<String, Object> getMemory() {
        return memory;
    }

    public void setMemory(ConcurrentHashMap<String, Object> memory) {
        this.memory = memory;
    }

    public NodeStatus getNodeStatus() {
        return nodeStatus;
    }

    public ExceptionSummary getError() {
        return error;
    }

    public void setError(ExceptionSummary error) {
        this.error = error;
    }

    public boolean isUpstreamFullyExecuted() {
        Node currentNode = Chain.currentChain().getDefinition().getNodeById(nodeId);
        List<Edge> inwardEdges = currentNode.getInwardEdges();
        if (inwardEdges == null || inwardEdges.isEmpty()) {
            return true;
        }

        List<String> shouldBeTriggerIds = inwardEdges.stream().map(Edge::getId).collect(Collectors.toList());
        return triggerEdgeIds.size() >= shouldBeTriggerIds.size()
            && shouldBeTriggerIds.parallelStream().allMatch(triggerEdgeIds::contains);
    }

    public void recordTrigger(String fromEdgeId) {
        triggerCount.incrementAndGet();
        triggerEdgeIds.add(fromEdgeId);
    }

    public synchronized void recordExecute(String fromEdgeId) {
        executeCount.incrementAndGet();
        executeEdgeIds.add(fromEdgeId);
    }

    public void setNodeStatus(NodeStatus nodeStatus) {
        this.nodeStatus = nodeStatus;
    }

    public void setNodeStatusFinished() {
        if (this.nodeStatus == NodeStatus.ERROR) {
            this.setNodeStatus(NodeStatus.FAILED);
        } else {
            this.setNodeStatus(NodeStatus.SUCCEEDED);
        }
    }
}
