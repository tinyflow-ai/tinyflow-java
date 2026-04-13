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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * 功能:
 * - schedule trigger (持久化到 TriggerStore 并 schedule)
 * - cancel trigger
 * - fire(triggerId) 用于 webhook/event/manual 主动触发
 * - recoverAndSchedulePending() 启动时恢复未执行的 trigger
 * - periodical scan findDue(upto) 以保证重启/宕机后补偿触发
 * <p>
 * 注意: 分布式环境下需要在 TriggerStore 层提供抢占/锁逻辑（例如 lease/owner 字段）。
 */
public class TriggerScheduler {

    private static final Logger log = LoggerFactory.getLogger(TriggerScheduler.class);
    private final TriggerStore store;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService worker;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    // map 用于管理取消：triggerId -> ScheduledFuture
    private final ConcurrentMap<String, ScheduledFuture<?>> scheduledFutures = new ConcurrentHashMap<>();

    // consumer 来把 trigger 交给 ChainExecutor（或 ChainRuntime）去处理
    private volatile TriggerConsumer consumer;

    // 周期扫查间隔（ms）
    private final long scanIntervalMs;

    // 扫描任务 future
    private ScheduledFuture<?> scanFuture;

    public interface TriggerConsumer {
        void accept(Trigger trigger, ExecutorService worker);
    }

    public TriggerScheduler(TriggerStore store, ScheduledExecutorService scheduler, ExecutorService worker, long scanIntervalMs) {
        this.store = Objects.requireNonNull(store, "TriggerStore required");
        this.scheduler = Objects.requireNonNull(scheduler, "ScheduledExecutorService required");
        this.worker = Objects.requireNonNull(worker, "ExecutorService required");
        this.scanIntervalMs = Math.max(1000, scanIntervalMs);

        // 恢复并 schedule
        recoverAndSchedulePending();

        // 启动周期扫查 findDue
        startPeriodicScan();
    }


    public void registerConsumer(TriggerConsumer consumer) {
        this.consumer = consumer;
    }

    /**
     * schedule a trigger: persist -> schedule (单机语义)
     */
    public Trigger schedule(Trigger trigger) {
        if (closed.get()) throw new IllegalStateException("TriggerScheduler closed");
        if (trigger.getId() == null) {
            trigger.setId(UUID.randomUUID().toString());
        }
        store.save(trigger);
        scheduleInternal(trigger);
        return trigger;
    }

    /**
     * cancel trigger (从 store 删除并尝试取消已 schedule 的 future)
     */
    public boolean cancel(String triggerId) {
        boolean removed = store.remove(triggerId);
        ScheduledFuture<?> f = scheduledFutures.remove(triggerId);
        if (f != null) {
            f.cancel(false);
        }
        return removed;
    }

    /**
     * 主动触发（webhook/event/manual 场景）
     */
    public boolean fire(String triggerId) {
        if (closed.get()) return false;
        Trigger t = store.find(triggerId);
        if (t == null) return false;
        if (consumer == null) {
            // 无 consumer，仍从 store 中移除
            store.remove(triggerId);
            return false;
        }
        // 在 worker 线程触发 consumer
        worker.submit(() -> {
            try {
                consumer.accept(t, worker);
            } catch (Exception e) {
                log.error(e.toString(), e);
            } finally {
                // 默认语义：触发后移除
                store.remove(triggerId);
                ScheduledFuture<?> sf = scheduledFutures.remove(triggerId);
                if (sf != null) sf.cancel(false);
            }
        });
        return true;
    }

    /**
     * internal scheduling for a trigger (单机 scheduled semantics)
     */
    private void scheduleInternal(Trigger trigger) {
        if (closed.get()) return;

        long delay = Math.max(0, trigger.getTriggerAt() - System.currentTimeMillis());

        // cancel any existing scheduled future for same id
        ScheduledFuture<?> prev = scheduledFutures.remove(trigger.getId());
        if (prev != null) {
            prev.cancel(false);
        }

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            // double-check existence in store (可能已被 cancel)
            Trigger existing = store.find(trigger.getId());
            if (existing == null) {
                scheduledFutures.remove(trigger.getId());
                return;
            }

            if (consumer != null) {
                worker.submit(() -> {
                    try {
                        TriggerContext.setCurrentTrigger(existing);
                        consumer.accept(existing, worker);
                    } catch (Throwable e) {
                        log.error(e.toString(), e);
                    } finally {
                        TriggerContext.clearCurrentTrigger();
                        store.remove(existing.getId());
                        scheduledFutures.remove(existing.getId());
                    }
                });
            } else {
                // 无 consumer，则移除
                store.remove(existing.getId());
                scheduledFutures.remove(existing.getId());
            }
        }, delay, TimeUnit.MILLISECONDS);

        scheduledFutures.put(trigger.getId(), future);
    }

    private void recoverAndSchedulePending() {
        try {
            List<Trigger> list = store.findAllPending();
            if (list == null || list.isEmpty()) return;
            for (Trigger t : list) {
                scheduleInternal(t);
            }
        } catch (Throwable t) {
            // 忽略单次恢复错误，继续运行
            t.printStackTrace();
        }
    }

    private void startPeriodicScan() {
        if (closed.get()) return;
        scanFuture = scheduler.scheduleAtFixedRate(() -> {
            try {
                long upto = System.currentTimeMillis();
                List<Trigger> due = store.findDue(upto);
                if (due == null || due.isEmpty()) return;
                for (Trigger t : due) {
                    // 如果已被 schedule 到未来（scheduledFutures 包含且尚未到期），跳过
                    ScheduledFuture<?> sf = scheduledFutures.get(t.getId());
                    if (sf != null && !sf.isDone() && !sf.isCancelled()) {
                        continue;
                    }
                    // 直接提交到 worker，让 consumer 处理；并从 store 中移除
                    if (consumer != null) {
                        worker.submit(() -> {
                            try {
                                consumer.accept(t, worker);
                            } finally {
                                store.remove(t.getId());
                                ScheduledFuture<?> f2 = scheduledFutures.remove(t.getId());
                                if (f2 != null) f2.cancel(false);
                            }
                        });
                    } else {
                        store.remove(t.getId());
                    }
                }
            } catch (Throwable tt) {
                tt.printStackTrace();
            }
        }, scanIntervalMs, scanIntervalMs, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        if (closed.compareAndSet(false, true)) {
            if (scanFuture != null) scanFuture.cancel(false);
            // cancel scheduled futures
            for (Map.Entry<String, ScheduledFuture<?>> e : scheduledFutures.entrySet()) {
                try {
                    e.getValue().cancel(false);
                } catch (Throwable ignored) {
                }
            }
            scheduledFutures.clear();

            try {
                scheduler.shutdownNow();
            } catch (Throwable ignored) {
            }
            try {
                worker.shutdownNow();
            } catch (Throwable ignored) {
            }
        }
    }
}
