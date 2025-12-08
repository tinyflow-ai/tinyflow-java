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
import dev.tinyflow.core.util.MapUtil;

import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryChainStateRepository implements ChainStateRepository {
    private static final Map<String, ChainState> chainStateMap = new ConcurrentHashMap<>();

    @Override
    public ChainState load(String instanceId) {
        return MapUtil.computeIfAbsent(chainStateMap, instanceId, k -> {
            ChainState state = new ChainState();
            state.setInstanceId(instanceId);
            return state;
        });
    }

    @Override
    public boolean tryUpdate(ChainState chainState, EnumSet<ChainStateField> fields) {
        chainStateMap.put(chainState.getInstanceId(), chainState);
        return true;
    }
}
