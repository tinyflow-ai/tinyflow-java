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

import com.agentsflex.core.chain.Parameter;
import com.agentsflex.core.chain.node.BaseNode;
import com.alibaba.fastjson.JSONObject;
import dev.tinyflow.core.Tinyflow;
import dev.tinyflow.core.node.HttpNode;
import dev.tinyflow.core.parser.BaseNodeParser;

import java.util.List;

public class HttpNodeParser extends BaseNodeParser {

    @Override
    public BaseNode doParse(JSONObject root, JSONObject data, Tinyflow tinyflow) {
        HttpNode httpNode = new HttpNode();
        httpNode.setUrl(data.getString("url"));
        httpNode.setMethod(data.getString("method"));
        httpNode.setBodyJson(data.getString("bodyJson"));
        httpNode.setRawBody(data.getString("rawBody"));
        httpNode.setBodyType(data.getString("bodyType"));
        httpNode.setFileStorage(tinyflow.getFileStorage());

        List<Parameter> headers = getParameters(data, "headers");
        httpNode.setHeaders(headers);

        List<Parameter> formData = getParameters(data, "formData");
        httpNode.setFormData(formData);

        List<Parameter> formUrlencoded = getParameters(data, "formUrlencoded");
        httpNode.setFormUrlencoded(formUrlencoded);

        return httpNode;
    }
}
