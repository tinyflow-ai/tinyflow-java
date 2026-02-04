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
package dev.tinyflow.core.node;


import dev.tinyflow.core.chain.*;
import dev.tinyflow.core.chain.repository.ChainStateField;
import dev.tinyflow.core.chain.repository.NodeStateField;
import dev.tinyflow.core.chain.runtime.Trigger;
import dev.tinyflow.core.chain.runtime.TriggerContext;
import dev.tinyflow.core.chain.runtime.TriggerType;
import dev.tinyflow.core.util.IterableUtil;
import dev.tinyflow.core.util.Maps;
import dev.tinyflow.core.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;

public class LoopNode extends BaseNode {

    private Parameter loopVar;

    public Parameter getLoopVar() {
        return loopVar;
    }

    public void setLoopVar(Parameter loopVar) {
        this.loopVar = loopVar;
    }

    @Override
    public Map<String, Object> execute(Chain chain) {
        Trigger prevTrigger = TriggerContext.getCurrentTrigger();
        LoopContext loopContext = getLoopContext(prevTrigger, chain);
        int triggerLoopIndex = getTriggerLoopIndex(prevTrigger);

        if (loopContext.currentIndex != triggerLoopIndex) {
            // 不执行，子流程有分叉，已经被其他的分叉节点触发了
            return Maps.of(ChainConsts.SCHEDULE_NEXT_NODE_DISABLED_KEY, true)
                    .set(ChainConsts.NODE_STATE_STATUS_KEY, NodeStatus.RUNNING);
        }

        ChainState parentState = chain.getState(loopContext.trigger.getStateInstanceId());

        Map<String, Object> parentStateMemory = parentState.getMemory();
        Map<String, Object> loopVars = parentState.resolveParameters(this, Collections.singletonList(loopVar));
        Object loopValue = loopVars.get(loopVar.getName());

        int shouldLoopCount = 0;
        if (loopValue instanceof Iterable) {
            shouldLoopCount = IterableUtil.size((Iterable<?>) loopValue);
        } else if (loopValue instanceof Number || (loopValue instanceof String && StringUtil.isNumeric(loopValue.toString()))) {
            shouldLoopCount = loopValue instanceof Number ? ((Number) loopValue).intValue() : Integer.parseInt(loopValue.toString().trim());
        }

        // 第一次进入，还没开始执行子循环
        if (loopContext.currentIndex == 0) {

            // 设置子状态 id
            chain.updateStateSafely(state -> {
                state.addChildStateId(loopContext.subStateId);
                return EnumSet.of(ChainStateField.CHILD_STATE_IDS);
            });

            // 初始化子状态
            chain.updateStateSafely(loopContext.subStateId, state -> {
                state.setChainDefinitionId(chain.getDefinition().getId());
                state.setParentInstanceId(chain.getStateInstanceId());
                state.setStatus(ChainStatus.RUNNING);
                return EnumSet.of(ChainStateField.CHAIN_DEFINITION_ID, ChainStateField.PARENT_INSTANCE_ID, ChainStateField.STATUS);
            });
        }

        //  不是第一次执行，合并结果到 subResult
        if (loopContext.currentIndex != 0) {
            ChainState subState = chain.getState();
            mergeResult(loopContext.subResult, subState);
        }


        // 执行的次数够了, 恢复父级触发
        if (loopContext.currentIndex >= shouldLoopCount) {
            prevTrigger.setPayload(loopContext.trigger.getPayload());
            prevTrigger.setParent(loopContext.trigger.getParent());
            prevTrigger.setStateInstanceId(loopContext.trigger.getStateInstanceId());
            chain.setStateInstanceId(prevTrigger.getStateInstanceId());
            return loopContext.subResult;
        }


        int loopIndex = loopContext.currentIndex;
        loopContext.currentIndex++;

        // 更新节点的执行状态
        chain.updateNodeStateSafely(loopContext.trigger.getStateInstanceId(), this.id, state -> {
            state.getMemory().put(loopContext.loopExecutionId, loopContext);
            return EnumSet.of(NodeStateField.MEMORY);
        });

        if (loopValue instanceof Iterable) {
            Object loopItem = IterableUtil.get((Iterable<?>) loopValue, loopIndex);
            executeLoopChain(chain, loopContext, loopItem, parentStateMemory);
        } else if (loopValue instanceof Number || (loopValue instanceof String && StringUtil.isNumeric(loopValue.toString()))) {
            executeLoopChain(chain, loopContext, loopIndex, parentStateMemory);
        }

        // 禁用调度下个节点
        return Maps.of(ChainConsts.SCHEDULE_NEXT_NODE_DISABLED_KEY, true)
                .set(ChainConsts.NODE_STATE_STATUS_KEY, NodeStatus.RUNNING);
    }


