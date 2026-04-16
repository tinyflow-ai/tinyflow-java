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

import dev.tinyflow.core.chain.listener.ChainErrorListener;
import dev.tinyflow.core.chain.listener.ChainEventListener;
import dev.tinyflow.core.chain.listener.ChainOutputListener;
import dev.tinyflow.core.chain.listener.NodeErrorListener;
import dev.tinyflow.core.util.MapUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventManager {

    private static final Logger log = LoggerFactory.getLogger(EventManager.class);

    protected final Map<Class<?>, List<ChainEventListener>> eventListeners = new ConcurrentHashMap<>();
    protected final List<ChainOutputListener> outputListeners = new CopyOnWriteArrayList<>();
    protected final List<ChainErrorListener> chainErrorListeners = new CopyOnWriteArrayList<>();
    protected final List<NodeErrorListener> nodeErrorListeners = new CopyOnWriteArrayList<>();
    protected final Map<String, Object> otherListeners = new ConcurrentHashMap<>();

    /**
     * ---------- 通用事件监听器 ----------
     */
    public void addEventListener(Class<? extends Event> eventClass, ChainEventListener listener) {
        List<ChainEventListener> chainEventListeners = eventListeners.computeIfAbsent(eventClass
                , k -> new CopyOnWriteArrayList<>());
        chainEventListeners.add(listener);
    }

    public void addEventListener(ChainEventListener listener) {
        addEventListener(Event.class, listener);
    }

    public void removeEventListener(Class<? extends Event> eventClass, ChainEventListener listener) {
        List<ChainEventListener> list = eventListeners.get(eventClass);
        if (list != null) {
            list.remove(listener);
            if (list.isEmpty()) {
                eventListeners.remove(eventClass, list);
            }
        }
    }

    public void removeEventListener(ChainEventListener listener) {
        for (List<ChainEventListener> list : eventListeners.values()) {
            list.remove(listener);
        }
    }

    public void notifyEvent(Event event, Chain chain) {
        for (Map.Entry<Class<?>, List<ChainEventListener>> entry : eventListeners.entrySet()) {
            if (entry.getKey().isInstance(event)) {
                for (ChainEventListener listener : entry.getValue()) {
                    try {
                        listener.onEvent(event, chain);
                    } catch (Exception e) {
                        log.error("Error in event listener: {}", e, e);
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

    public void notifyOutput(Chain chain, Node node, Object response) {
        for (ChainOutputListener listener : outputListeners) {
            try {
                listener.onOutput(chain, node, response);
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

    public void notifyChainError(Throwable error, Chain chain) {
        for (ChainErrorListener listener : chainErrorListeners) {
            try {
                listener.onError(error, chain);
            } catch (Exception e) {
                log.error("Error in chain error listener: {}", e, e);
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

    public void notifyNodeError(Throwable error, Node node, Map<String, Object> result, Chain chain) {
        for (NodeErrorListener listener : nodeErrorListeners) {
            try {
                listener.onError(error, node, result, chain);
            } catch (Exception e) {
                log.error("Error in node error listener: {}", e.toString(), e);
            }
        }
    }

    public void addOtherListener(String key, Object listener) {
        otherListeners.put(key, listener);
    }

    public void removeOtherListener(String key) {
        otherListeners.remove(key);
    }

    public <T> T getOtherListener(String key) {
        //noinspection unchecked
        return (T) otherListeners.get(key);
    }
}