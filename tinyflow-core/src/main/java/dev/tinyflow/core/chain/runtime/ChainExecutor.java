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
package dev.tinyflow.core.chain.runtime;

import dev.tinyflow.core.chain.*;
import dev.tinyflow.core.chain.event.ChainStatusChangeEvent;
import dev.tinyflow.core.chain.listener.ChainErrorListener;
import dev.tinyflow.core.chain.listener.ChainEventListener;
import dev.tinyflow.core.chain.listener.ChainOutputListener;
import dev.tinyflow.core.chain.listener.NodeErrorListener;
import dev.tinyflow.core.chain.repository.ChainDefinitionRepository;
import dev.tinyflow.core.chain.repository.ChainStateField;
import dev.tinyflow.core.chain.repository.ChainStateRepository;
import dev.tinyflow.core.chain.repository.NodeStateRepository;

import java.util.*;
import java.util.concurrent.*;

/**
 * TinyFlow 最新 ChainExecutor
 * <p>
 * 说明:
 * * 负责触发 Chain 执行 / 恢复
 * * 不持有长时间运行的 Chain 实例
 * * 支持 async-only 架构
 */
public class ChainExecutor {

    private final ChainDefinitionRepository definitionRepository;
    private final ChainStateRepository chainStateRepository;
    private final NodeStateRepository nodeStateRepository;
    private final TriggerScheduler triggerScheduler;
    private final EventManager eventManager = new EventManager();

    public ChainExecutor(ChainDefinitionRepository definitionRepository
            , ChainStateRepository chainStateRepository
            , NodeStateRepository nodeStateRepository
    ) {
        this.definitionRepository = definitionRepository;
        this.chainStateRepository = chainStateRepository;
        this.nodeStateRepository = nodeStateRepository;
        this.triggerScheduler = ChainRuntime.triggerScheduler();
        this.triggerScheduler.registerConsumer(this::accept);
    }


    public ChainExecutor(ChainDefinitionRepository definitionRepository
            , ChainStateRepository chainStateRepository
            , NodeStateRepository nodeStateRepository
            , TriggerScheduler triggerScheduler) {
        this.definitionRepository = definitionRepository;
        this.chainStateRepository = chainStateRepository;
        this.nodeStateRepository = nodeStateRepository;
        this.triggerScheduler = triggerScheduler;
        this.triggerScheduler.registerConsumer(this::accept);
    }


    public Map<String, Object> execute(String definitionId, Map<String, Object> variables) {
        return execute(definitionId, variables, Long.MAX_VALUE, TimeUnit.SECONDS);
    }


