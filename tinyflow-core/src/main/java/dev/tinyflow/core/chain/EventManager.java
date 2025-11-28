package dev.tinyflow.core.chain;

import dev.tinyflow.core.chain.listener.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EventManager {

    private static final Logger log = LoggerFactory.getLogger(EventManager.class);

    protected final Map<Class<?>, List<ChainEventListener>> eventListeners = new ConcurrentHashMap<>();
    protected final List<ChainOutputListener> outputListeners = Collections.synchronizedList(new ArrayList<>());
    protected final List<ChainErrorListener> chainErrorListeners = Collections.synchronizedList(new ArrayList<>());
    protected final List<NodeErrorListener> nodeErrorListeners = Collections.synchronizedList(new ArrayList<>());
    protected final List<ChainSuspendListener> suspendListeners = Collections.synchronizedList(new ArrayList<>());

    /**
     * ---------- 通用事件监听器 ----------
     */
    public void addEventListener(Class<? extends Event> eventClass, ChainEventListener listener) {
        eventListeners.computeIfAbsent(eventClass, k -> Collections.synchronizedList(new ArrayList<>())).add(listener);
    }

    public void addEventListener(ChainEventListener listener) {
        addEventListener(Event.class, listener);
    }

    public void removeEventListener(Class<? extends Event> eventClass, ChainEventListener listener) {
        List<ChainEventListener> list = eventListeners.get(eventClass);
        if (list != null) list.remove(listener);
    }

    public void removeEventListener(ChainEventListener listener) {
        for (List<ChainEventListener> list : eventListeners.values()) {
            list.remove(listener);
        }
    }

    public void notifyEvent(Event event, Chain execution) {
        for (Map.Entry<Class<?>, List<ChainEventListener>> entry : eventListeners.entrySet()) {
            if (entry.getKey().isInstance(event)) {
                for (ChainEventListener listener : entry.getValue()) {
                    try {
                        listener.onEvent(event, execution);
                    } catch (Exception e) {
                        log.error("Error in event listener: {}", e.toString(), e);
                    }
                }
            }
        }
    }

    /**
     * ---------- Output Listener ----------
     */
    public void addOutputListener(ChainOutputListener listener) {
        outputListeners.add(listener);
    }

    public void removeOutputListener(ChainOutputListener listener) {
        outputListeners.remove(listener);
    }

    public void notifyOutput(Chain execution, Node node, Object response) {
        for (ChainOutputListener listener : outputListeners) {
            try {
                listener.onOutput(execution, node, response);
            } catch (Exception e) {
                log.error("Error in output listener: {}", e.toString(), e);
            }
        }
    }

    /**
     * ---------- Chain Error Listener ----------
     */
    public void addChainErrorListener(ChainErrorListener listener) {
        chainErrorListeners.add(listener);
    }

    public void removeChainErrorListener(ChainErrorListener listener) {
        chainErrorListeners.remove(listener);
    }

    public void notifyChainError(Throwable error, Chain execution) {
        if (chainErrorListeners.isEmpty()) throw new RuntimeException(error);
        for (ChainErrorListener listener : chainErrorListeners) {
            try {
                listener.onError(error, execution);
            } catch (Exception e) {
                log.error("Error in chain error listener: {}", e.toString(), e);
            }
        }
    }

    /**
     * ---------- Node Error Listener ----------
     */
    public void addNodeErrorListener(NodeErrorListener listener) {
        nodeErrorListeners.add(listener);
    }

    public void removeNodeErrorListener(NodeErrorListener listener) {
        nodeErrorListeners.remove(listener);
    }

    public void notifyNodeError(Throwable error, Node node, Map<String, Object> result, Chain execution) {
        for (NodeErrorListener listener : nodeErrorListeners) {
            try {
                listener.onError(error, node, result, execution);
            } catch (Exception e) {
                log.error("Error in node error listener: {}", e.toString(), e);
            }
        }
    }

    /**
     * ---------- Suspend Listener ----------
     */
    public void addSuspendListener(ChainSuspendListener listener) {
        suspendListeners.add(listener);
    }

    public void removeSuspendListener(ChainSuspendListener listener) {
        suspendListeners.remove(listener);
    }

    public void notifySuspend(Chain execution) {
        for (ChainSuspendListener listener : suspendListeners) {
            try {
                listener.onSuspend(execution);
            } catch (Exception e) {
                log.error("Error in suspend listener: {}", e.toString(), e);
            }
        }
    }

}
