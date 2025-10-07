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


import dev.tinyflow.core.chain.Chain;
import dev.tinyflow.core.chain.ChainStatus;
import dev.tinyflow.core.chain.Parameter;
import dev.tinyflow.core.chain.RefType;

import java.util.*;

public class LoopNode extends BaseNode {

    private Parameter loopVar;
    private Chain loopChain;

    public Parameter getLoopVar() {
        return loopVar;
    }

    public void setLoopVar(Parameter loopVar) {
        this.loopVar = loopVar;
    }

    public Chain getLoopChain() {
        return loopChain;
    }

    public void setLoopChain(Chain loopChain) {
        this.loopChain = loopChain;
    }

    @Override
    protected Map<String, Object> execute(Chain chain) {
        loopChain.setStatus(ChainStatus.READY);

        // 复制父流程的参数
        loopChain.setEventListeners(chain.getEventListeners());
        loopChain.setOutputListeners(chain.getOutputListeners());
        loopChain.setChainErrorListeners(chain.getChainErrorListeners());
        loopChain.setNodeErrorListeners(chain.getNodeErrorListeners());
        loopChain.setSuspendNodes(chain.getSuspendNodes());


        Map<String, Object> executeResult = new HashMap<>();
        Map<String, Object> chainMemory = chain.getMemory();

        Map<String, Object> loopVars = chain.getParameterValues(this, Collections.singletonList(loopVar));
        Object loopValue = loopVars.get(loopVar.getName());
        if (loopValue instanceof Iterable) {
            Iterable<?> iterable = (Iterable<?>) loopValue;
            int index = 0;
            for (Object o : iterable) {
                if (this.loopChain.getStatus() != ChainStatus.READY) {
                    break;
                }
                executeLoopChain(index++, o, chainMemory, executeResult);
            }
        } else if (loopValue instanceof Number || (loopValue instanceof String && isNumeric(loopValue.toString()))) {
            int count = loopValue instanceof Number ? ((Number) loopValue).intValue() : Integer.parseInt(loopValue.toString().trim());
            for (int i = 0; i < count; i++) {
                if (this.loopChain.getStatus() != ChainStatus.READY) {
                    break;
                }
                executeLoopChain(i, i, chainMemory, executeResult);
            }
        }

        return executeResult;
    }

    private void executeLoopChain(int index, Object loopItem, Map<String, Object> parentMap, Map<String, Object> executeResult) {
        Map<String, Object> loopParams = new HashMap<>();
        loopParams.put(this.id + ".index", index);
        loopParams.put(this.id + ".loopItem", loopItem);
        loopParams.putAll(parentMap);
        try {
            loopChain.execute(loopParams);
        } finally {
            // 正常结束的情况下，填充结果
            if (loopChain.getStatus() == ChainStatus.FINISHED_NORMAL) {
                fillResult(executeResult, loopChain);

                //重置 chain statue 为 ready
                loopChain.reset();
            }
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
     * @param executeResult 主流程的输出参数
     * @param loopChain     子流程的
     */
    private void fillResult(Map<String, Object> executeResult, Chain loopChain) {
        List<Parameter> outputDefs = getOutputDefs();
        if (outputDefs != null) {
            for (Parameter outputDef : outputDefs) {
                Object value = null;

                //引用
                if (outputDef.getRefType() == RefType.REF) {
                    value = loopChain.get(outputDef.getRef());
                }
                //固定值
                else if (outputDef.getRefType() == RefType.FIXED) {
                    value = outputDef.getValue();
                }

                @SuppressWarnings("unchecked") List<Object> existList = (List<Object>) executeResult.get(outputDef.getName());
                if (existList == null) {
                    existList = new ArrayList<>();
                }
                existList.add(value);
                executeResult.put(outputDef.getName(), existList);
            }
        }
    }
}
