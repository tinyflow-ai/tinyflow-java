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
package dev.tinyflow.core.searchengine.impl;

import com.agentsflex.core.chain.Chain;
import com.agentsflex.core.document.Document;
import com.agentsflex.core.util.Maps;
import dev.tinyflow.core.node.SearchEngineNode;
import dev.tinyflow.core.searchengine.BaseSearchEngine;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BochaaiSearchEngineImpl extends BaseSearchEngine {

    private static final String DEFAULT_API_URL = "https://api.bochaai.com/v1/ai-search";

    public BochaaiSearchEngineImpl() {
        setApiUrl(DEFAULT_API_URL);
    }


    @Override
    public List<Document> search(String keyword, int limit, SearchEngineNode searchEngineNode, Chain chain) {

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + apiKey);
        headers.put("Content-Type", "application/json");

        String jsonString = Maps.of("query", keyword)
                .set("summary", true).
                set("freshness", "noLimit")
                .set("count", limit)
                .set("stream", false)
                .toJSON();

        String responseString = httpClient.post(apiUrl, headers, jsonString);


        return Collections.emptyList();
    }
}
