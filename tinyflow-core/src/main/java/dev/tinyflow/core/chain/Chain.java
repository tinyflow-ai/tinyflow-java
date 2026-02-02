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

import dev.tinyflow.core.chain.event.*;
import dev.tinyflow.core.chain.repository.*;
import dev.tinyflow.core.chain.runtime.*;
import dev.tinyflow.core.util.CollectionUtil;
import dev.tinyflow.core.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;


public class Chain {

    private static final Logger log = LoggerFactory.getLogger(Chain.class);
    private static final ThreadLocal<Chain> EXECUTION_THREAD_LOCAL = new ThreadLocal<>();


    protected final ChainDefinition definition;
    protected String stateInstanceId;

    //    protected final ChainState state;
    protected ChainStateRepository chainStateRepository;
    protected NodeStateRepository nodeStateRepository;
    protected EventManager eventManager;
    protected TriggerScheduler triggerScheduler;

    public static Chain currentChain() {
        return EXECUTION_THREAD_LOCAL.get();
    }

    public Chain(ChainDefinition definition, String stateInstanceId) {
        this.definition = definition;
        this.stateInstanceId = stateInstanceId;
    }

    public void notifyEvent(Event event) {
        eventManager.notifyEvent(event, this);
    }

    public void setStatusAndNotifyEvent(ChainStatus status) {
        AtomicReference<ChainStatus> before = new AtomicReference<>();
        updateStateSafely(state -> {
            before.set(state.getStatus());
            state.setStatus(status);
            return EnumSet.of(ChainStateField.STATUS);
        });
        notifyEvent(new ChainStatusChangeEvent(this, status, before.get()));
    }

    public void setStatusAndNotifyEvent(String stateInstanceId, ChainStatus status) {
        AtomicReference<ChainStatus> before = new AtomicReference<>();
        updateStateSafely(stateInstanceId, state -> {
            before.set(state.getStatus());
            state.setStatus(status);
            return EnumSet.of(ChainStateField.STATUS);
        });
        notifyEvent(new ChainStatusChangeEvent(this, status, before.get()));
    }

    /**
     * Safely updates the chain state with optimistic locking and retry-on-conflict.
     *
     * @param modifier the modifier that applies changes and declares updated fields
     * @throws ChainUpdateTimeoutException if update cannot succeed within timeout
     */
    public ChainState updateStateSafely(ChainStateModifier modifier) {
        return updateStateSafely(this.stateInstanceId, modifier);
    }


    public ChainState updateStateSafely(String stateInstanceId, ChainStateModifier modifier) {
        final long timeoutMs = 30_000; // 30 seconds total timeout
        final long maxRetryDelayMs = 100; // Maximum delay between retries

        long startTime = System.currentTimeMillis();
        int attempt = 0;
        ChainState current = null;
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            current = chainStateRepository.load(stateInstanceId);
            if (current == null) {
                throw new IllegalStateException("Chain state not found: " + stateInstanceId);
            }

            EnumSet<ChainStateField> updatedFields = modifier.modify(current);
            if (updatedFields.isEmpty()) {
                return current; // No actual changes, exit early
            }

            if (chainStateRepository.tryUpdate(current, updatedFields)) {
                return current;
            }

            // Prepare next retry
            attempt++;
            long nextDelay = calculateNextRetryDelay(attempt, maxRetryDelayMs);
            sleepUninterruptibly(nextDelay);
        }

