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

import java.util.concurrent.*;

public class ChainRuntime {

    private static final int NODE_POOL_CORE = 32;
    private static final int NODE_POOL_MAX = 512;
    private static final int CHAIN_POOL_CORE = 8;
    private static final int CHAIN_POOL_MAX = 64;

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, r -> {
        Thread t = new Thread(r, "tinyflow-trigger-scheduler");
        t.setDaemon(true);
        return t;
    });

    private static final ExecutorService ASYNC_NODE_EXECUTORS =
            new ThreadPoolExecutor(
                    NODE_POOL_CORE, NODE_POOL_MAX,
                    60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(10000),
                    r -> new Thread(r, "tinyflow-node-exec"));


    private static final TriggerScheduler TRIGGER_SCHEDULER = new TriggerScheduler(
            new InMemoryTriggerStore()
            , scheduler
            , ASYNC_NODE_EXECUTORS
            , 10000L);


    public static TriggerScheduler triggerScheduler() {
        return TRIGGER_SCHEDULER;
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            TRIGGER_SCHEDULER.shutdown();
            ASYNC_NODE_EXECUTORS.shutdownNow();
        }));
    }
}
