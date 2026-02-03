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
package dev.tinyflow.core.chain;

public enum NodeStatus {
    READY(0), // 未开始执行
    RUNNING(1), // 已开始执行，执行中...
    SUSPEND(5),
    ERROR(10), //发生错误
    SUCCEEDED(20), //正常结束
    FAILED(21); //错误结束

    final int value;

    NodeStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static NodeStatus fromValue(int value) {
        for (NodeStatus status : NodeStatus.values()) {
            if (status.value == value) {
                return status;
            }
        }
        return null;
    }

}
