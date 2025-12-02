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
import dev.tinyflow.core.chain.repository.NodeStateField;
import dev.tinyflow.core.chain.runtime.Trigger;
import dev.tinyflow.core.chain.runtime.TriggerContext;
import dev.tinyflow.core.chain.runtime.TriggerType;
import dev.tinyflow.core.util.IterableUtil;
import dev.tinyflow.core.util.StringUtil;

import java.util.*;

public class LoopNode extends BaseNode {

    private Parameter loopVar;
    private ChainDefinition loopChainDefinition;

    public Parameter getLoopVar() {
        return loopVar;
    }

    public void setLoopVar(Parameter loopVar) {
        this.loopVar = loopVar;
    }

    public ChainDefinition getLoopChain() {
        return loopChainDefinition;
    }

    public void setLoopChain(ChainDefinition loopChain) {
        List<Node> nodes = loopChain.getNodes();
        for (Node node : nodes) {
            List<Edge> outwardEdges = node.getOutwardEdges();
            if (outwardEdges == null || outwardEdges.isEmpty()) {
                Edge edge = new Edge();
                edge.setId(UUID.randomUUID().toString());
                edge.setSource(node.getId());
                edge.setTarget(this.getId());
                loopChain.addEdge(edge);
            }
        }
        this.loopChainDefinition = loopChain;
    }

    @Override
    protected Map<String, Object> execute(Chain chain) {

        Trigger trigger = TriggerContext.getCurrentTrigger();
        String parentInstanceId = trigger.getParentInstanceId();

        ChainState parentChainState;
        int currentIndex;// 循环次数的定义
        Map<String, Object> subResult;  // 执行结果的定义

        String executeParentInstanceId;
        String executeCurrentStateId;

        // 第一次进入，还没开始执行子循环
        if (StringUtil.noText(parentInstanceId)) {
            parentChainState = chain.getState();
            currentIndex = 0;
            subResult = new HashMap<>();

            executeParentInstanceId = parentInstanceId;
            executeCurrentStateId = UUID.randomUUID().toString();
        }
        // 由子循环触发（至少完成了第一次循环）
        else {

            parentChainState = chain.getState(parentInstanceId);
            NodeState parentNodeState = chain.getNodeStateRepository().load(parentInstanceId, this.getId());
            // 循环次数的定义
            currentIndex = parentNodeState.getMemoryOrDefault("currentIndex", 0);
            // 执行结果的定义
            subResult = parentNodeState.getMemoryOrDefault("subResult", new HashMap<>());

            executeParentInstanceId = chain.getStateInstanceId();
            executeCurrentStateId = parentNodeState.getMemoryOrDefault("currentStateId", UUID.randomUUID().toString());
        }


        Map<String, Object> chainMemory = parentChainState.getMemory();
        Map<String, Object> loopVars = parentChainState.resolveParameters(this, Collections.singletonList(loopVar));
        Object loopValue = loopVars.get(loopVar.getName());

        int shouldLoopCount = 0;
        if (loopValue instanceof Iterable) {
            shouldLoopCount = IterableUtil.size((Iterable<?>) loopValue);
        } else if (loopValue instanceof Number || (loopValue instanceof String && isNumeric(loopValue.toString()))) {
            shouldLoopCount = loopValue instanceof Number ? ((Number) loopValue).intValue() : Integer.parseInt(loopValue.toString().trim());
        }

        // 执行的次数够了,恢复父级触发
        if (currentIndex >= shouldLoopCount) {
            trigger.setParentInstanceId(null);
            return subResult;
        }
        // 执行的次数不够
        else {
            // 不是第一次执行
            if (currentIndex != 0) {
                ChainState childState = chain.getState();
                mergeResult(subResult, childState);
            }
        }


        if (loopValue instanceof Iterable) {
            Iterable<?> iterable = (Iterable<?>) loopValue;
            Object loopItem = null;
            int loopIndex = 0;
            for (Object o : iterable) {
                if (currentIndex == loopIndex++) {
                    loopItem = o;
                    break;
                }
            }
            executeLoopChain(chain, executeParentInstanceId, executeCurrentStateId, currentIndex, loopItem, chainMemory);
        } else if (loopValue instanceof Number || (loopValue instanceof String && isNumeric(loopValue.toString()))) {
            int count = loopValue instanceof Number ? ((Number) loopValue).intValue() : Integer.parseInt(loopValue.toString().trim());
            if (currentIndex < count) {
                executeLoopChain(chain, executeParentInstanceId, executeCurrentStateId, currentIndex, currentIndex, chainMemory);
            }
        }


        chain.updateNodeStateSafely(parentChainState.getInstanceId(), this.getId(), state -> {
            // 保存当前索引和结果
            state.addMemory("currentIndex", currentIndex + 1);
            state.addMemory("subResult", subResult);
            return EnumSet.of(NodeStateField.MEMORY);
        });

        return subResult;
    }

    private void executeLoopChain(Chain chain, String parentStateId, String currentStateId, int index, Object loopItem, Map<String, Object> parentMap) {
        Map<String, Object> loopParams = new HashMap<>();
        loopParams.put(this.id + ".index", index);
        loopParams.put(this.id + ".loopItem", loopItem);
        loopParams.putAll(parentMap);
        // 调度入口节点
        List<Node> startNodes = loopChainDefinition.getStartNodes();
        for (Node startNode : startNodes) {
            chain.scheduleNode(startNode, parentStateId, currentStateId, null, TriggerType.NEXT, loopParams, 0);
        }
    }

    /**
     * 判断字符串是否是数字
     *
     * @param string 需要判断的字符串
     * @return boolean 是数字返回 true，否则返回 false
     */
    private boolean isNumeric(String string) {
        if (string == null || string.isEmpty()) {
            return false;
        }
        char[] chars = string.trim().toCharArray();
        for (char c : chars) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 把子流程执行的结果填充到主流程的输出参数中
     *
     * @param toResult   主流程的输出参数
     * @param childState 子流程的
     */
    private void mergeResult(Map<String, Object> toResult, ChainState childState) {
        List<Parameter> outputDefs = getOutputDefs();
        if (outputDefs != null) {
            for (Parameter outputDef : outputDefs) {
                Object value = null;

                //引用
                if (outputDef.getRefType() == RefType.REF) {
                    value = childState.resolveValue(outputDef.getRef());
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
}
