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
            JSONObject inputParam = parametersJsonArray.getJSONObject(i);
            Parameter parameter = new Parameter();
            parameter.setId(inputParam.getString("id"));
            parameter.setName(inputParam.getString("name"));
            parameter.setDescription(inputParam.getString("description"));
            parameter.setDataType(DataType.ofValue(inputParam.getString("dataType")));
            parameter.setRef(inputParam.getString("ref"));
            parameter.setRefType(RefType.ofValue(inputParam.getString("refType")));
            parameter.setRequired(inputParam.getBooleanValue("required"));
            parameter.setDefaultValue(inputParam.getString("defaultValue"));


            JSONArray childrenJSONArray = inputParam.getJSONArray("children");
            if (childrenJSONArray != null && !childrenJSONArray.isEmpty()) {
                parameter.addChildren(getParameters(childrenJSONArray));
            }

            parameters.add(parameter);
        }

        return parameters;
    }


    public void addOutputDefs(BaseNode node, JSONObject data) {
        JSONArray outputDefs = data.getJSONArray("outputDefs");
        if (outputDefs != null) for (int i = 0; i < outputDefs.size(); i++) {
            JSONObject outputDef = outputDefs.getJSONObject(i);
            Parameter parameter = new Parameter();
            parameter.setName(outputDef.getString("name"));
            parameter.setDescription(outputDef.getString("description"));
            parameter.setRef(outputDef.getString("ref"));
            parameter.setRefType(RefType.ofValue(outputDef.getString("refType")));
            parameter.setDataType(DataType.ofValue(outputDef.getString("dataType")));
            node.addOutputDef(parameter);
        }
    }
}
