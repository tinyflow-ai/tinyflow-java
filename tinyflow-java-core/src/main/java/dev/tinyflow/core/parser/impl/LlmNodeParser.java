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

import com.agentsflex.core.chain.node.BaseNode;
import com.agentsflex.core.chain.node.LlmNode;
import com.agentsflex.core.llm.ChatOptions;
import com.agentsflex.core.llm.Llm;
import com.alibaba.fastjson.JSONObject;
import dev.tinyflow.core.Tinyflow;
import dev.tinyflow.core.parser.BaseNodeParser;
import dev.tinyflow.core.provider.LlmProvider;

public class LlmNodeParser extends BaseNodeParser {

    @Override
    public BaseNode doParse(JSONObject root, JSONObject data, Tinyflow tinyflow) {
        LlmNode llmNode = new LlmNode();
        llmNode.setUserPrompt(data.getString("userPrompt"));
        llmNode.setSystemPrompt(data.getString("systemPrompt"));
        llmNode.setOutType(data.getString("outType"));


        ChatOptions chatOptions = new ChatOptions();
        chatOptions.setTopK(data.containsKey("topK") ? data.getInteger("topK") : 10);
        chatOptions.setTopP(data.containsKey("topP") ? data.getFloat("topP") : 0.8F);
        chatOptions.setTemperature(data.containsKey("temperature") ? data.getFloat("temperature") : 0.8F);
        llmNode.setChatOptions(chatOptions);

        LlmProvider llmProvider = tinyflow.getLlmProvider();
        if (llmProvider != null) {
            Llm llm = llmProvider.getLlm(data.getString("llmId"));
            llmNode.setLlm(llm);
        }

        return llmNode;
    }
}
