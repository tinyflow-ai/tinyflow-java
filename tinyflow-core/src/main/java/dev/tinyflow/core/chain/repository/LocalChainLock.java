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
package dev.tinyflow.core.chain.repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class LocalChainLock implements ChainLock {

    private static final Map<String, LockRef> GLOBAL_LOCKS = new ConcurrentHashMap<>();

    private final String instanceId;
    private final ReentrantLock lock;
    private final boolean acquired;

    public LocalChainLock(String instanceId, long timeout, TimeUnit unit) {
        if (instanceId == null || instanceId.isEmpty()) {
            throw new IllegalArgumentException("instanceId must not be blank");
        }
        this.instanceId = instanceId;

        // 获取或创建锁（带引用计数）
        LockRef lockRef = GLOBAL_LOCKS.compute(instanceId, (key, ref) -> {
            if (ref == null) {
                return new LockRef(new ReentrantLock());
            } else {
                ref.refCount.incrementAndGet();
                return ref;
            }
        });

        this.lock = lockRef.lock;
        boolean locked = false;
        try {
            if (timeout <= 0) {
                lock.lock();
                locked = true;
            } else {
                locked = lock.tryLock(timeout, unit);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        this.acquired = locked;

        // 如果获取失败，清理引用计数
        if (!locked) {
            releaseRef();
        }
    }

    @Override
    public boolean isAcquired() {
        return acquired;
    }

    @Override
    public void close() {
        if (acquired) {
            lock.unlock();
            releaseRef();
        }
    }

    private void releaseRef() {
        GLOBAL_LOCKS.computeIfPresent(instanceId, (key, ref) -> {
            if (ref.refCount.decrementAndGet() <= 0) {
                return null; // 移除，允许 GC
            }
            return ref;
        });
    }

    // 内部类：带引用计数的锁包装
    private static class LockRef {
        final ReentrantLock lock;
        final java.util.concurrent.atomic.AtomicInteger refCount = new java.util.concurrent.atomic.AtomicInteger(1);

        LockRef(ReentrantLock lock) {
            this.lock = lock;
        }
    }
}