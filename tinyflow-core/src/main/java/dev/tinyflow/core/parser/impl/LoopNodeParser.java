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

import dev.tinyflow.core.chain.Chain;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import dev.tinyflow.core.Tinyflow;
import dev.tinyflow.core.chain.Parameter;
import dev.tinyflow.core.node.LoopNode;
import dev.tinyflow.core.parser.BaseNodeParser;

import java.util.List;

public class LoopNodeParser extends BaseNodeParser<LoopNode> {

    @Override
    public LoopNode doParse(JSONObject root, JSONObject data, Tinyflow tinyflow) {
        LoopNode loopNode = new LoopNode();
        List<Parameter> loopVars = getParameters(data, "loopVars");
        if (!loopVars.isEmpty()) {
            loopNode.setLoopVar(loopVars.get(0));
        }

        String jsonString = tinyflow.getData();
        JSONObject flowRoot = JSON.parseObject(jsonString);
        JSONArray nodes = flowRoot.getJSONArray("nodes");
        JSONArray edges = flowRoot.getJSONArray("edges");

        Chain chain = tinyflow.getChainParser().parse(tinyflow, nodes, edges, root);
        loopNode.setLoopChain(chain);

        return loopNode;
    }
}
