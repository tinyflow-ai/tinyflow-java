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

import com.alibaba.fastjson.JSON;
import dev.tinyflow.core.chain.event.*;
import dev.tinyflow.core.chain.listener.ChainErrorListener;
import dev.tinyflow.core.chain.listener.ChainEventListener;
import dev.tinyflow.core.chain.listener.ChainOutputListener;
import dev.tinyflow.core.chain.listener.NodeErrorListener;
import dev.tinyflow.core.chain.repository.ChainDefinitionRepository;
import dev.tinyflow.core.chain.repository.ChainStateRepository;
import dev.tinyflow.core.chain.runtime.ChainRuntime;
import dev.tinyflow.core.chain.runtime.LoopScheduler;
import dev.tinyflow.core.util.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Phaser;
import java.util.stream.Collectors;


public class Chain {
    private static final ThreadLocal<Chain> EXECUTION_THREAD_LOCAL = new ThreadLocal<>();

    protected final ChainDefinition definition;
    protected final ChainState state;
    protected ChainStateRepository stateRepository;
    protected ChainDefinitionRepository definitionRepository;

    protected EventManager eventManager = new EventManager();
    protected ExecutorService asyncNodeExecutors = NamedThreadPools.newFixedThreadPool("chain-executor");
    protected Phaser phaser = new Phaser(1);
    protected Throwable error;

    public static Chain currentChain() {
        return EXECUTION_THREAD_LOCAL.get();
    }

    public Chain(ChainDefinition definition) {
        this.definition = definition;
        this.state = new ChainState();
        this.state.setInstanceId(UUID.randomUUID().toString());
        this.state.setChainDefinitionId(definition.getId());
    }


    public Chain(ChainDefinition definition, ChainState state) {
        this.definition = definition;
        this.state = state;
    }


    public synchronized void addEventListener(Class<? extends Event> eventClass, ChainEventListener listener) {
        eventManager.addEventListener(eventClass, listener);
    }

    public synchronized void addEventListener(ChainEventListener listener) {
        eventManager.addEventListener(listener);
    }


    public synchronized void removeEventListener(ChainEventListener listener) {
        eventManager.removeEventListener(listener);
    }

    public synchronized void removeEventListener(Class<? extends Event> eventClass, ChainEventListener listener) {
        eventManager.removeEventListener(eventClass, listener);
    }

    public synchronized void addErrorListener(ChainErrorListener listener) {
        eventManager.addChainErrorListener(listener);
    }

    public synchronized void removeErrorListener(ChainErrorListener listener) {
        eventManager.removeChainErrorListener(listener);
    }

    public synchronized void addNodeErrorListener(NodeErrorListener listener) {
        eventManager.addNodeErrorListener(listener);
    }

    public synchronized void removeNodeErrorListener(NodeErrorListener listener) {
        eventManager.removeNodeErrorListener(listener);
    }


    public void addOutputListener(ChainOutputListener outputListener) {
        eventManager.addOutputListener(outputListener);
    }

    public void setStatusAndNotifyEvent(ChainStatus status) {
        ChainStatus before = state.getStatus();
        state.setStatus(status);

        if (before != status) {
            try {
                notifyEvent(new ChainStatusChangeEvent(this, state.getStatus(), before));

                // cancel loop task when chain status is terminal
                if (status.isTerminal()) {
                    cancelAllLoopTasks();
                }

            } finally {
                if (stateRepository != null) {
                    stateRepository.updateChainStatus(state.getInstanceId(), status);
                }
            }
        }
    }

    public void notifyEvent(Event event) {
        eventManager.notifyEvent(event, this);
    }


    public Phaser getPhaser() {
        return phaser;
    }

    public void setPhaser(Phaser phaser) {
        this.phaser = phaser;
    }

    public void set(String key, Object value) {
        state.getMemory().put(key, value);
    }


    public Object get(String key) {
        Object result = MapUtil.getByPath(state.getMemory(), key);
        return result != null ? result : MapUtil.getByPath(state.getEnvironment(), key);
    }

    public void execute(Map<String, Object> variables) {
        runInLifeCycle(variables,
                new ChainStartEvent(this, variables),
                this::executeInternal);
    }


    public Map<String, Object> executeForResult(Map<String, Object> variables) {
        return executeForResult(variables, false);
    }

