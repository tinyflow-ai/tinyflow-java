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

import dev.tinyflow.core.chain.ChainState;

import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

public interface ChainStateRepository {

    ChainState load(String instanceId);

    boolean tryUpdate(ChainState newState, EnumSet<ChainStateField> fields);

    /**
     * 获取指定 instanceId 的分布式锁
     *
     * @param instanceId 链实例 ID
     * @param timeout    获取锁的超时时间
     * @param unit       时间单位
     * @return ChainLock 句柄，调用方必须负责 close()
     * @throws IllegalArgumentException if instanceId is blank
     */
    default ChainLock getLock(String instanceId, long timeout, TimeUnit unit) {
        return new LocalChainLock(instanceId, timeout, unit);
    }
}
