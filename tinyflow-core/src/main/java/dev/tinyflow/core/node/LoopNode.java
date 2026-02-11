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

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
        Deque<LoopContext> loopStack = getOrCreateLoopStack(chain);

        LoopContext loopContext;

        // 判断是否是首次进入该 LoopNode（即不是由子节点返回）
        TriggerType triggerType = prevTrigger.getType();
        boolean isFirstEntry = triggerType != TriggerType.PARENT && triggerType != TriggerType.SELF;

        if (isFirstEntry) {
            // 首次触发：创建新的 LoopContext 并压入堆栈
            loopContext = new LoopContext();
//            loopContext.loopExecutionId = UUID.randomUUID().toString();
            loopContext.currentIndex = 0;
            loopContext.subResult = new HashMap<>();
            // 保存原始触发上下文（用于循环结束后恢复）
            loopStack.push(loopContext);

            chain.updateNodeStateSafely(this.id, state -> {
                state.getMemory().put(buildLoopStackId(), loopStack);
                return EnumSet.of(NodeStateField.MEMORY);
            });

            if (loopStack.size() > 1) {
                // 不执行，等等其他节点唤起
                return Maps.of(ChainConsts.SCHEDULE_NEXT_NODE_DISABLED_KEY, true)
                        .set(ChainConsts.NODE_STATE_STATUS_KEY, NodeStatus.RUNNING);
            }
        }
        // 由子节点返回：从堆栈低部获取当前循环上下文
        else {
            if (loopStack.isEmpty()) {
                throw new IllegalStateException("Loop stack is empty when returning from child node.");
            }
            loopContext = loopStack.peekLast();
        }


//       LoopContext loopContext = getLoopContext(prevTrigger, chain);
//        int triggerLoopIndex = getTriggerLoopIndex(prevTrigger);
//
//        if (loopContext.currentIndex != triggerLoopIndex) {
//            // 不执行，子流程有分叉，已经被其他的分叉节点触发了
//            return Maps.of(ChainConsts.SCHEDULE_NEXT_NODE_DISABLED_KEY, true)
//                    .set(ChainConsts.NODE_STATE_STATUS_KEY, NodeStatus.RUNNING);
//        }

        Map<String, Object> loopVars = chain.getState().resolveParameters(this, Collections.singletonList(loopVar));
        Object loopValue = loopVars.get(loopVar.getName());

        int shouldLoopCount;
        if (loopValue instanceof Iterable) {
            shouldLoopCount = IterableUtil.size((Iterable<?>) loopValue);
        } else if (loopValue instanceof Number || (loopValue instanceof String && StringUtil.isNumeric(loopValue.toString()))) {
            shouldLoopCount = loopValue instanceof Number ? ((Number) loopValue).intValue() : Integer.parseInt(loopValue.toString().trim());
        } else {
            throw new IllegalArgumentException("loopValue must be Iterable or Number or String, but loopValue is \"" + loopValue + "\"");
        }

        //  不是第一次执行，合并结果到 subResult
        if (loopContext.currentIndex != 0) {
            ChainState subState = chain.getState();
            mergeResult(loopContext.subResult, subState);
        }


        // 执行的次数够了, 恢复父级触发
        if (loopContext.currentIndex >= shouldLoopCount) {
//            prevTrigger.setPayload(loopContext.triggerPayload);
//            prevTrigger.setPayload(loopContext.trigger.getPayload());
//            prevTrigger.setParent(loopContext.trigger.getParent());
            loopStack.poll();
            chain.updateNodeStateSafely(this.id, state -> {
                state.getMemory().put(buildLoopStackId(), loopStack);
                return EnumSet.of(NodeStateField.MEMORY);
            });
            if (!loopStack.isEmpty()) {
                chain.scheduleNode(this, null, TriggerType.SELF, 0);
            }
            return loopContext.subResult;
        }

        int loopIndex = loopContext.currentIndex;
        loopContext.currentIndex++;

        chain.updateNodeStateSafely(this.id, state -> {
            state.getMemory().put(buildLoopStackId(), loopStack);
            return EnumSet.of(NodeStateField.MEMORY);
        });

        // 更新节点的执行状态
