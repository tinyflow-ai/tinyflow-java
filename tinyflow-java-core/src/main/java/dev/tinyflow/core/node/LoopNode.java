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

import com.agentsflex.core.chain.Chain;
import com.agentsflex.core.chain.Parameter;
import com.agentsflex.core.chain.RefType;
import com.agentsflex.core.chain.node.BaseNode;
import com.agentsflex.core.util.Maps;

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
        loopChain.setParent(chain);
        Map<String, Object> loopVars = getChainParameters(chain, Collections.singletonList(loopVar));
        Maps result = Maps.of();
        Object value = loopVars.get(loopVar.getName());
        if (value instanceof Iterable) {
            Iterable<?> iterable = (Iterable<?>) value;
            int index = 0;
            for (Object o : iterable) {
                Map<String, Object> loopParams = new HashMap<>();
                loopParams.put("loopItem", o);
                loopParams.put("index", index++);
                loopParams.putAll(loopVars);
                loopParams.putAll(chain.getMemory().getAll());
                loopChain.execute(loopParams);
                fillResult(result, loopChain.getMemory().getAll());
            }
        } else if (value instanceof Number) {
            int count = ((Number) value).intValue();
            for (int i = 0; i < count; i++) {
                Map<String, Object> loopParams = new HashMap<>();
                loopParams.put("loopItem", i);
                loopParams.put("index", i);
                loopParams.putAll(loopVars);
                loopParams.putAll(chain.getMemory().getAll());
                loopChain.execute(loopParams);
                fillResult(result, loopChain.getMemory().getAll());
            }
        }

        List<Parameter> outputDefs = getOutputDefs();
        if (outputDefs != null) {
            for (Parameter outputDef : outputDefs) {
                if (outputDef.getRefType() == RefType.INPUT) {
                    result.put(outputDef.getName(), outputDef.getRef());
                }
            }
        }
        return result;
    }

    /**
     * 把子流程执行的结果填充到主流程的输出参数中
     *
     * @param result        主流程的输出参数
     * @param executeResult 子流程的执行结果
     */
    private void fillResult(Maps result, Map<String, Object> executeResult) {
        List<Parameter> outputDefs = getOutputDefs();
        if (outputDefs != null) {
            for (Parameter outputDef : outputDefs) {
                if (outputDef.getRefType() == RefType.REF) {
                    Object value = executeResult.get(outputDef.getRef());
                    @SuppressWarnings("unchecked") List<Object> list = (List<Object>) result.get(outputDef.getName());
                    if (list == null) list = new ArrayList<>();
                    list.add(value);
                    result.put(outputDef.getName(), list);
                }
            }
        }
    }
}
