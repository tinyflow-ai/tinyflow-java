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

/**
 * 分布式锁句柄，用于确保锁的正确释放。
 * 使用 try-with-resources 模式保证释放。
 */
public interface ChainLock extends AutoCloseable {
    /**
     * 锁是否成功获取（用于判断是否超时）
     */
    boolean isAcquired();

    /**
     * 释放锁（幂等）
     */
    @Override
    void close();
}