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


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryTriggerStore implements TriggerStore {

    private final ConcurrentHashMap<String, Trigger> store = new ConcurrentHashMap<>();

    @Override
    public Trigger save(Trigger trigger) {
        if (trigger.getId() == null) {
            trigger.setId(UUID.randomUUID().toString());
        }
        store.put(trigger.getId(), trigger);
        return trigger;
    }

    @Override
    public boolean remove(String triggerId) {
        return store.remove(triggerId) != null;
    }

    @Override
    public Trigger find(String triggerId) {
        return store.get(triggerId);
    }

    @Override
    public List<Trigger> findDue(long uptoTimestamp) {
        return null;
    }

    @Override
    public List<Trigger> findAllPending() {
        return new ArrayList<>(store.values());
    }
}