    public Map<String, Object> execute(String definitionId, Map<String, Object> variables, long timeout, TimeUnit unit) {
        Chain chain = createChain(definitionId);
        String stateInstanceId = chain.getStateInstanceId();
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();

        ChainEventListener listener = (event, c) -> {
            if (event instanceof ChainStatusChangeEvent && c.getStateInstanceId().equals(stateInstanceId)) {
                if (((ChainStatusChangeEvent) event).getStatus().isTerminal()) {
                    ChainState state = chainStateRepository.load(stateInstanceId);
                    Map<String, Object> execResult = state.getExecuteResult();
                    future.complete(execResult != null ? execResult : Collections.emptyMap());
                }
                // 挂起状态
                else if (((ChainStatusChangeEvent) event).getStatus() == ChainStatus.SUSPEND) {
                    future.completeExceptionally(new ChainSuspendException(
                            "Chain is suspended"
                            , c.getStateInstanceId()
                            , ((ChainStatusChangeEvent) event).getChain().getState().getSuspendForParameters())
                    );
                }
            }
        };

        ChainErrorListener errorListener = (error, c) -> {
            if (c.getStateInstanceId().equals(stateInstanceId)) {
                future.completeExceptionally(error);
            }
        };

        try {
            this.addEventListener(listener);
            this.addErrorListener(errorListener);
            chain.start(variables);
            Map<String, Object> result = future.get(timeout, unit);
            clearDefaultStates(result);
            return result;
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new RuntimeException("Execution timed out", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            throw new RuntimeException("Execution interrupted", e);
        } catch (Throwable e) {
            future.cancel(true);
            Throwable err = e;
            if (e instanceof ExecutionException) {
                err = e.getCause();
            }
            if (err instanceof RuntimeException) {
                throw (RuntimeException) err;
            } else {
                throw new RuntimeException("Execution failed", err.getCause());
            }
        } finally {
            this.removeEventListener(listener);
            this.removeErrorListener(errorListener);
        }
    }

    /**
     * 清理默认状态
     *
     * @param result 执行结果
     */
    public void clearDefaultStates(Map<String, Object> result) {
        if (result == null || result.isEmpty()) {
            return;
        }
        result.remove(ChainConsts.SCHEDULE_NEXT_NODE_DISABLED_KEY);
        result.remove(ChainConsts.NODE_STATE_STATUS_KEY);
        result.remove(ChainConsts.CHAIN_STATE_STATUS_KEY);
        result.remove(ChainConsts.CHAIN_STATE_MESSAGE_KEY);
    }

    public String executeAsync(String definitionId, Map<String, Object> variables) {
        Chain chain = createChain(definitionId);
        chain.start(variables);
        return chain.getStateInstanceId();
    }


    /**
     * 执行指定节点的业务逻辑
     *
     * @param definitionId 流程定义ID，用于标识哪个流程定义
     * @param nodeId       节点ID，用于标识要执行的具体节点
     * @param variables    执行上下文变量集合，包含节点执行所需的参数和数据
     * @return 执行结果映射表，包含节点执行后的输出数据
     */
    public Map<String, Object> executeNode(String definitionId, String nodeId, Map<String, Object> variables) {
        ChainDefinition chainDefinitionById = definitionRepository.getChainDefinitionById(definitionId);
        Node node = chainDefinitionById.getNodeById(nodeId);
        Chain temp = createChain(definitionId);
        if (variables != null && !variables.isEmpty()) {
            temp.updateStateSafely(s -> {
                s.getMemory().putAll(variables);
                return EnumSet.of(ChainStateField.MEMORY);
            });
        }
        return node.execute(temp);
    }


    /**
     * 获取指定节点的参数列表
     *
     * @param definitionId 链定义ID，用于定位具体的链定义
     * @param nodeId       节点ID，用于在链定义中定位具体节点
     * @return 返回指定节点的参数列表
     */
    public List<Parameter> getNodeParameters(String definitionId, String nodeId) {
        ChainDefinition chainDefinitionById = definitionRepository.getChainDefinitionById(definitionId);
        Node node = chainDefinitionById.getNodeById(nodeId);
        return node.getParameters();
    }


    public void resumeAsync(String stateInstanceId) {
        this.resumeAsync(stateInstanceId, Collections.emptyMap());
    }


    public void resumeAsync(String stateInstanceId, Map<String, Object> variables) {
        ChainState state = chainStateRepository.load(stateInstanceId);
        if (state == null) {
            return;
        }

        ChainDefinition definition = definitionRepository.getChainDefinitionById(state.getChainDefinitionId());
        if (definition == null) {
            return;
        }

        Chain chain = new Chain(definition, state.getInstanceId());
        chain.setTriggerScheduler(triggerScheduler);
        chain.setChainStateRepository(chainStateRepository);
        chain.setNodeStateRepository(nodeStateRepository);
        chain.setEventManager(eventManager);

        chain.resume(variables);
    }


    private Chain createChain(String definitionId) {
        ChainDefinition definition = definitionRepository.getChainDefinitionById(definitionId);
        if (definition == null) {
            throw new RuntimeException("Chain definition not found");
        }

        String stateInstanceId = UUID.randomUUID().toString();
        Chain chain = new Chain(definition, stateInstanceId);
        chain.setTriggerScheduler(triggerScheduler);
        chain.setChainStateRepository(chainStateRepository);
        chain.setNodeStateRepository(nodeStateRepository);
        chain.setEventManager(eventManager);

        return chain;
    }


    private void accept(Trigger trigger, ExecutorService worker) {
        ChainState state = chainStateRepository.load(trigger.getStateInstanceId());
        if (state == null) {
            throw new ChainException("Chain state not found");
        }


        ChainDefinition definition = definitionRepository.getChainDefinitionById(state.getChainDefinitionId());
        if (definition == null) {
            throw new ChainException("Chain definition not found");
        }

        Chain chain = new Chain(definition, trigger.getStateInstanceId());
        chain.setTriggerScheduler(triggerScheduler);
        chain.setChainStateRepository(chainStateRepository);
        chain.setNodeStateRepository(nodeStateRepository);
        chain.setEventManager(eventManager);

        String nodeId = trigger.getNodeId();
        if (nodeId == null) {
            throw new ChainException("Node ID not found in trigger.");
        }

        Node node = definition.getNodeById(nodeId);
        if (node == null) {
            throw new ChainException("Node not found in definition(id: " + definition.getId() + ")");
        }

        chain.executeNode(node, trigger);
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

    public ChainDefinitionRepository getDefinitionRepository() {
        return definitionRepository;
    }

    public ChainStateRepository getChainStateRepository() {
        return chainStateRepository;
    }

    public NodeStateRepository getNodeStateRepository() {
        return nodeStateRepository;
    }

    public TriggerScheduler getTriggerScheduler() {
        return triggerScheduler;
    }

    public EventManager getEventManager() {
        return eventManager;
    }
}