    private void executeLoopChain(Chain chain, LoopContext loopContext, Object loopItem, Map<String, Object> variables) {

        Map<String, Object> subVariables = new HashMap<>();
        subVariables.put(this.id + ".index", (loopContext.currentIndex - 1));
        subVariables.put(this.id + ".loopItem", loopItem);
        subVariables.putAll(variables);

        Map<String, Object> subPayload = createSubPayload(loopContext);

        ChainDefinition definition = chain.getDefinition();
        List<Edge> outwardEdges = definition.getOutwardEdge(this.id);
        for (Edge edge : outwardEdges) {
            Node child = definition.getNodeById(edge.getTarget());
            if (child.getParentId() != null && child.getParentId().equals(this.id)) {
                chain.scheduleNode(child, loopContext.subStateId, edge.getId(), TriggerType.NEXT
                        , subVariables, subPayload
                        , loopContext.trigger, 0);
            }
        }
    }


    @NotNull
    private Map<String, Object> createSubPayload(LoopContext loopContext) {
        Map<String, Object> payload = new HashMap<>();
        payload.put(buildLoopKey(), loopContext.loopExecutionId);
        payload.put(buildLoopIndexKey(), loopContext.currentIndex);
        return payload;
    }


    /**
     * 把子流程执行的结果填充到主流程的输出参数中
     *
     * @param toResult 主流程的输出参数
     * @param subState 子流程的
     */
    private void mergeResult(Map<String, Object> toResult, ChainState subState) {
        List<Parameter> outputDefs = getOutputDefs();
        if (outputDefs != null) {
            for (Parameter outputDef : outputDefs) {
                Object value = null;

                //引用
                if (outputDef.getRefType() == RefType.REF) {
                    value = subState.resolveValue(outputDef.getRef());
                }
                //固定值
                else if (outputDef.getRefType() == RefType.FIXED) {
                    value = outputDef.getValue();
                }

                @SuppressWarnings("unchecked") List<Object> existList = (List<Object>) toResult.get(outputDef.getName());
                if (existList == null) {
                    existList = new ArrayList<>();
                }
                existList.add(value);
                toResult.put(outputDef.getName(), existList);
            }
        }
    }


    private LoopContext getLoopContext(Trigger prevTrigger, Chain chain) {
        Map<String, Object> payload = prevTrigger.getPayload();

        // 循环的执行 id（每一次执行，都是不同的执行 id， 1 次执行包含 N 次循环）
        // 每个执行 id，在当前的 memory 中，对应一个 LoopContext
        String loopExecutionId = payload == null ? null : (String) payload.get(buildLoopKey());

        // 是否是第一次执行
        boolean isFirstLoop = loopExecutionId == null;

        LoopContext loopContext;
        if (isFirstLoop) {
            loopContext = new LoopContext();
            loopContext.loopExecutionId = UUID.randomUUID().toString();
            loopContext.currentIndex = 0;
            loopContext.subResult = new HashMap<>();
            loopContext.subStateId = UUID.randomUUID().toString();
            loopContext.trigger = prevTrigger;

        }
        // 不是第一次执行
        else {
            String stateInstanceId = prevTrigger.getParent().getStateInstanceId();
            NodeState nodeState = chain.getNodeState(stateInstanceId, this.id);
            loopContext = (LoopContext) nodeState.getMemory().get(loopExecutionId);
        }

        return loopContext;
    }


    public int getTriggerLoopIndex(Trigger trigger) {
        Map<String, Object> payload = trigger.getPayload();
        if (payload == null) {
            return 0;
        }
        Object loopIndex = payload.get(buildLoopIndexKey());
        if (loopIndex == null) {
            return 0;
        }
        return (int) loopIndex;
    }

    @NotNull
    private String buildLoopKey() {
        return "loop__" + this.getId();
    }

    private String buildLoopIndexKey() {
        return "loop__index__" + this.getId();
    }


    public static class LoopContext implements Serializable {
        String loopExecutionId;
        int currentIndex;
        Map<String, Object> subResult;
        String subStateId;
        Trigger trigger;
    }
}
