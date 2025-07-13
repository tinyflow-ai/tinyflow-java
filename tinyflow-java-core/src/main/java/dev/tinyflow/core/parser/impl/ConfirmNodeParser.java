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
package dev.tinyflow.core.parser.impl;

import com.agentsflex.core.chain.DataType;
import com.agentsflex.core.chain.RefType;
import com.agentsflex.core.chain.node.BaseNode;
import com.agentsflex.core.chain.node.ConfirmNode;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import dev.tinyflow.core.Tinyflow;
import dev.tinyflow.core.parser.BaseNodeParser;

import java.util.ArrayList;
import java.util.List;

public class ConfirmNodeParser extends BaseNodeParser {

    @Override
    public BaseNode doParse(JSONObject root, JSONObject data, Tinyflow tinyflow) {

        ConfirmNode confirmNode = new ConfirmNode();
        confirmNode.setMessage(data.getString("message"));

        JSONArray confirmsJSONArray = data.getJSONArray("confirms");
        if (confirmsJSONArray != null && !confirmsJSONArray.isEmpty()) {
            List<ConfirmNode.ConfirmParameter> parameters = new ArrayList<>(confirmsJSONArray.size());
            for (int i = 0; i < confirmsJSONArray.size(); i++) {
                JSONObject parameterJsonObject = confirmsJSONArray.getJSONObject(i);
                ConfirmNode.ConfirmParameter parameter = new ConfirmNode.ConfirmParameter();

                // Parameter 基础信息
                parameter.setId(parameterJsonObject.getString("id"));
                parameter.setName(parameterJsonObject.getString("name"));
                parameter.setDescription(parameterJsonObject.getString("description"));
                parameter.setDataType(DataType.ofValue(parameterJsonObject.getString("dataType")));
                parameter.setRef(parameterJsonObject.getString("ref"));
                parameter.setRefType(RefType.ofValue(parameterJsonObject.getString("refType")));
                parameter.setRequired(parameterJsonObject.getBooleanValue("required"));
                parameter.setDefaultValue(parameterJsonObject.getString("defaultValue"));
                parameter.setValue(parameterJsonObject.getString("value"));

                // ConfirmParameter 内容
                parameter.setSelectionDataType(parameterJsonObject.getString("selectionDataType"));
                parameter.setSelectionMode(parameterJsonObject.getString("selectionMode"));

                parameters.add(parameter);
            }
            confirmNode.setConfirms(parameters);
        }


        return confirmNode;
    }
}
