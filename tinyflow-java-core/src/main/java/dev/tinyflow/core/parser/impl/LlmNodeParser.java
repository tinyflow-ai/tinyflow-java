package dev.tinyflow.core.parser.impl;

import com.alibaba.fastjson.JSONObject;
import dev.tinyflow.core.Tinyflow;
import dev.tinyflow.core.chain.Parameter;
import dev.tinyflow.core.llm.ChatModel;
import dev.tinyflow.core.node.LlmNode;
import dev.tinyflow.core.parser.BaseNodeParser;

import java.util.List;

public class LlmNodeParser extends BaseNodeParser<LlmNode> {

    @Override
    public LlmNode doParse(JSONObject root, JSONObject data, Tinyflow tinyflow) {
        LlmNode llmNode = new LlmNode();
        llmNode.setLlmId(data.getString("llmId"));
        llmNode.setUserPrompt(data.getString("userPrompt"));
        llmNode.setSystemPrompt(data.getString("systemPrompt"));
        llmNode.setOutType(data.getString("outType"));


        ChatModel.ChatOptions chatOptions = new ChatModel.ChatOptions();
        chatOptions.setTopK(data.containsKey("topK") ? data.getInteger("topK") : 10);
        chatOptions.setTopP(data.containsKey("topP") ? data.getFloat("topP") : 0.8F);
        chatOptions.setTemperature(data.containsKey("temperature") ? data.getFloat("temperature") : 0.8F);
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
