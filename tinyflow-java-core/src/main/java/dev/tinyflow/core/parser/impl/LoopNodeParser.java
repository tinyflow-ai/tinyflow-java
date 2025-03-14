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

import com.agentsflex.core.chain.Chain;
import com.agentsflex.core.chain.ChainNode;
import com.agentsflex.core.chain.Parameter;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import dev.tinyflow.core.Tinyflow;
import dev.tinyflow.core.node.LoopNode;
import dev.tinyflow.core.parser.BaseNodeParser;
import dev.tinyflow.core.parser.ChainParser;

import java.util.List;

public class LoopNodeParser extends BaseNodeParser {

    @Override
    public ChainNode parse(JSONObject nodeJSONObject, Tinyflow tinyflow) {
        LoopNode loopNode = new LoopNode();
        JSONObject data = getData(nodeJSONObject);
        loopNode.setName(data.getString("label"));

        List<Parameter> loopParameters = getParameters(data, "loopVar");
        if (!loopParameters.isEmpty()) {
            loopNode.setLoopVar(loopParameters.get(0));
        }


        String jsonString = tinyflow.getData();
        JSONObject root = JSON.parseObject(jsonString);
        JSONArray nodes = root.getJSONArray("nodes");
        JSONArray edges = root.getJSONArray("edges");

        Chain chain = ChainParser.parse(tinyflow, nodes, edges, nodeJSONObject);
        loopNode.setLoopChain(chain);


        addOutputDefs(loopNode, data);
        return loopNode;
    }
}
