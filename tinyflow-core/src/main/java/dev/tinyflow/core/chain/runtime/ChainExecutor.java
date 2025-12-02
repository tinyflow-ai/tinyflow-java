package dev.tinyflow.core.chain.runtime;

import dev.tinyflow.core.chain.*;
import dev.tinyflow.core.chain.event.ChainStatusChangeEvent;
import dev.tinyflow.core.chain.listener.ChainErrorListener;
import dev.tinyflow.core.chain.listener.ChainEventListener;
import dev.tinyflow.core.chain.listener.ChainOutputListener;
import dev.tinyflow.core.chain.listener.NodeErrorListener;
import dev.tinyflow.core.chain.repository.*;

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
        Chain current = createChain(definitionId);
        if (current == null) {
            throw new RuntimeException("Chain definition not found");
        }

        String stateInstanceId = current.getStateInstanceId();
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();

        ChainEventListener listener = (event, chain) -> {
            if (event instanceof ChainStatusChangeEvent) {
                if (((ChainStatusChangeEvent) event).getStatus().isTerminal()
                        && chain.getStateInstanceId().equals(stateInstanceId)) {
                    ChainState state = chainStateRepository.load(stateInstanceId);
                    Map<String, Object> execResult = state.getExecuteResult();
                    future.complete(execResult != null ? execResult : Collections.emptyMap());
                }
            }
        };

        ChainErrorListener errorListener = (error, chain) -> {
            if (chain.getStateInstanceId().equals(stateInstanceId)) {
                future.completeExceptionally(error);
            }
        };
        try {
            this.addEventListener(listener);
            this.addErrorListener(errorListener);
            current.start(variables);
            return future.get(300, TimeUnit.SECONDS);
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

    public void executeAsync(String definitionId, Map<String, Object> variables) {
        Chain current = createChain(definitionId);
        if (current == null) {
            throw new RuntimeException("Chain definition not found");
        }
        current.start(variables);
    }


    public void resumeAsync(String instanceId) {
        ChainState state = chainStateRepository.load(instanceId);
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

        chain.resume();
    }


    private Chain createChain(String definitionId) {
        ChainDefinition definition = definitionRepository.getChainDefinitionById(definitionId);
        if (definition == null) {
            return null;
        }

        Chain chain = new Chain(definition, UUID.randomUUID().toString());
        chain.setTriggerScheduler(triggerScheduler);
        chain.setChainStateRepository(chainStateRepository);
        chain.setNodeStateRepository(nodeStateRepository);
        chain.setEventManager(eventManager);

        return chain;
    }


    private void accept(Trigger trigger, ExecutorService worker) {
        System.out.println("Trigger accepted: " + trigger);
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

}