        // Timeout reached
        assert current != null;
        String msg = String.format(
                "Chain state update timeout after %d ms (instanceId: %s)",
                timeoutMs, current.getInstanceId()
        );
        log.warn(msg);
        throw new ChainUpdateTimeoutException(msg);
    }


    public NodeState updateNodeStateSafely(String nodeId, NodeStateModifier modifier) {
        return this.updateNodeStateSafely(this.stateInstanceId, nodeId, modifier);
    }

    public NodeState updateNodeStateSafely(String stateInstanceId, String nodeId, NodeStateModifier modifier) {
        final long timeoutMs = 30_000;
        final long maxRetryDelayMs = 100;
        long startTime = System.currentTimeMillis();
        int attempt = 0;

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            // 1. 加载最新 ChainState（获取 chainVersion）
            ChainState chainState = chainStateRepository.load(stateInstanceId);
            if (chainState == null) {
                throw new IllegalStateException("Chain state not found");
            }

            // 2. 加载 NodeState
            NodeState nodeState = nodeStateRepository.load(stateInstanceId, nodeId);
            if (nodeState == null) {
                nodeState = new NodeState();
                nodeState.setChainInstanceId(chainState.getInstanceId());
                nodeState.setNodeId(nodeId);
            }

            // 3. 应用修改
            EnumSet<NodeStateField> updatedFields = modifier.modify(nodeState);

            if (updatedFields.isEmpty()) {
                return nodeState;
            }

            // 4. 尝试更新（传入 chainVersion 保证一致性）
            if (nodeStateRepository.tryUpdate(nodeState, updatedFields, chainState.getVersion())) {
                return nodeState;
            }

            // 5. 退避重试
            attempt++;
            sleepUninterruptibly(calculateNextRetryDelay(attempt, maxRetryDelayMs));
        }

        throw new ChainUpdateTimeoutException("Node state update timeout");
    }


    /**
     * Calculates the next retry delay using exponential backoff with jitter.
     *
     * @param attempt    the current retry attempt (1-based)
     * @param maxDelayMs the maximum delay in milliseconds
     * @return the delay in milliseconds to wait before next retry
     */
    private long calculateNextRetryDelay(int attempt, long maxDelayMs) {
        // Base delay: 10ms * (2^(attempt-1))
        long baseDelay = 10L * (1L << (attempt - 1));

        // Add jitter: ±25% randomness to avoid thundering herd
        double jitterFactor = 0.75 + (Math.random() * 0.5); // [0.75, 1.25)
        long delayWithJitter = (long) (baseDelay * jitterFactor);

        // Clamp between 1ms and maxDelayMs
        return Math.max(1L, Math.min(delayWithJitter, maxDelayMs));
    }

    /**
     * Sleeps for the specified duration, silently ignoring interrupts
     * but preserving the interrupt status.
     *
     * @param millis the length of time to sleep in milliseconds
     */
    private void sleepUninterruptibly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Preserve interrupt status
            // Do NOT throw here — we want to continue retrying
        }
    }


    public void start(Map<String, Object> variables) {
        Trigger prev = TriggerContext.getCurrentTrigger();
        try {
            // start 可能在 node 里执行一个新的 chain 的情况，
            // 需要清空父级 chain 的 Trigger
            TriggerContext.setCurrentTrigger(null);
            updateStateSafely(state -> {
                EnumSet<ChainStateField> fields = EnumSet.of(ChainStateField.STATUS);
                state.setStatus(ChainStatus.RUNNING);

                if (variables != null && !variables.isEmpty()) {
                    state.getMemory().putAll(variables);
                    fields.add(ChainStateField.MEMORY);
                }

                if (StringUtil.noText(state.getChainDefinitionId())) {
                    state.setChainDefinitionId(definition.getId());
                    fields.add(ChainStateField.CHAIN_DEFINITION_ID);
                }

                return fields;
            });

            notifyEvent(new ChainStartEvent(this, variables));
            setStatusAndNotifyEvent(ChainStatus.RUNNING);

            // 调度入口节点
            List<Node> startNodes = definition.getStartNodes();
            for (Node startNode : startNodes) {
                scheduleNode(startNode, null, TriggerType.NEXT, 0);
            }
        } finally {
            // 恢复父级 chain 的 Trigger
            TriggerContext.setCurrentTrigger(prev);
        }
    }

    public void executeNode(Node node, String byEdgeId) {
        try {
            EXECUTION_THREAD_LOCAL.set(this);
            ChainState chainState = getState();

            // 当前处于挂起状态
            if (chainState.getStatus() == ChainStatus.SUSPEND) {
                updateStateSafely(state -> {
                    chainState.addSuspendNodeId(node.getId());
                    return EnumSet.of(ChainStateField.SUSPEND_NODE_IDS);
                });
                return;
            }
            // 处于非运行状态，比如错误状态
            else if (chainState.getStatus() != ChainStatus.RUNNING) {
                return;
            }

            if (shouldSkipNode(node, byEdgeId)) {
                return;
            }

            Map<String, Object> nodeResult = null;
            Throwable error = null;
            try {
                updateNodeStateSafely(node.id, s -> {
                    s.setStatus(NodeStatus.RUNNING);
                    s.recordExecute(byEdgeId);
                    return EnumSet.of(NodeStateField.EXECUTE_COUNT, NodeStateField.EXECUTE_EDGE_IDS, NodeStateField.STATUS);
                });

                notifyEvent(new NodeStartEvent(this, node));
                nodeResult = node.execute(this);
            } catch (Throwable throwable) {
                log.error("Node execute error", throwable);
                error = throwable;
            }
            handleNodeResult(node, nodeResult, byEdgeId, error);
        } finally {
            EXECUTION_THREAD_LOCAL.remove();
        }
    }

    public NodeState getNodeState(String nodeId) {
        return getNodeState(this.stateInstanceId, nodeId);
    }

    public NodeState getNodeState(String stateInstanceId, String nodeId) {
        return nodeStateRepository.load(stateInstanceId, nodeId);
    }

    public <T> T executeWithLock(String instanceId, long timeout, TimeUnit unit, Supplier<T> action) {
        try (ChainLock lock = chainStateRepository.getLock(instanceId, timeout, unit)) {
            if (!lock.isAcquired()) {
                throw new ChainLockTimeoutException("Failed to acquire lock for instance: " + instanceId);
            }
            return action.get();
        }
    }

    private boolean shouldSkipNode(Node node, String edgeId) {
        return executeWithLock(stateInstanceId, 5, TimeUnit.SECONDS, () -> {
            NodeState newState = updateNodeStateSafely(node.id, s -> {
                s.recordTrigger(edgeId);
                return EnumSet.of(NodeStateField.TRIGGER_COUNT, NodeStateField.TRIGGER_EDGE_IDS);
            });

            NodeCondition condition = node.getCondition();
            if (condition == null) {
                return false;
            }
            Map<String, Object> prevResult = Collections.emptyMap();
            return !condition.check(this, newState, prevResult);
        });
    }


    private void handleNodeResult(Node node, Map<String, Object> result, String byEdgeId, Throwable error) {
        ChainStatus finalStatus = null;
        NodeStatus finalNodeStatus = null;
        try {
            if (error == null) {
                // 更新 state 数据
                updateStateSafely(state -> {
                    EnumSet<ChainStateField> fields = EnumSet.of(ChainStateField.EXECUTE_RESULT);
                    state.setExecuteResult(result);

                    if (result != null && !result.isEmpty()) {
                        result.forEach((k, v) -> {
                            if (v != null) {
                                state.getMemory().put(node.getId() + "." + k, v);
                            }
                        });
                        fields.add(ChainStateField.MEMORY);
                    }

                    return fields;
                });

                if (node.isRetryEnable() && node.isResetRetryCountAfterNormal()) {
                    updateNodeStateSafely(node.id, state -> {
                        state.setRetryCount(0);
                        return EnumSet.of(NodeStateField.RETRY_COUNT);
                    });
                }

                finalNodeStatus = result == null ? null : (NodeStatus) result.get(ChainConsts.NODE_STATE_STATUS_KEY);

                // 不调度下一个节点，由 node 自行调度，比如 Loop 循环
                Boolean scheduleNextNodeDisabled = result == null ? null : (Boolean) result.get(ChainConsts.SCHEDULE_NEXT_NODE_DISABLED_KEY);
                if (scheduleNextNodeDisabled != null && scheduleNextNodeDisabled) {
                    return;
                }

                // 结束节点
                finalStatus = result != null ? (ChainStatus) result.get(ChainConsts.CHAIN_STATE_STATUS_KEY) : null;
                if (finalStatus != null && finalStatus.isTerminal()) {
                    return;
                }

                scheduleNextForNode(node, result, byEdgeId);
            } else {
                // 挂起
                if (error instanceof ChainSuspendException) {
                    updateNodeStateSafely(node.id, s -> {
                        s.setStatus(NodeStatus.SUSPEND);
                        return EnumSet.of(NodeStateField.STATUS);
                    });

                    updateStateSafely(s -> {
                        s.addSuspendNodeId(node.getId());
                        s.addSuspendForParameters(((ChainSuspendException) error).getSuspendParameters());
                        return EnumSet.of(ChainStateField.SUSPEND_NODE_IDS, ChainStateField.SUSPEND_FOR_PARAMETERS);
                    });

                    finalStatus = ChainStatus.SUSPEND;
                }
                // 失败
                else {
                    NodeState newState = updateNodeStateSafely(node.getId(), s -> {
                        s.setStatus(NodeStatus.ERROR);
                        s.setError(new ExceptionSummary(error));
                        return EnumSet.of(NodeStateField.ERROR, NodeStateField.STATUS);
                    });

                    eventManager.notifyNodeError(error, node, result, this);

                    if (node.isRetryEnable()
                            && node.getMaxRetryCount() > 0
                            && newState.getRetryCount() < node.getMaxRetryCount()) {

                        updateNodeStateSafely(node.getId(), s -> {
                            s.setRetryCount(s.getRetryCount() + 1);
                            return EnumSet.of(NodeStateField.RETRY_COUNT);
                        });

                        scheduleNode(node, byEdgeId, TriggerType.RETRY, node.getRetryIntervalMs());
                    } else {
                        finalStatus = handleNodeError(node.id, error);
                    }
                }
            }
        } finally {
            // 如果不是还在执行中的状态，则通知事件
            if (finalNodeStatus != NodeStatus.RUNNING) {
                NodeStatus nodeStatus = finalNodeStatus == null ? NodeStatus.SUCCEEDED : finalNodeStatus;
                updateNodeStateSafely(node.id, state -> {
                    state.setStatus(nodeStatus);
                    return EnumSet.of(NodeStateField.STATUS);
                });
                notifyEvent(new NodeEndEvent(this, node, result, error));
            }

            if (finalStatus != null) {
                setStatusAndNotifyEvent(finalStatus);

                // chain 执行结束
                if (finalStatus.isTerminal()) {
                    eventManager.notifyEvent(new ChainEndEvent(this), this);

                    // 执行结束，但是未执行成功，失败和取消等
                    // 更新父级链的状态
                    if (!finalStatus.isSuccess()) {
                        ChainState currentState = getState();
                        ChainStatus currentStatus = finalStatus;
                        while (currentState != null && StringUtil.hasText(currentState.getParentInstanceId())) {
                            updateStateSafely(currentState.getParentInstanceId(), state -> {
                                state.setStatus(currentStatus);
                                return EnumSet.of(ChainStateField.STATUS);
                            });
                            setStatusAndNotifyEvent(currentState.getParentInstanceId(), currentStatus);
                            currentState = getState(currentState.getParentInstanceId());
                        }
                    }
                }
            }
        }
    }

    /**
     * 为指定节点调度下一次执行
     *
     * @param node      要调度的节点
     * @param result    节点执行结果
     * @param byEdigeId 触发边的ID
     */
    private void scheduleNextForNode(Node node, Map<String, Object> result, String byEdigeId) {
        // 如果节点不支持循环，则直接调度向外的节点
        if (!node.isLoopEnable()) {
            scheduleOutwardNodes(node, result);
            return;
        }

        NodeState nodeState = getNodeState(node.getId());
        // 如果达到最大循环次数限制，则调度向外的节点
        if (node.getMaxLoopCount() > 0 && nodeState.getLoopCount() >= node.getMaxLoopCount()) {
            scheduleOutwardNodes(node, result);
            return;
        }

        // 检查循环中断条件，如果满足则调度向外的节点
        NodeCondition breakCondition = node.getLoopBreakCondition();
        if (breakCondition != null && breakCondition.check(this, nodeState, result)) {
            scheduleOutwardNodes(node, result);
            return;
        }

        // 增加循环计数并重新调度当前节点
        updateNodeStateSafely(node.getId(), s -> {
            s.setLoopCount(s.getLoopCount() + 1);
            return EnumSet.of(NodeStateField.LOOP_COUNT);
        });

        scheduleNode(node, byEdigeId, TriggerType.LOOP, node.getLoopIntervalMs());
    }


    private void scheduleOutwardNodes(Node node, Map<String, Object> result) {
        List<Edge> edges = definition.getOutwardEdge(node.getId());
        if (!CollectionUtil.hasItems(edges)) {
            // 当前节点没有向外的边，则调度父节点（自动回归父节点） 用在 Loop 循环等场景
            if (StringUtil.hasText(node.getParentId())) {
                Node parent = definition.getNodeById(node.getParentId());
                scheduleNode(parent, null, TriggerType.NEXT, 0L);
            }
            return;
        }

        for (Edge edge : edges) {
            EdgeCondition cond = edge.getCondition();
            if (cond == null || cond.check(this, edge, result)) {
                Node next = definition.getNodeById(edge.getTarget());
                if (next != null && isSameParent(node, next)) {
                    scheduleNode(next, edge.getId(), TriggerType.NEXT, 0L);
                }
            }
        }
    }

    /**
     * 判断两个节点是否具有相同的父节点
     *
     * @param node 第一个节点
     * @param next 第二个节点
     * @return 如果两个节点的父节点ID相同则返回true，否则返回false
     */
    private boolean isSameParent(Node node, Node next) {
        // 如果两个节点的父节点ID都为空或空白，则认为是相同父节点
        if (StringUtil.noText(node.getParentId()) && StringUtil.noText(next.getParentId())) {
            return true;
        }

        // 比较两个节点的父节点ID是否相等
        return node.getParentId() != null && node.getParentId().equals(next.getParentId());
    }


    public void scheduleNode(Node node, String edgeId, TriggerType type, long delayMs) {
        Trigger prevTrigger = TriggerContext.getCurrentTrigger();
        Map<String, Object> payload = prevTrigger == null ? null : prevTrigger.getPayload();
        Trigger parent = prevTrigger == null ? null : prevTrigger.getParent();
        String stateInstanceId = prevTrigger == null ? this.stateInstanceId : prevTrigger.getStateInstanceId();
        scheduleNode(node, stateInstanceId, edgeId, type, null, payload, parent, delayMs);
    }

    public void scheduleNode(Node node, String stateInstanceId, String edgeId,
                             TriggerType type, Map<String, Object> variables, Map<String, Object> payload, Trigger parent, long delayMs) {

        Trigger trigger = new Trigger();
        trigger.setStateInstanceId(stateInstanceId);
        trigger.setEdgeId(edgeId);
        trigger.setNodeId(node.getId());
        trigger.setType(type);
        trigger.setVariables(variables);
        trigger.setTriggerAt(System.currentTimeMillis() + delayMs);
        trigger.setPayload(payload);
        trigger.setParent(parent);

        getTriggerScheduler().schedule(trigger);
    }


    private ChainStatus handleNodeError(String nodeId, Throwable throwable) {
        updateNodeStateSafely(nodeId, s -> {
            s.setStatus(NodeStatus.FAILED);
            s.setError(new ExceptionSummary(throwable));
            return EnumSet.of(NodeStateField.ERROR, NodeStateField.STATUS);
        });

        updateStateSafely(state -> {
            state.setError(new ExceptionSummary(throwable));
            return EnumSet.of(ChainStateField.ERROR);
        });

        setStatusAndNotifyEvent(ChainStatus.FAILED);
        eventManager.notifyChainError(throwable, this);
        return ChainStatus.FAILED;
    }

    public void suspend() {
        setStatusAndNotifyEvent(ChainStatus.SUSPEND);
    }

    public void suspend(Node node) {
        updateStateSafely(state -> {
            state.addSuspendNodeId(node.getId());
            return EnumSet.of(ChainStateField.SUSPEND_NODE_IDS);
        });
        setStatusAndNotifyEvent(ChainStatus.SUSPEND);
    }


    public void resume(Map<String, Object> variables) {
        ChainState newState = updateStateSafely(state -> {
            if (variables != null) {
                state.getMemory().putAll(variables);
                return EnumSet.of(ChainStateField.MEMORY);
            } else {
                return EnumSet.noneOf(ChainStateField.class);
            }
        });


        notifyEvent(new ChainResumeEvent(this, variables));
        setStatusAndNotifyEvent(ChainStatus.RUNNING);

        Set<String> suspendNodeIds = newState.getSuspendNodeIds();
        if (suspendNodeIds != null && !suspendNodeIds.isEmpty()) {
            for (String id : suspendNodeIds) {
                Node node = definition.getNodeById(id);
                if (node != null) {
                    NodeState nodeState = getNodeState(node.getId());
                    String edgeId = nodeState.getLastExecuteEdgeId();
                    scheduleNode(node, edgeId, TriggerType.MANUAL, 0L);
                }
            }
        }
    }

    public void resume() {
        resume(Collections.emptyMap());
    }

    public void output(Node node, Object response) {
        eventManager.notifyOutput(this, node, response);
    }


    public EventManager getEventManager() {
        return eventManager;
    }

    public void setEventManager(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    public ChainStateRepository getChainStateRepository() {
        return chainStateRepository;
    }

    public void setChainStateRepository(ChainStateRepository chainStateRepository) {
        this.chainStateRepository = chainStateRepository;
    }

    public ChainDefinition getDefinition() {
        return definition;
    }

    public TriggerScheduler getTriggerScheduler() {
        if (this.triggerScheduler == null) {
            this.triggerScheduler = ChainRuntime.triggerScheduler();
        }
        return triggerScheduler;
    }

    public void setTriggerScheduler(TriggerScheduler triggerScheduler) {
        this.triggerScheduler = triggerScheduler;
    }

    public NodeStateRepository getNodeStateRepository() {
        return nodeStateRepository;
    }

    public void setNodeStateRepository(NodeStateRepository nodeStateRepository) {
        this.nodeStateRepository = nodeStateRepository;
    }

    public String getStateInstanceId() {
        return stateInstanceId;
    }

    public ChainState getState() {
        return chainStateRepository.load(stateInstanceId);
    }

    public ChainState getState(String stateInstanceId) {
        return chainStateRepository.load(stateInstanceId);
    }

    public void setStateInstanceId(String stateInstanceId) {
        this.stateInstanceId = stateInstanceId;
    }
}