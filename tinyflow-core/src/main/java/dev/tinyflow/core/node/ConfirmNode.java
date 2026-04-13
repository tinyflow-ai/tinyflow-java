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
import dev.tinyflow.core.chain.ChainSuspendException;
import dev.tinyflow.core.chain.Parameter;
import dev.tinyflow.core.chain.RefType;
import dev.tinyflow.core.chain.repository.ChainStateField;

import java.util.*;

public class ConfirmNode extends BaseNode {

    private String message;
    private List<Parameter> confirms;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Parameter> getConfirms() {
        return confirms;
    }

    public void setConfirms(List<Parameter> confirms) {
        if (confirms != null) {
            for (Parameter confirm : confirms) {
                confirm.setRefType(RefType.INPUT);
                confirm.setRequired(true); // 必填，才能正确通过 getParameterValuesOnly 获取参数值
                confirm.setName(confirm.getName());
            }
        }
        this.confirms = confirms;
    }


    @Override
    public Map<String, Object> execute(Chain chain) {

        List<Parameter> confirmParameters = new ArrayList<>();
        addConfirmParameter(confirmParameters);

        if (confirms != null) {
            for (Parameter confirm : confirms) {
                Parameter clone = confirm.clone();
                clone.setName(confirm.getName() + "__" + getId());
                clone.setRefType(RefType.INPUT);
                confirmParameters.add(clone);
            }
        }

        Map<String, Object> values;
        try {
            values = chain.getState().resolveParameters(this, confirmParameters);
            // 移除 confirm 参数，方便在其他节点二次确认，或者在 for 循环中第二次获取
            chain.updateStateSafely(state -> {
                for (Parameter confirmParameter : confirmParameters) {
                    state.getMemory().remove(confirmParameter.getName());
                }
                return EnumSet.of(ChainStateField.MEMORY);
            });
        } catch (ChainSuspendException e) {
            chain.updateStateSafely(state -> {
                state.setMessage(message);
                return EnumSet.of(ChainStateField.MESSAGE);
            });

            if (confirms != null) {
                List<Parameter> newParameters = new ArrayList<>();
                for (Parameter confirm : confirms) {
                    Parameter clone = confirm.clone();
                    clone.setName(confirm.getName() + "__" + getId());
                    clone.setRefType(RefType.REF); // 固定为 REF
                    newParameters.add(clone);
                }

                // 获取参数值，不会触发 ChainSuspendException 错误
                Map<String, Object> parameterValues = chain.getState().resolveParameters(this, newParameters, null, true);

                // 设置 enums，方便前端给用户进行选择
                for (Parameter confirmParameter : confirmParameters) {
                    if (confirmParameter.getEnums() == null) {
                        Object enumsObject = parameterValues.get(confirmParameter.getName());
                        confirmParameter.setEnumsObject(enumsObject);
                    }
                }
            }

            throw e;
        }


        Map<String, Object> results = new HashMap<>(values.size());
        values.forEach((key, value) -> {
            int index = key.lastIndexOf("__");
            if (index >= 0) {
                results.put(key.substring(0, index), value);
            } else {
                results.put(key, value);
            }
        });

        return results;
    }


    private void addConfirmParameter(List<Parameter> parameters) {
        // “确认 和 取消” 的参数
        Parameter parameter = new Parameter();
        parameter.setRefType(RefType.INPUT);
        parameter.setId("confirm");
        parameter.setName("confirm__" + getId());
        parameter.setRequired(true);

        List<Object> selectionData = new ArrayList<>();
        selectionData.add("yes");
        selectionData.add("no");

        parameter.setEnums(selectionData);
        parameter.setContentType("text");
        parameter.setFormType("confirm");
        parameters.add(parameter);
    }


}
