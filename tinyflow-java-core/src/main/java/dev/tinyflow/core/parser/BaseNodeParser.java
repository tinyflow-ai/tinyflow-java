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
package dev.tinyflow.core.parser;


import com.agentsflex.core.chain.*;
import com.agentsflex.core.chain.node.BaseNode;
import com.agentsflex.core.util.StringUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import dev.tinyflow.core.Tinyflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public abstract class BaseNodeParser implements NodeParser {

    private static final JSONObject EMPTY_JSON_OBJECT = new JSONObject(Collections.emptyMap());

    public JSONObject getData(JSONObject nodeObject) {
        JSONObject jsonObject = nodeObject.getJSONObject("data");
        return jsonObject != null ? jsonObject : EMPTY_JSON_OBJECT;
    }

    public void addParameters(BaseNode node, JSONObject data) {
        List<Parameter> inputParameters = getParameters(data, "parameters");
        node.setParameters(inputParameters);
    }

    public List<Parameter> getParameters(JSONObject data, String key) {
        return getParameters(data.getJSONArray(key));
    }

    public List<Parameter> getParameters(JSONArray parametersJsonArray) {
        if (parametersJsonArray == null || parametersJsonArray.isEmpty()) {
            return Collections.emptyList();
        }
        List<Parameter> parameters = new ArrayList<>(parametersJsonArray.size());
        for (int i = 0; i < parametersJsonArray.size(); i++) {
            JSONObject parameterJsonObject = parametersJsonArray.getJSONObject(i);
            Parameter parameter = new Parameter();
            parameter.setId(parameterJsonObject.getString("id"));
            parameter.setName(parameterJsonObject.getString("name"));
            parameter.setDescription(parameterJsonObject.getString("description"));
            parameter.setDataType(DataType.ofValue(parameterJsonObject.getString("dataType")));
            parameter.setRef(parameterJsonObject.getString("ref"));
            parameter.setValue(parameterJsonObject.getString("value"));
            parameter.setDefaultValue(parameterJsonObject.getString("defaultValue"));
            parameter.setRefType(RefType.ofValue(parameterJsonObject.getString("refType")));
            parameter.setDataType(DataType.ofValue(parameterJsonObject.getString("dataType")));
            parameter.setRequired(parameterJsonObject.getBooleanValue("required"));
            parameter.setDefaultValue(parameterJsonObject.getString("defaultValue"));

            //新增
            parameter.setContentType(parameterJsonObject.getString("contentType"));
            parameter.setEnums(parameterJsonObject.getJSONArray("enums"));
            parameter.setFormType(parameterJsonObject.getString("formType"));
            parameter.setFormLabel(parameterJsonObject.getString("formLabel"));
            parameter.setFormDescription(parameterJsonObject.getString("formDescription"));

            JSONArray children = parameterJsonObject.getJSONArray("children");
            if (children != null && !children.isEmpty()) {
                parameter.addChildren(getParameters(children));
            }

            parameters.add(parameter);
        }

        return parameters;
    }


    public void addOutputDefs(BaseNode node, JSONObject data) {
        List<Parameter> outputDefs = getParameters(data, "outputDefs");
        if (outputDefs == null || outputDefs.isEmpty()) {
            return;
        }
        node.setOutputDefs(outputDefs);
    }


    @Override
    public ChainNode parse(JSONObject nodeJSONObject, Tinyflow tinyflow) {
        JSONObject data = getData(nodeJSONObject);
        BaseNode node = doParse(nodeJSONObject, data, tinyflow);
        if (node != null) {

            node.setId(nodeJSONObject.getString("id"));
            node.setName(nodeJSONObject.getString("label"));
            node.setDescription(nodeJSONObject.getString("description"));

            if (!data.isEmpty()) {

                addParameters(node, data);
                addOutputDefs(node, data);

                String conditionString = data.getString("condition");

                if (StringUtil.hasText(conditionString)) {
                    node.setCondition(new JsCodeCondition(conditionString.trim()));
                }

                Boolean async = data.getBoolean("async");
                if (async != null) {
                    node.setAsync(async);
                }

                String name = data.getString("title");
                if (StringUtil.hasText(name)) {
                    node.setName(name);
                }

                String description = data.getString("description");
                if (StringUtil.hasText(description)) {
                    node.setDescription(description);
                }

                Boolean loopEnable = data.getBoolean("loopEnable");
                if (loopEnable != null) {
                    node.setLoopEnable(loopEnable);
                }

                Long loopIntervalMs = data.getLong("loopIntervalMs");
                if (loopIntervalMs != null) {
                    node.setLoopIntervalMs(loopIntervalMs);
                }

                Integer maxLoopCount = data.getInteger("maxLoopCount");
                if (maxLoopCount != null) {
                    node.setMaxLoopCount(maxLoopCount);
                }

                String loopBreakCondition = data.getString("loopBreakCondition");
                if (StringUtil.hasText(loopBreakCondition)) {
                    node.setLoopBreakCondition(new JsCodeCondition(loopBreakCondition.trim()));
                }
            }
        }

        return node;
    }

    protected abstract BaseNode doParse(JSONObject root, JSONObject data, Tinyflow tinyflow);
}
