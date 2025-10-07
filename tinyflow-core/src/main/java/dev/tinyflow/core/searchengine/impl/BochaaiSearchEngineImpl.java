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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import dev.tinyflow.core.chain.Chain;
import dev.tinyflow.core.node.SearchEngineNode;
import dev.tinyflow.core.searchengine.BaseSearchEngine;
import dev.tinyflow.core.util.Maps;
import dev.tinyflow.core.util.OKHttpClientWrapper;

import java.util.*;

public class BochaaiSearchEngineImpl extends BaseSearchEngine {

    private static final String DEFAULT_API_URL = "https://api.bochaai.com/v1/ai-search";
    private OKHttpClientWrapper okHttpClientWrapper = new OKHttpClientWrapper();

    public BochaaiSearchEngineImpl() {
        setApiUrl(DEFAULT_API_URL);
    }

    public OKHttpClientWrapper getOkHttpClientWrapper() {
        return okHttpClientWrapper;
    }

    public void setOkHttpClientWrapper(OKHttpClientWrapper okHttpClientWrapper) {
        this.okHttpClientWrapper = okHttpClientWrapper;
    }

    @Override
    public List<Map<String, Object>> search(String keyword, int limit, SearchEngineNode searchEngineNode, Chain chain) {

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + apiKey);
        headers.put("Content-Type", "application/json");

        String jsonString = Maps.of("query", keyword)
                .set("summary", true).
                set("freshness", "noLimit")
                .set("count", limit)
                .set("stream", false)
                .toJSON();


        String responseString = okHttpClientWrapper.post(apiUrl, headers, jsonString);
        JSONObject object = JSON.parseObject(responseString);

        if (200 == object.getIntValue("code")) {
            JSONArray messages = object.getJSONArray("messages");
            List<Map<String, Object>> result = new ArrayList<>();
            for (int i = 0; i < messages.size(); i++) {
                result.add(messages.getJSONObject(i));
            }
            return result;
        }

        return Collections.emptyList();
    }
}
