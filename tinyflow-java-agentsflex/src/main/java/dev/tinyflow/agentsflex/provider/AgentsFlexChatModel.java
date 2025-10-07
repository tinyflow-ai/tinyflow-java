package dev.tinyflow.agentsflex.provider;

import com.agentsflex.core.llm.Llm;
import com.agentsflex.core.llm.response.AiMessageResponse;
import com.agentsflex.core.message.SystemMessage;
import com.agentsflex.core.prompt.ImagePrompt;
import com.agentsflex.core.prompt.TextPrompt;
import dev.tinyflow.core.chain.Chain;
import dev.tinyflow.core.llm.ChatModel;
import dev.tinyflow.core.node.LlmNode;
import dev.tinyflow.core.util.StringUtil;

import java.util.List;

public class AgentsFlexChatModel implements ChatModel {

    private Llm llm;

    public Llm getLlm() {
        return llm;
    }

    public void setLlm(Llm llm) {
        this.llm = llm;
    }

    @Override
    public String chat(MessageInfo messageInfo, ChatOptions options, LlmNode llmNode, Chain chain) {
        TextPrompt userPrompt = new TextPrompt(messageInfo.getMessage());

        if (StringUtil.hasText(llmNode.getSystemPrompt())) {
            userPrompt.setSystemMessage(SystemMessage.of(llmNode.getSystemPrompt()));
        }

        List<String> images = messageInfo.getImages();
        if (images != null && !images.isEmpty()) {
            ImagePrompt imagePrompt = new ImagePrompt(userPrompt);
            images.forEach(imagePrompt::addImageUrl);
        }

        com.agentsflex.core.llm.ChatOptions chatOptions = new com.agentsflex.core.llm.ChatOptions();
        chatOptions.setSeed(options.getSeed());
        chatOptions.setTemperature(options.getTemperature());
        chatOptions.setTopP(options.getTopP());
        chatOptions.setTopK(options.getTopK());
        chatOptions.setMaxTokens(options.getMaxTokens());
        chatOptions.setStop(options.getStop());

        AiMessageResponse response = llm.chat(userPrompt, chatOptions);
        return response.isError() ? null : response.getResponse();
    }
}
