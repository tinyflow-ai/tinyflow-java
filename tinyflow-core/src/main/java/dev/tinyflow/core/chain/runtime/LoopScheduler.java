package dev.tinyflow.core.chain.runtime;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TinyFlow LoopScheduler（最终版）
 * <p>
 * 统一调度 Trigger（Loop / Timer / Webhook / SubChain）
 * 异步执行 Node 通过 asyncNodeExecutors
 * 支持分布式恢复（Leader-only）
 */
public class LoopScheduler {

    /**
     * 调度线程池（单线程即可，高性能 DelayQueue）
     */
    private final ScheduledExecutorService scheduler;

    /**
     * key -> Future，方便取消或覆盖旧任务
     */
    private final ConcurrentMap<String, ScheduledFuture<?>> futureMap = new ConcurrentHashMap<>();

    /**
     * Scheduler 是否已关闭
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public LoopScheduler(int threads) {
        this.scheduler = Executors.newScheduledThreadPool(threads, r -> {
            Thread t = new Thread(r, "tinyflow-loop-scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Trigger：统一调度单元
     */
    public static class Trigger {
        public String key;         // 唯一标识：chainId:nodeId
        public long triggerAt;     // 触发时间戳
        public TriggerType type;   // 类型（Loop / Timer / Webhook / SubChain）
        public Runnable task;      // 执行逻辑（只触发，不执行 Node）
    }

    public enum TriggerType {
        LOOP, TIMER, WEBHOOK, SUBCHAIN
    }

    /**
     * 提交调度
     * 如果已有同 key 的任务，旧任务将被取消
     */
    public void schedule(Trigger trigger) {
        if (closed.get()) {
            throw new IllegalStateException("LoopScheduler is closed");
        }

        long delay = Math.max(trigger.triggerAt - System.currentTimeMillis(), 0);

        futureMap.compute(trigger.key, (k, old) -> {
            if (old != null) {
                old.cancel(false);
            }
            return scheduler.schedule(() -> {
                try {
                    trigger.task.run();
                } finally {
                    futureMap.remove(k);
                }
            }, delay, TimeUnit.MILLISECONDS);
        });
    }

    /**
     * 取消某个 Trigger
     */
    public void cancel(String key) {
        ScheduledFuture<?> f = futureMap.remove(key);
        if (f != null) {
            f.cancel(false);
        }
    }

    /**
     * graceful shutdown
     */
    public void shutdown() {
        if (closed.compareAndSet(false, true)) {
            for (Map.Entry<String, ScheduledFuture<?>> e : futureMap.entrySet()) {
                e.getValue().cancel(false);
            }
            futureMap.clear();
            scheduler.shutdownNow();
        }
    }
}
