package dev.tinyflow.core.chain;

import dev.tinyflow.core.chain.event.*;
import dev.tinyflow.core.chain.repository.*;
import dev.tinyflow.core.chain.runtime.*;
import dev.tinyflow.core.util.CollectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;


public class Chain {

    private static final Logger log = LoggerFactory.getLogger(Chain.class);
    private static final ThreadLocal<Chain> EXECUTION_THREAD_LOCAL = new ThreadLocal<>();


    protected final ChainDefinition definition;
    protected final String stateInstanceId;

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
//        this.state = new ChainState();
//        this.state.setInstanceId(UUID.randomUUID().toString());
//        this.state.setChainDefinitionId(definition.getId());
    }

//    public Chain(ChainDefinition definition) {
//        this.definition = definition;

    /// /        this.state = state;
    /// /
    /// /        if (state.getChainDefinitionId() == null) {
    /// /            state.setChainDefinitionId(definition.getId());
    /// /        }
//    }
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
            NodeState nodeState = nodeStateRepository.load(stateInstanceId,nodeId );
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
        updateStateSafely(state -> {
            EnumSet<ChainStateField> fields = EnumSet.of(ChainStateField.STATUS);
            state.setStatus(ChainStatus.RUNNING);


            if (variables != null && !variables.isEmpty()) {
                state.getMemory().putAll(variables);
                fields.add(ChainStateField.MEMORY);
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
    }

    public void executeNode(Node node, String byEdgeId) {
        NodeState nodeState = getNodeState(node.getId());

        if (shouldSkipNode(node, nodeState, byEdgeId)) {
            return;
        }

        Map<String, Object> nodeResult = null;
        Throwable error = null;
        try {
            EXECUTION_THREAD_LOCAL.set(this);
            notifyEvent(new NodeStartEvent(this, node));
            nodeResult = node.execute(this);
        } catch (Throwable throwable) {
            error = throwable;
        } finally {
            EXECUTION_THREAD_LOCAL.remove();
        }

        handleNodeResult(node, nodeState, nodeResult, byEdgeId, error);
    }

    public NodeState getNodeState(String nodeId) {
        return nodeStateRepository.load(this.stateInstanceId, nodeId);
    }


    private boolean shouldSkipNode(Node node, NodeState nodeState, String edgeId) {
        synchronized (this) {
            nodeState.recordTrigger(edgeId);
            NodeCondition condition = node.getCondition();
            if (condition == null) return false;
            Map<String, Object> prevResult = Collections.emptyMap();
            return !condition.check(this, nodeState, prevResult);
        }
    }


    private void handleNodeResult(Node node, NodeState nodeState, Map<String, Object> result, String byEdigeId, Throwable error) {
        try {

            nodeState.recordExecute(byEdigeId);

            if (error == null) {
                // 成功
                nodeState.setStatus(NodeStatus.SUCCEEDED);

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
                    nodeState.setRetryCount(0);
                }

                scheduleNextForNode(node, result, byEdigeId);
            } else {
                // 挂起
                if (error instanceof ChainSuspendException) {
                    setStatusAndNotifyEvent(ChainStatus.SUSPEND);
                    nodeState.setStatus(NodeStatus.SUSPEND);
                } else {
                    // 失败
                    nodeState.setStatus(NodeStatus.ERROR);
                    nodeState.setError(new ExceptionSummary(error));
                    eventManager.notifyNodeError(error, node, result, this);

                    if (node.isRetryEnable()
                            && node.getMaxRetryCount() > 0
                            && nodeState.getRetryCount() < node.getMaxRetryCount()) {

                        nodeState.setRetryCount(nodeState.getRetryCount() + 1);

                        scheduleNode(node, byEdigeId, TriggerType.RETRY, node.getRetryIntervalMs());
                    } else {
                        handleError(error);
                    }
                }
            }
        } finally {
            notifyEvent(new NodeEndEvent(this, node, result));
        }
    }

    private void scheduleNextForNode(Node node, Map<String, Object> result, String byEdigeId) {
        if (node.isLoopEnable()) {
            NodeState nodeState = getNodeState(node.getId());
            if (node.getMaxLoopCount() > 0 && nodeState.getLoopCount() >= node.getMaxLoopCount()) {
                scheduleOutwardNodes(node, result);
                return;
            }

            NodeCondition breakCondition = node.getLoopBreakCondition();
            if (breakCondition != null && breakCondition.check(this, nodeState, result)) {
                scheduleOutwardNodes(node, result);
                return;
            }

            nodeState.setLoopCount(nodeState.getLoopCount() + 1);
            scheduleNode(node, byEdigeId, TriggerType.LOOP, node.getLoopIntervalMs());
            return;
        }

        scheduleOutwardNodes(node, result);
    }

    private void scheduleOutwardNodes(Node node, Map<String, Object> result) {
        List<Edge> edges = node.getOutwardEdges();
        if (!CollectionUtil.hasItems(edges)) {
            return;
        }

        for (Edge edge : edges) {
            EdgeCondition cond = edge.getCondition();
            if (cond == null || cond.check(this, edge, result)) {
                Node next = definition.getNodeById(edge.getTarget());
                if (next != null) {
                    scheduleNode(next, edge.getId(), TriggerType.NEXT, 0L);
                }
            }
        }
    }


    public void scheduleNode(Node node, String edgeId, TriggerType type, long delayMs) {
        scheduleNode(node, this.stateInstanceId, null, edgeId, type, null, delayMs);
    }

    public void scheduleNode(Node node, String stateInstanceId, String parentInstanceId, String edgeId, TriggerType type, Map<String, Object> payload, long delayMs) {
        Trigger prevTrigger = TriggerContext.getCurrentTrigger();
        if (parentInstanceId == null && prevTrigger != null) {
            parentInstanceId = prevTrigger.getStateInstanceId();
        }

        Trigger trigger = new Trigger();
        trigger.setStateInstanceId(stateInstanceId);
        trigger.setParentInstanceId(parentInstanceId);
        trigger.setEdgeId(edgeId);
        trigger.setNodeId(node.getId());
        trigger.setType(type);
        trigger.setPayload(payload);
        trigger.setTriggerAt(System.currentTimeMillis() + delayMs);
        getTriggerScheduler().schedule(trigger);
    }


    private void handleError(Throwable throwable) {
        updateStateSafely(state -> {
            state.setError(new ExceptionSummary(throwable));
            return EnumSet.of(ChainStateField.ERROR);
        });

        if (throwable instanceof ChainSuspendException) {
            setStatusAndNotifyEvent(ChainStatus.SUSPEND);
        } else {
            setStatusAndNotifyEvent(ChainStatus.FAILED);
            eventManager.notifyChainError(throwable, this);
        }
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

//    public void saveOrUpdateState() {
//        if (chainStateRepository != null) {
//            chainStateRepository.tryUpdate(state, EnumSet.of(ChainStateField.STATUS));
//        }
//    }


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

    public void success(String message) {
        updateStateSafely(state -> {
            state.setMessage(message);
            return EnumSet.of(ChainStateField.MESSAGE);
        });
        setStatusAndNotifyEvent(ChainStatus.SUCCEEDED);
    }

    public void failed(String message) {
        updateStateSafely(state -> {
            state.setMessage(message);
            return EnumSet.of(ChainStateField.MESSAGE);
        });
        setStatusAndNotifyEvent(ChainStatus.FAILED);
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
}