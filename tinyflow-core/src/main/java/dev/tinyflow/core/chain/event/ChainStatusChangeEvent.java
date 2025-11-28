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
package dev.tinyflow.core.chain.event;


import dev.tinyflow.core.chain.Chain;
import dev.tinyflow.core.chain.ChainStatus;

public class ChainStatusChangeEvent extends BaseEvent {

    private final ChainStatus status;
    private final ChainStatus before;

    public ChainStatusChangeEvent(Chain chain, ChainStatus status, ChainStatus before) {
        super(chain);
        this.status = status;
        this.before = before;
    }

    public ChainStatus getStatus() {
        return status;
    }

    public ChainStatus getBefore() {
        return before;
    }


    @Override
    public String toString() {
        return "ChainStatusChangeEvent{" +
            "status=" + status +
            ", before=" + before +
            ", chain=" + chain +
            '}';
    }
}
