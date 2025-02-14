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
import com.agentsflex.core.chain.InputParameter;
import com.agentsflex.core.chain.OutputKey;
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

    public void addInputParameters(BaseNode node, JSONObject data) {
        List<InputParameter> inputParameters = getInputParameters(data, "inputParams");
        node.setInputParameters(inputParameters);
    }

    public List<InputParameter> getInputParameters(JSONObject data, String key) {
        JSONArray inputParams = data.getJSONArray(key);
        if (inputParams == null || inputParams.isEmpty()) {
            return Collections.emptyList();
        }
        List<InputParameter> parameters = new ArrayList<>(inputParams.size());
        for (int i = 0; i < inputParams.size(); i++) {
            JSONObject inputParam = inputParams.getJSONObject(i);
            InputParameter parameter = new InputParameter();
            parameter.setName(inputParam.getString("name"));
            parameter.setDescription(inputParam.getString("description"));
            parameter.setRef(inputParam.getString("ref"));
            parameter.setRefType(RefType.ofValue(inputParam.getString("refType")));
            parameter.setDataType(DataType.ofValue(inputParam.getString("dataType")));
            parameter.setRequired(inputParam.getBooleanValue("required"));
            parameters.add(parameter);
        }

        return parameters;
    }

    public void addOutputKeys(BaseNode node, JSONObject data) {
        JSONArray outputParams = data.getJSONArray("outputParams");
        if (outputParams != null) for (int i = 0; i < outputParams.size(); i++) {
            JSONObject outputParam = outputParams.getJSONObject(i);
            OutputKey outputKey = new OutputKey();
            outputKey.setName(outputParam.getString("name"));
            outputKey.setDescription(outputParam.getString("description"));
            outputKey.setRef(outputParam.getString("ref"));
            outputKey.setRefType(RefType.ofValue(outputParam.getString("refType")));
            outputKey.setDataType(DataType.ofValue(outputParam.getString("dataType")));
            node.addOutputKey(outputKey);
        }
    }
}
