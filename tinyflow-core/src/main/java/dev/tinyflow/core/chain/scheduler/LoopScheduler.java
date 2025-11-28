//package dev.tinyflow.core.chain;
//
//import java.util.Map;
//import java.util.concurrent.*;
//import java.util.concurrent.atomic.AtomicLong;
//
//public class LoopScheduler {
//
//    // scheduled pool 可以小，负责触发任务；真正的 node 执行仍由 doExecuteNode (可能提交到 asyncNodeExecutors)
//    private volatile ScheduledExecutorService scheduler;
//    private final ConcurrentMap<String, ScheduledFuture<?>> scheduledMap = new ConcurrentHashMap<>();
//    private final AtomicLong threadIndex = new AtomicLong();
//
//    public LoopScheduler() {
//        rebuild();
//    }
//
//    private ThreadFactory makeThreadFactory() {
//        return r -> {
//            Thread t = new Thread(r);
//            t.setName("chain-loop" + "-" + threadIndex.incrementAndGet());
//            t.setDaemon(true);
//            return t;
//        };
//    }
//
//    public synchronized void rebuild() {
//        if (this.scheduler != null && !this.scheduler.isShutdown()) {
//            try {
//                this.scheduler.shutdownNow();
//            } catch (Exception ignored) {
//            }
//        }
//        this.scheduler = Executors.newScheduledThreadPool(2, makeThreadFactory());
//        scheduledMap.clear();
//    }
//
//    /**
//     * schedule loop for next round
//     *
//     * @param ctx     execution context
//     * @param delayMs delay in milliseconds
//     */
//    public void scheduleLoop(Chain.ExecutionContext ctx, long delayMs) {
//        if (ctx == null || ctx.currentNode == null) {
//            return;
//        }
//        final String nodeId = ctx.currentNode.id;
//        if (state.getStatus() == ChainStatus.SUSPEND || state.getStatus() == ChainStatus.ERROR
//                || state.getStatus() == ChainStatus.FINISHED_ABNORMAL || state.getStatus() == ChainStatus.FINISHED_NORMAL) {
//            // chain 不处于运行态，不再 schedule
//            return;
//        }
//
//        // 避免重复 schedule：如果已经有一个待触发任务，则不再重复安排
//        ScheduledFuture<?> existing = scheduledMap.get(nodeId);
//        if (existing != null && !existing.isDone() && !existing.isCancelled()) {
//            // 已存在计划任务，直接返回（避免多次触发同一节点）
//            return;
//        }
//
//        Runnable task = () -> {
//            // 在 scheduler 线程中触发节点执行；执行时要设置线程本地的 chain
//            try {
//                EXECUTION_THREAD_LOCAL.set(Chain.this);
//                // 检查链状态
//                if (state.getStatus() != ChainStatus.RUNNING) {
//                    return;
//                }
//                // 执行节点
//                doExecuteNode(ctx);
//            } catch (Throwable t) {
//                // 捕获任何异常，通知事件
//                eventManager.notifyChainError(t, Chain.this);
//            } finally {
//                // 清理 scheduledMap 中对应的 entry（因为任务已经触发）
//                scheduledMap.remove(nodeId);
//                EXECUTION_THREAD_LOCAL.remove();
//            }
//        };
//
//        ScheduledFuture<?> future;
//        if (delayMs <= 0) {
//            future = scheduler.schedule(task, 0, TimeUnit.MILLISECONDS);
//        } else {
//            future = scheduler.schedule(task, delayMs, TimeUnit.MILLISECONDS);
//        }
//        scheduledMap.put(nodeId, future);
//    }
//
//    public void cancel(String nodeId) {
//        ScheduledFuture<?> f = scheduledMap.remove(nodeId);
//        if (f != null) {
//            try {
//                f.cancel(false);
//            } catch (Exception ignored) {
//            }
//        }
//    }
//
//    public void cancelAll() {
//        for (Map.Entry<String, ScheduledFuture<?>> e : scheduledMap.entrySet()) {
//            try {
//                e.getValue().cancel(false);
//            } catch (Exception ignored) {
//            }
//        }
//        scheduledMap.clear();
//    }
//
//    public void shutdown() {
//        cancelAll();
//        if (scheduler != null && !scheduler.isShutdown()) {
//            try {
//                scheduler.shutdownNow();
//            } catch (Exception ignored) {
//            }
//        }
//    }
//}