    public Map<String, Object> executeForResult(Map<String, Object> variables, boolean ignoreError) {
        if (state.getStatus() == ChainStatus.SUSPEND) {
            this.resume(variables);
        } else {
            runInLifeCycle(variables, new ChainStartEvent(this, variables), this::executeInternal);
        }

        if (!ignoreError) {
            if (state.getStatus() == ChainStatus.FAILED) {
                if (this.error != null) {
                    if (this.error instanceof RuntimeException) {
                        throw (RuntimeException) this.error;
                    } else {
                        throw new RuntimeException(this.error);
                    }
                } else if (state.getError() != null) {
                    throw new ChainException(state.getError().getMessage());
                } else {
                    throw new ChainException("Chain execute error");
                }
            } else if (state.getStatus() == ChainStatus.SUSPEND) {
                if (this.error != null) {
                    if (this.error instanceof ChainSuspendException) {
                        throw (ChainSuspendException) this.error;
                    } else {
                        throw new ChainSuspendException(this.error);
                    }
                } else if (state.getError() != null) {
                    throw new ChainSuspendException(state.getError().getMessage());
                } else {
                    throw new ChainSuspendException("Chain execute error");
                }
            }
        }

        return state.getExecuteResult();
    }


    public Map<String, Object> getParameterValues(Node node) {
        return getParameterValues(node, node.getParameters());
    }

    public Map<String, Object> getParameterValues(Node node, List<? extends Parameter> parameters) {
        return getParameterValues(node, parameters, null);
    }

    public Map<String, Object> getParameterValues(Node node, List<? extends Parameter> parameters, Map<String, Object> formatArgs) {
        return getParameterValues(node, parameters, formatArgs, false);
    }

    private boolean isNullOrBlank(Object value) {
        return value == null || value instanceof String && StringUtil.noText((String) value);
    }

    public Map<String, Object> getParameterValuesOnly(Node node, List<? extends Parameter> parameters, Map<String, Object> formatArg) {
        return getParameterValues(node, parameters, formatArg, true);
    }

    public Map<String, Object> getEnvMap() {
        Map<String, Object> formatArgsMap = new HashMap<>();
        formatArgsMap.put("env", state.getEnvironment());
        formatArgsMap.put("env.sys", System.getenv());
        return formatArgsMap;
    }

