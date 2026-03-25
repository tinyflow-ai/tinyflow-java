package dev.tinyflow.core.parser.impl;

import com.alibaba.fastjson.JSONObject;
import dev.tinyflow.core.chain.Parameter;
import dev.tinyflow.core.llm.Llm;
import dev.tinyflow.core.node.LlmNode;
import dev.tinyflow.core.parser.BaseNodeParser;

import java.util.List;

public class LlmNodeParser extends BaseNodeParser<LlmNode> {

    @Override
    public LlmNode doParse(JSONObject root, JSONObject data, JSONObject chainJSONObject) {
        LlmNode llmNode = new LlmNode();
        llmNode.setLlmId(data.getString("llmId"));
        llmNode.setUserPrompt(data.getString("userPrompt"));
        llmNode.setSystemPrompt(data.getString("systemPrompt"));
        llmNode.setOutType(data.getString("outType"));
        llmNode.setJsonSchema(data.getString("jsonSchema"));

        
        Llm.ChatOptions chatOptions = new Llm.ChatOptions();
        chatOptions.setTopK(data.containsKey("topK") ? data.getInteger("topK") : 10);
        chatOptions.setTopP(data.containsKey("topP") ? data.getFloat("topP") : 0.8F);
        chatOptions.setTemperature(data.containsKey("temperature") ? data.getFloat("temperature") : 0.5F);
        llmNode.setChatOptions(chatOptions);

//        LlmProvider llmProvider = tinyflow.getLlmProvider();
//        if (llmProvider != null) {
//            Llm llm = llmProvider.getLlm(data.getString("llmId"));
//            llmNode.setLlm(llm);
//        }

        // 支持图片识别输入
        List<Parameter> images = getParameters(data, "images");
        llmNode.setImages(images);

        return llmNode;
    }
}
