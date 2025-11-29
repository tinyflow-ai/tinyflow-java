package dev.tinyflow.core.chain.runtime;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ChainRuntime {

    /** 全局 LoopScheduler（单例） */
    private static final LoopScheduler LOOP_SCHEDULER = new LoopScheduler(1);

    /** 异步执行 Node 的线程池 */
    private static final ExecutorService ASYNC_NODE_EXECUTORS =
            new ThreadPoolExecutor(
                    32, 512,
                    60, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(10000),
                    r -> {
                        Thread t = new Thread(r, "tinyflow-loop-node");
                        t.setDaemon(true);
                        return t;
                    }
            );

    public static LoopScheduler loopScheduler() {
        return LOOP_SCHEDULER;
    }

    public static ExecutorService asyncExecutors() {
        return ASYNC_NODE_EXECUTORS;
    }

    /** JVM shutdown hook */
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOOP_SCHEDULER.shutdown();
            ASYNC_NODE_EXECUTORS.shutdownNow();
        }));
    }
}
