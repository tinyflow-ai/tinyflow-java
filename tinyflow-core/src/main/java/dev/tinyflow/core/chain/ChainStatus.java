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

import java.util.Map;

/**
 * 链（Chain）执行生命周期状态枚举
 * <p>
 * 该状态机描述了一个 Chain 实例从创建到终止的完整生命周期。
 * 状态分为三类：
 * - <b>初始状态</b>：READY
 * - <b>运行中状态</b>：RUNNING, SUSPEND, WAITING
 * - <b>终态（Terminal）</b>：SUCCEEDED, FAILED, CANCELLED（不可再变更）
 * <p>
 * 设计原则：
 * - 使用行业通用术语（如 SUCCEEDED/FAILED，而非 FINISHED_NORMAL/ABNORMAL）
 * - 明确区分人工干预（SUSPEND）与系统调度（WAITING）
 * - 终态互斥且不可逆，便于状态判断与持久化恢复
 */
public enum ChainStatus {

    /**
     * 初始状态：Chain 已创建，但尚未开始执行。
     * <p>
     * 此状态下，Chain 的内存为空，节点尚未触发。
     * 调用 {@link Chain#start(Map)} 后进入 {@link #RUNNING}。
     */
    READY(0),

    /**
     * 运行中：Chain 正在执行节点逻辑（同步或异步）。
     * <p>
     * 只要至少一个节点仍在处理（包括等待 Phaser 同步），状态即为 RUNNING。
     * 遇到挂起条件（如缺少参数、loop 间隔）时，会转为 {@link #SUSPEND}
     */
    RUNNING(1),

    /**
     * 暂停（人工干预）：Chain 因缺少外部输入而暂停，需用户主动恢复。
     * <p>
     * 典型场景：
     * - 节点参数缺失且标记为 required（等待用户提交）
     * - 人工审批节点（等待管理员操作）
     * <p>
     * 恢复方式：调用 {@link Chain#resume(Map)} 注入所需变量。
     */
    SUSPEND(5),


    /**
     * 错误（中间状态）：执行中发生异常，但尚未终结（例如正在重试）。
     * <p>
     * 此状态表示：Chain 遇到错误，但仍在尝试恢复（如重试机制触发）。
     * 如果重试成功，可回到 RUNNING；如果重试耗尽，则进入 {@link #FAILED}。
     * <p>
     * ⚠️ 注意：此状态 <b>不是终态</b>，Chain 仍可恢复。
     */
    ERROR(10),

    /**
     * 成功完成：Chain 所有节点正常执行完毕，无错误发生。
     * <p>
     * 终态（Terminal State）—— 状态不可再变更。
     * 此状态下，Chain 的执行结果（executeResult）有效。
     */
    SUCCEEDED(20),

    /**
     * 失败结束：Chain 因未处理的异常或错误条件而终止。
     * <p>
     * 终态（Terminal State）—— 状态不可再变更。
     * 常见原因：节点抛出异常、重试耗尽、条件校验失败等。
     * 错误详情可通过 {@link ChainState#getError()} 获取。
     */
    FAILED(21),

    /**
     * 已取消：Chain 被用户或系统主动终止，非因错误。
     * <p>
     * 终态（Terminal State）—— 状态不可再变更。
     * 典型场景：
     * - 用户点击“取消”按钮
     * - 超时自动取消（如审批超时）
     * - 父流程终止子流程
     * <p>
     * 与 {@link #FAILED} 的区别：CANCELLED 是预期行为，通常不计入错误率。
     */
    CANCELLED(22);

    /**
     * 状态的数值标识，可用于数据库存储或网络传输
     */
    private final int value;

    ChainStatus(int value) {
        this.value = value;
    }

    /**
     * 判断当前状态是否为终态（Terminal State）
     * <p>
     * 终态包括：SUCCEEDED, FAILED, CANCELLED
     * 一旦进入终态，Chain 不可再恢复或继续执行。
     *
     * @return 如果是终态，返回 true；否则返回 false
     */
    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED || this == CANCELLED;
    }

    /**
     * 判断当前状态是否表示成功完成
     *
     * @return 如果是 {@link #SUCCEEDED}，返回 true；否则返回 false
     */
    public boolean isSuccess() {
        return this == SUCCEEDED;
    }


    /**
     * 获取状态对应的数值标识
     *
     * @return 状态值
     */
    public int getValue() {
        return value;
    }

    public static ChainStatus fromValue(int value) {
        for (ChainStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        return null;
    }
}