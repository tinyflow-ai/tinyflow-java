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


import com.agentsflex.core.chain.DataType;
import com.agentsflex.core.chain.Parameter;
import com.agentsflex.core.chain.RefType;
import com.agentsflex.core.chain.node.BaseNode;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

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
            parameter.setRefType(RefType.ofValue(parameterJsonObject.getString("refType")));
            parameter.setRequired(parameterJsonObject.getBooleanValue("required"));
            parameter.setDefaultValue(parameterJsonObject.getString("defaultValue"));
            parameter.setValue(parameterJsonObject.getString("value"));

            JSONArray children = parameterJsonObject.getJSONArray("children");
            if (children != null && !children.isEmpty()) {
                parameter.addChildren(getParameters(children));
            }

            parameters.add(parameter);
        }

        return parameters;
    }


    public void addOutputDefs(BaseNode node, JSONObject data) {
        JSONArray outputDefs = data.getJSONArray("outputDefs");
        if (outputDefs == null || outputDefs.isEmpty()) {
            return;
        }

        for (int i = 0; i < outputDefs.size(); i++) {
            JSONObject outputDefJsonObject = outputDefs.getJSONObject(i);
            Parameter parameter = new Parameter();
            parameter.setId(outputDefJsonObject.getString("id"));
            parameter.setName(outputDefJsonObject.getString("name"));
            parameter.setDescription(outputDefJsonObject.getString("description"));
            parameter.setRef(outputDefJsonObject.getString("ref"));
            parameter.setValue(outputDefJsonObject.getString("value"));
            parameter.setDefaultValue(outputDefJsonObject.getString("defaultValue"));
            parameter.setRefType(RefType.ofValue(outputDefJsonObject.getString("refType")));
            parameter.setDataType(DataType.ofValue(outputDefJsonObject.getString("dataType")));

            JSONArray children = outputDefJsonObject.getJSONArray("children");
            if (children != null && !children.isEmpty()) {
                parameter.addChildren(getParameters(children));
            }

            node.addOutputDef(parameter);
        }
    }
}