//        chain.updateNodeStateSafely(this.id, state -> {
//            state.getMemory().put(loopContext.loopExecutionId, loopContext);
//            return EnumSet.of(NodeStateField.MEMORY);
//        });

        if (loopValue instanceof Iterable) {
            Object loopItem = IterableUtil.get((Iterable<?>) loopValue, loopIndex);
            executeLoopChain(chain, loopContext, loopItem);
        } else if (loopValue instanceof Number || (loopValue instanceof String && StringUtil.isNumeric(loopValue.toString()))) {
            executeLoopChain(chain, loopContext, loopIndex);
        } else {
            throw new IllegalArgumentException("loopValue must be Iterable or Number or String, but loopValue is \"" + loopValue + "\"");
        }

        // 禁用调度下个节点
        return Maps.of(ChainConsts.SCHEDULE_NEXT_NODE_DISABLED_KEY, true)
                .set(ChainConsts.NODE_STATE_STATUS_KEY, NodeStatus.RUNNING);
    }


    /**
     * 获取或创建当前节点的 LoopContext 堆栈（每个 LoopNode 实例独立）
     */
    @SuppressWarnings("unchecked")
    private Deque<LoopContext> getOrCreateLoopStack(Chain chain) {
        NodeState nodeState = chain.getNodeState(this.id);
        String key = buildLoopStackId();
        Object stackObj = nodeState.getMemory().get(key);
        Deque<LoopContext> stack;
        if (stackObj instanceof Deque) {
            stack = (Deque<LoopContext>) stackObj;
        } else {
            stack = new ArrayDeque<>();
            chain.updateNodeStateSafely(this.id, state -> {
                state.getMemory().put(key, stack);
                return EnumSet.of(NodeStateField.MEMORY);
            });
        }
        return stack;
    }


    private void executeLoopChain(Chain chain, LoopContext loopContext, Object loopItem) {
//        Map<String, Object> variables = new HashMap<>();
//        variables.put(this.id + ".index", (loopContext.currentIndex - 1));
//        variables.put(this.id + ".loopItem", loopItem);

        chain.updateStateSafely(state -> {
            ConcurrentHashMap<String, Object> memory = state.getMemory();
            memory.put(this.id + ".index", (loopContext.currentIndex - 1));
            memory.put(this.id + ".loopItem", loopItem);
            return EnumSet.of(ChainStateField.MEMORY);
        });

//        Map<String, Object> payload = createSubPayload(loopContext);

        ChainDefinition definition = chain.getDefinition();
        List<Edge> outwardEdges = definition.getOutwardEdge(this.id);
        for (Edge edge : outwardEdges) {
            Node childNode = definition.getNodeById(edge.getTarget());
            if (childNode.getParentId() != null && childNode.getParentId().equals(this.id)) {
//                chain.scheduleNode(childNode, chain.getStateInstanceId(), edge.getId(), TriggerType.CHILD  , variables, null, 0);
                chain.scheduleNode(childNode, edge.getId(), TriggerType.CHILD, 0);
            }
        }
    }


//    private Map<String, Object> createSubPayload(LoopContext loopContext) {
//        Map<String, Object> payload = new HashMap<>();
//        payload.put(buildLoopId(), loopContext.loopExecutionId);
//        payload.put(buildLoopIndex(), loopContext.currentIndex);
//        return payload;
//    }


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


//    private LoopContext getLoopContext(Trigger prevTrigger, Chain chain) {
//        Map<String, Object> payload = prevTrigger.getPayload();
//
//        // 循环的执行 id（每一次执行，都是不同的执行 id， 1 次执行包含 N 次循环）
//        // 每个执行 id，在当前的 memory 中，对应一个 LoopContext
//        String loopExecutionId = payload == null ? null : (String) payload.get(buildLoopId());
//
//        // 是否是第一次执行
//        boolean isFirstLoop = loopExecutionId == null;
//
//        LoopContext loopContext;
//        if (isFirstLoop) {
//            loopContext = new LoopContext();
//            loopContext.loopExecutionId = UUID.randomUUID().toString();
//
//            loopContext.currentIndex = 0;
//            loopContext.subResult = new HashMap<>();
//            loopContext.triggerPayload = payload;
////            loopContext.trigger = prevTrigger;
//        }
//        // 不是第一次执行
//        else {

    /// /            String stateInstanceId = prevTrigger.getParent().getStateInstanceId();
//            NodeState nodeState = chain.getNodeState(this.id);
//            loopContext = (LoopContext) nodeState.getMemory().get(loopExecutionId);
//        }
//        return loopContext;
//    }
    public int getTriggerLoopIndex(Trigger trigger) {
        Map<String, Object> payload = trigger.getPayload();
        if (payload == null) {
            return 0;
        }
        Object loopIndex = payload.get(buildLoopIndex());
        if (loopIndex == null) {
            return 0;
        }
        return (int) loopIndex;
    }

    private String buildLoopStackId() {
        return this.getId() + "__loop__context";
    }

    private String buildLoopIndex() {
        return this.getId() + "__loop__index";
    }


    public static class LoopContext implements Serializable {
        //        String loopExecutionId;
        int currentIndex;
        Map<String, Object> subResult;
//        Map<String, Object> triggerPayload;

//        public String getLoopExecutionId() {
//            return loopExecutionId;
//        }
//
//        public void setLoopExecutionId(String loopExecutionId) {
//            this.loopExecutionId = loopExecutionId;
//        }

        public int getCurrentIndex() {
            return currentIndex;
        }

        public void setCurrentIndex(int currentIndex) {
            this.currentIndex = currentIndex;
        }

        public Map<String, Object> getSubResult() {
            return subResult;
        }

        public void setSubResult(Map<String, Object> subResult) {
            this.subResult = subResult;
        }

//        public Map<String, Object> getTriggerPayload() {
//            return triggerPayload;
//        }
//
//        public void setTriggerPayload(Map<String, Object> triggerPayload) {
//            this.triggerPayload = triggerPayload;
//        }
    }
}
