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

import com.agentsflex.core.document.Document;
import com.agentsflex.core.llm.client.HttpClient;
import com.agentsflex.core.util.Maps;
import dev.tinyflow.core.searchengine.SearchEngine;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BochaaiSearchEngineImpl implements SearchEngine {

    private String url = "https://api.bochaai.com/v1/ai-search";
    private String apiKey;
    private HttpClient httpClient = new HttpClient();

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public List<Document> search(String keyword, int limit) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + apiKey);
        headers.put("Content-Type", "application/json");

        String jsonString = Maps.of("query", keyword)
                .set("summary", true).
                set("freshness", "noLimit")
                .set("count", limit)
                .set("stream", false)
                .toJSON();

        String responseString = httpClient.post(url, headers, jsonString);

        return Collections.emptyList();
    }
}
