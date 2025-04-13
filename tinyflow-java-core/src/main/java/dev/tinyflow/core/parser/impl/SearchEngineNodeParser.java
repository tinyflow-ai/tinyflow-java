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

import com.agentsflex.core.chain.ChainNode;
import com.alibaba.fastjson.JSONObject;
import dev.tinyflow.core.Tinyflow;
import dev.tinyflow.core.node.SearchEngineNode;
import dev.tinyflow.core.parser.BaseNodeParser;
import dev.tinyflow.core.searchengine.SearchEngine;

public class SearchEngineNodeParser extends BaseNodeParser {

    @Override
    public ChainNode parse(JSONObject nodeJSONObject, Tinyflow tinyflow) {
        SearchEngineNode searchEngineNode = new SearchEngineNode();
        JSONObject data = getData(nodeJSONObject);
        searchEngineNode.setName(data.getString("label"));
        searchEngineNode.setKeyword(data.getString("keyword"));
        searchEngineNode.setLimit(data.getString("limit"));

        String engine = data.getString("engine");
        searchEngineNode.setEngine(engine);
        
        if (tinyflow.getSearchEngineProvider() != null) {
            SearchEngine searchEngine = tinyflow.getSearchEngineProvider().getSearchEngine(engine);
            searchEngineNode.setSearchEngine(searchEngine);
        }

        addParameters(searchEngineNode, data);
        addOutputDefs(searchEngineNode, data);

        return searchEngineNode;
    }
}
