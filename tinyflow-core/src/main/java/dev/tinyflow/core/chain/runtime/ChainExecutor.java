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
import dev.tinyflow.core.chain.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
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

    private static final Logger log = LoggerFactory.getLogger(ChainExecutor.class);
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
        return execute(definitionId, variables, 3, TimeUnit.MINUTES);
    }


    public Map<String, Object> execute(String definitionId, Map<String, Object> variables, long timeout, TimeUnit unit) {
        Chain chain = createChain(definitionId);
        if (chain == null) {
            throw new RuntimeException("Chain definition not found");
        }

        String stateInstanceId = chain.getStateInstanceId();
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();

        ChainEventListener listener = (event, c) -> {
            if (event instanceof ChainStatusChangeEvent) {
                if (((ChainStatusChangeEvent) event).getStatus().isTerminal()
                        && c.getStateInstanceId().equals(stateInstanceId)) {
                    ChainState state = chainStateRepository.load(stateInstanceId);
                    Map<String, Object> execResult = state.getExecuteResult();
                    future.complete(execResult != null ? execResult : Collections.emptyMap());
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
            return future.get(timeout, unit);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new RuntimeException("Execution timed out", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Execution failed", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            throw new RuntimeException("Execution interrupted", e);
        } finally {
            this.removeEventListener(listener);
            this.removeErrorListener(errorListener);
        }
    }

    public String executeAsync(String definitionId, Map<String, Object> variables) {
        Chain chain = createChain(definitionId);
        if (chain == null) {
            throw new RuntimeException("Chain definition not found");
        }
        chain.start(variables);
        return chain.getStateInstanceId();
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
            return null;
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
            return;
        }

        ChainDefinition definition = definitionRepository.getChainDefinitionById(state.getChainDefinitionId());
        if (definition == null) {
            return;
        }

        Chain chain = new Chain(definition, trigger.getStateInstanceId());
        chain.setTriggerScheduler(triggerScheduler);
        chain.setChainStateRepository(chainStateRepository);
        chain.setNodeStateRepository(nodeStateRepository);
        chain.setEventManager(eventManager);


        Map<String, Object> payload = trigger.getPayload();
        if (payload != null && !payload.isEmpty()) {
            chain.updateStateSafely(s -> {
                s.getMemory().putAll(payload);
                return EnumSet.of(ChainStateField.MEMORY);
            });
        }

        String nodeId = trigger.getNodeId();
        if (nodeId == null) return;

        Node node = definition.getNodeById(nodeId);
        if (node == null) return;

        chain.executeNode(node, trigger.getEdgeId());
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