    public Map<String, Object> getParameterValues(Node node, List<? extends Parameter> parameters, Map<String, Object> formatArgs, boolean getValueOnly) {
        if (parameters == null || parameters.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> variables = new LinkedHashMap<>();
        List<Parameter> suspendParameters = null;
        for (Parameter parameter : parameters) {
            RefType refType = parameter.getRefType();
            Object value;
            if (refType == RefType.FIXED) {
                value = TextTemplate.of(parameter.getValue())
                        .formatToString(Arrays.asList(formatArgs, getEnvMap()));
            } else if (refType == RefType.REF) {
                value = this.get(parameter.getRef());
            } else {
                value = this.get(parameter.getName());
            }

            if (value == null && parameter.getDefaultValue() != null) {
                value = parameter.getDefaultValue();
            }

            if (refType == RefType.INPUT && isNullOrBlank(value)) {
                if (!getValueOnly && parameter.isRequired()) {
                    if (suspendParameters == null) {
                        suspendParameters = new ArrayList<>();
                    }
                    suspendParameters.add(parameter);
                    continue;
                }
            }

            if (parameter.isRequired() && isNullOrBlank(value)) {
                if (!getValueOnly) {
                    throw new ChainException(node.getName() + " Missing required parameter:" + parameter.getName());
                }
            }

            if (value instanceof String) {
                value = ((String) value).trim();
                if (parameter.getDataType() == DataType.Boolean) {
                    value = "true".equalsIgnoreCase((String) value) || "1".equalsIgnoreCase((String) value);
                } else if (parameter.getDataType() == DataType.Number) {
                    value = Long.parseLong((String) value);
                } else if (parameter.getDataType() == DataType.Array) {
                    value = JSON.parseArray((String) value);
                }
            }

            variables.put(parameter.getName(), value);
        }

        if (suspendParameters != null && !suspendParameters.isEmpty()) {
            state.setSuspendForParameters(suspendParameters);
            this.suspend(node);

            // 构建参数名称列表
            String missingParams = suspendParameters.stream()
                    .map(Parameter::getName)
                    .collect(Collectors.joining("', '", "'", "'"));

            String errorMessage = String.format(
                    "Node '%s' (type: %s) is suspended. Waiting for input parameters: %s.",
                    StringUtil.getFirstWithText(node.getName(), node.getId()),
                    node.getClass().getSimpleName(),
                    missingParams
            );

            throw new ChainSuspendException(errorMessage);
        }

        return variables;
    }

    protected List<Node> getStartNodes() {
        List<Node> suspendNodes = getNodeByIds(state.getSuspendNodeIds());
        if (suspendNodes != null) return suspendNodes;

        return definition.getStartNodes();
    }

    @Nullable
    private List<Node> getNodeByIds(Set<String> nodeIds) {
        if (nodeIds != null && !nodeIds.isEmpty()) {
            List<Node> result = new ArrayList<>(nodeIds.size());
            for (String waitingNodeId : nodeIds) {
                Node waitingNode = definition.getNodeById(waitingNodeId);
                if (waitingNode != null) {
                    result.add(waitingNode);
                }
            }
            return result;
        }
        return null;
    }


    protected void executeInternal() {
        List<Node> startNodes = getStartNodes();
        if (startNodes == null || startNodes.isEmpty()) {
            return;
        }

        List<ExecutionContext> executionContexts = new ArrayList<>();
        for (Node node : startNodes) {
            executionContexts.add(new ExecutionContext(node, null, ""));
        }

        doExecuteNodes(executionContexts);
    }


    protected void doExecuteNodes(List<ExecutionContext> executionContexts) {
        for (ExecutionContext context : executionContexts) {
            Node currentNode = context.currentNode;
            if (currentNode.isAsync()) {
                phaser.register();
                asyncNodeExecutors.execute(() -> {
                    try {
                        EXECUTION_THREAD_LOCAL.set(Chain.this);
                        doExecuteNode(context);
                    } finally {
                        saveOrUpdateState();
                        EXECUTION_THREAD_LOCAL.remove();
                        phaser.arriveAndDeregister();
                    }
                });
            } else {
                try {
                    doExecuteNode(context);
                } finally {
                    saveOrUpdateState();
                }
            }
        }
    }


    /**
     * 获取节点执行结果
     *
     * @param nodeId 节点ID
     * @return 执行结果
     */
    public Map<String, Object> getNodeExecuteResult(String nodeId) {
        Map<String, Object> all = state.getMemory();
        Map<String, Object> result = new HashMap<>();
        all.forEach((k, v) -> {
            if (k.startsWith(nodeId)) {
                String newKey = k.substring(nodeId.length() + 1);
                result.put(newKey, v);
            }
        });
        return result;
    }


    public void doExecuteNode(ExecutionContext context) {

        Node currentNode = context.currentNode;
        String fromEdgeId = context.fromEdgeId;

        if (state.getStatus() == ChainStatus.SUSPEND) {
            state.addSuspendNodeId(currentNode.getId());
            return;
        }

        // 恢复状态
        if (state.getStatus() == ChainStatus.WAITING) {
            setStatusAndNotifyEvent(ChainStatus.RUNNING);
        }

        if (state.getStatus() != ChainStatus.RUNNING) {
            return;
        }

        NodeState nodeState = state.getOrCreateNodeState(currentNode.id);
        Map<String, Object> executeResult = null;


        if (shouldSkipCurrentNode(context, nodeState, currentNode)) {
            return;
        }

        try {
            notifyEvent(new NodeStartEvent(this, currentNode));
            if (state.getStatus() != ChainStatus.RUNNING) {
                return;
            }
            nodeState.setNodeStatus(NodeStatus.RUNNING);
            try {
                state.removeSuspendNodeId(currentNode.getId());
                executeResult = currentNode.execute(this);
                state.addComputeCost(currentNode.calculateComputeCost(this, executeResult));
            } finally {
                // 记录执行结果 和 执行次数，防止在循环执行的情况下，可能导致死循环
                nodeState.recordExecute(fromEdgeId);
                state.setExecuteResult(executeResult);
            }
        } catch (Throwable error) {
            nodeState.setNodeStatus(NodeStatus.ERROR);
            nodeState.setError(new ExceptionSummary(error));
            eventManager.notifyNodeError(error, currentNode, executeResult, this);

            // 错误重试
            if (currentNode.isRetryEnable()
                    && currentNode.getMaxRetryCount() > 0
                    && nodeState.getRetryCount() <= currentNode.getMaxRetryCount()) {

                nodeState.setRetryCount(nodeState.getRetryCount() + 1);

                // 保存状态
                saveOrUpdateState();

                safeSleep(currentNode.getRetryIntervalMs());
                doExecuteNode(context);
            }
            // 异常处理
            else {
                throw error;
            }
        } finally {
            nodeState.setNodeStatusFinished();
            notifyEvent(new NodeEndEvent(this, currentNode, executeResult));
        }

        if (executeResult != null && !executeResult.isEmpty()) {
            executeResult.forEach((s, o) -> {
                Chain.this.state.getMemory().put(currentNode.id + "." + s, o);
            });
        }

        // 重置重试次数
        if (currentNode.isRetryEnable() && currentNode.isResetRetryCountAfterNormal()) {
            nodeState.setRetryCount(0);
        }

        // 保存状态
        saveOrUpdateState();


        // 继续执行下一个节点
        if (!currentNode.isLoopEnable()) {
            doExecuteNextNodes(currentNode, executeResult);
            return;
        }


        // 检查是否达到最大循环次数
        if (currentNode.getMaxLoopCount() > 0 && nodeState.getExecuteCount() >= currentNode.getMaxLoopCount()) {
            doExecuteNextNodes(currentNode, executeResult);
            return;
        }

        // 检查跳出条件
        NodeCondition breakCondition = currentNode.getLoopBreakCondition();
        if (breakCondition != null && breakCondition.check(this, nodeState, executeResult)) {
            doExecuteNextNodes(currentNode, executeResult);
            return;
        }

        // 等待间隔, 下次执行时间
//        safeSleep(currentNode.getLoopIntervalMs());
//        doExecuteNode(context);

        // 循环执行
        setStatusAndNotifyEvent(ChainStatus.WAITING);
        scheduleLoopNode(context);
    }


    /**
     * 循环执行
     *
     * @param context 执行上下文
     */
    private void scheduleLoopNode(ExecutionContext context) {
        if (state.getStatus() != ChainStatus.RUNNING) {
            return;
        }

        String instanceId = state.getInstanceId();
        String nodeId = context.currentNode.getId();
        long loopIntervalMs = context.currentNode.getLoopIntervalMs();

        LoopScheduler.Trigger trigger = new LoopScheduler.Trigger();
        trigger.key = instanceId + ":" + nodeId;
        trigger.type = LoopScheduler.TriggerType.LOOP;
        trigger.triggerAt = System.currentTimeMillis() + loopIntervalMs;

        // 触发后提交到 asyncNodeExecutors 执行 Node
        trigger.task = () -> ChainRuntime.asyncExecutors().submit(() -> {
            if (state.getStatus() != ChainStatus.RUNNING) {
                return;
            }
            doExecuteNode(context);
        });

        // 提交到 LoopScheduler
        ChainRuntime.loopScheduler().schedule(trigger);
    }


    /**
     * 取消所有循环任务
     */
    private void cancelAllLoopTasks() {
        String instanceId = state.getInstanceId();
        // 取消该链实例的所有循环任务
        for (Node node : definition.getNodes()) {
            String taskKey = instanceId + ":" + node.getId();
            ChainRuntime.loopScheduler().cancel(taskKey);
        }
    }


    private void safeSleep(long retryIntervalMs) {
        if (retryIntervalMs <= 0) {
            return;
        }
        try {
            Thread.sleep(retryIntervalMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 恢复中断状态
        }
    }


    /**
     * 记录节点触发，并检查当前节点的执行条件是否未通过。
     * 若条件未通过，则返回 true，表示应跳过该节点的执行。
     *
     * @param executionContext 来源于哪个边触发
     * @param nodeState        节点上下文，用于记录触发信息
     * @param currentNode      当前链路节点配置
     * @return 如果条件不满足（需要跳过），返回 true；否则返回 false
     */
    private synchronized boolean shouldSkipCurrentNode(ExecutionContext executionContext, NodeState nodeState, Node currentNode) {

        // NodeState.recordTrigger 和 condition.check 必须在同步块内执行，
        // 否则会导致并发问题: 异步执行的情况下，可能出现全部节点触发了 trigger，但是 check 还未开始执行
        nodeState.recordTrigger(executionContext.fromEdgeId);

        NodeCondition condition = currentNode.getCondition();
        if (condition == null) {
            return false; // 无条件则不应跳过
        }

        Node prevNode = executionContext.prevNode;
        Map<String, Object> prevNodeExecuteResult = prevNode != null ? getNodeExecuteResult(prevNode.getId()) : Collections.emptyMap();

        // 返回 true 表示条件不满足，应跳过当前节点
        return !condition.check(this, nodeState, prevNodeExecuteResult);
    }


    /**
     * 执行后续节点（可能有多个）
     *
     * @param currentNode   当前节点
     * @param executeResult 执行结果
     */
    private void doExecuteNextNodes(Node currentNode, Map<String, Object> executeResult) {
        List<Edge> outwardEdges = currentNode.getOutwardEdges();
        if (CollectionUtil.hasItems(outwardEdges)) {
            List<ExecutionContext> nextExecutionContexts = new ArrayList<>(outwardEdges.size());
            for (Edge edge : outwardEdges) {
                Node nextNode = definition.getNodeById(edge.getTarget());
                if (nextNode == null) {
                    continue;
                }
                EdgeCondition condition = edge.getCondition();
                if (condition == null || condition.check(this, edge, executeResult)) {
                    nextExecutionContexts.add(new ExecutionContext(nextNode, currentNode, edge.getId()));
                }
            }
            doExecuteNodes(nextExecutionContexts);
        }
    }


    protected void runInLifeCycle(Map<String, Object> variables, Event startEvent, Runnable runnable) {
        if (variables != null) {
            state.getMemory().putAll(variables);
        }
        try {
            EXECUTION_THREAD_LOCAL.set(this);
            notifyEvent(startEvent);
            try {
                setStatusAndNotifyEvent(ChainStatus.RUNNING);
                runnable.run();
            } catch (ChainSuspendException cse) {
                this.error = cse;
                state.setError(new ExceptionSummary(cse));
            } catch (Exception e) {
                this.error = e;
                state.setError(new ExceptionSummary(e));
                setStatusAndNotifyEvent(ChainStatus.ERROR);
                eventManager.notifyChainError(e, this);
            }

            // 等待所有节点执行完成
            this.phaser.arriveAndAwaitAdvance();

            if (state.getStatus() == ChainStatus.RUNNING) {
                setStatusAndNotifyEvent(ChainStatus.SUCCEEDED);
            } else if (state.getStatus() == ChainStatus.ERROR) {
                setStatusAndNotifyEvent(ChainStatus.FAILED);
            }
            // else { // do nothing, 不用理会 WAITING 和 SUSPEND 状态  }
        } finally {
            saveOrUpdateState();
            notifyEvent(new ChainEndEvent(this));
            EXECUTION_THREAD_LOCAL.remove();
        }
    }


    public void stopNormal(String message) {
        state.setMessage(message);
        setStatusAndNotifyEvent(ChainStatus.SUCCEEDED);
    }


    public void stopError(String message) {
        state.setMessage(message);
        setStatusAndNotifyEvent(ChainStatus.FAILED);
    }


    public void output(Node node, Object response) {
        eventManager.notifyOutput(this, node, response);
    }


    public ExecutorService getAsyncNodeExecutors() {
        return asyncNodeExecutors;
    }

    public void setAsyncNodeExecutors(ExecutorService asyncNodeExecutors) {
        this.asyncNodeExecutors = asyncNodeExecutors;
    }


    public synchronized void suspend() {
        setStatusAndNotifyEvent(ChainStatus.SUSPEND);
    }


    public synchronized void suspend(Node node) {
        try {
            state.addSuspendNodeId(node.getId());
        } finally {
            setStatusAndNotifyEvent(ChainStatus.SUSPEND);
        }
    }

    public synchronized void resume() {
        resume(Collections.emptyMap());
    }

    public synchronized void resume(Map<String, Object> variables) {
        runInLifeCycle(variables,
                new ChainResumeEvent(this, variables),
                this::executeInternal);
    }


    public static class ExecutionContext {
        final Node currentNode;
        final Node prevNode;
        final String fromEdgeId;

        public ExecutionContext(Node currentNode, Node prevNode, String fromEdgeId) {
            this.currentNode = currentNode;
            this.prevNode = prevNode;
            this.fromEdgeId = fromEdgeId;
        }
    }


    public void reset() {
        //node
        this.state.reset();

        this.asyncNodeExecutors = NamedThreadPools.newFixedThreadPool("chain-executor");
        this.phaser = new Phaser(1);
    }


    public NodeValidResult validate() throws Exception {

        if (definition.nodes == null || definition.nodes.isEmpty()) {
            return NodeValidResult.fail("Chain nodes can not be empty.");
        }

        Map<String, Object> details = new HashMap<>();
        for (Node node : definition.nodes) {
            NodeValidResult nodeResult = node.validate();
            if (nodeResult != null && !nodeResult.isSuccess()) {
                details.put(node.getId(), nodeResult);
            }
        }

        return details.isEmpty() ? NodeValidResult.ok() : NodeValidResult.fail("", details);
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public void setEventManager(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    public void saveOrUpdateState() {
        if (stateRepository == null) {
            return;
        }
        stateRepository.saveOrUpdateChainState(state);
    }


    public ChainStateRepository getStateRepository() {
        return stateRepository;
    }

    public void setStateRepository(ChainStateRepository stateRepository) {
        this.stateRepository = stateRepository;
    }

    public ChainDefinition getDefinition() {
        return definition;
    }

    public ChainState getState() {
        return state;
    }

}
