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

public enum ChainStateField {
    INSTANCE_ID,
    STATUS,
    MESSAGE,
    ERROR,
    MEMORY,
    PAYLOAD,
    NODE_STATES,
    COMPUTE_COST,
    SUSPEND_NODE_IDS,
    SUSPEND_FOR_PARAMETERS,
    EXECUTE_RESULT,
    CHAIN_DEFINITION_ID,
    ENVIRONMENT,
    CHILD_STATE_IDS,
    PARENT_INSTANCE_ID,
    TRIGGER_NODE_IDS,
    TRIGGER_EDGE_IDS,
    UNCHECKED_EDGE_IDS,
    UNCHECKED_NODE_IDS;
}
