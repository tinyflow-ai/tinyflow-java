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

import com.alibaba.fastjson.JSONObject;
import dev.tinyflow.core.chain.Parameter;
import dev.tinyflow.core.node.LoopNode;
import dev.tinyflow.core.parser.BaseNodeParser;

import java.util.List;

public class LoopNodeParser extends BaseNodeParser<LoopNode> {

    @Override
    public LoopNode doParse(JSONObject root, JSONObject data, JSONObject chainJSONObject) {
        LoopNode loopNode = new LoopNode();

        // 这里需要设置 id，先设置 id 后， loopNode.setLoopChain(chain); 才能取获取当前节点的 id
//        loopNode.setId(root.getString("id"));

        List<Parameter> loopVars = getParameters(data, "loopVars");
        if (!loopVars.isEmpty()) {
            loopNode.setLoopVar(loopVars.get(0));
        }

//        JSONArray nodes = chainJSONObject.getJSONArray("nodes");
//        JSONArray edges = chainJSONObject.getJSONArray("edges");

//        ChainDefinition chain = getChainParser().parse(chainJSONObject, nodes, edges, root);
//        loopNode.setLoopChain(chain);

        return loopNode;
    }
}
