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
                    r -> {
                        Thread t = new Thread(r, "tinyflow-node-exec");
                        t.setDaemon(true);
                        return t;
                    });


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
