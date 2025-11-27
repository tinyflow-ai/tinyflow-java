package dev.tinyflow.agentsflex.provider;

import com.agentsflex.core.message.AiMessage;
import com.agentsflex.core.message.SystemMessage;
import com.agentsflex.core.model.chat.ChatModel;
import com.agentsflex.core.model.chat.response.AiMessageResponse;
import com.agentsflex.core.prompt.SimplePrompt;
import dev.tinyflow.core.chain.Chain;
import dev.tinyflow.core.llm.Llm;
import dev.tinyflow.core.node.LlmNode;
import dev.tinyflow.core.util.StringUtil;

import java.util.List;

public class AgentsFlexLlm implements Llm {

    private ChatModel chatModel;

    public ChatModel getChatModel() {
        return chatModel;
    }

    public void setChatModel(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String chat(MessageInfo messageInfo, ChatOptions options, LlmNode llmNode, Chain chain) {

        SimplePrompt prompt = new SimplePrompt(messageInfo.getMessage());

        // 系统提示词
        if (StringUtil.hasText(llmNode.getSystemPrompt())) {
            prompt.setSystemMessage(SystemMessage.of(llmNode.getSystemPrompt()));
        }

        //  图片
        List<String> images = messageInfo.getImages();
        if (images != null && !images.isEmpty()) {
            for (String image : images) {
                prompt.addImageUrl(image);
            }
        }

        com.agentsflex.core.model.chat.ChatOptions chatOptions = new com.agentsflex.core.model.chat.ChatOptions();
        chatOptions.setSeed(options.getSeed());
        chatOptions.setTemperature(options.getTemperature());
        chatOptions.setTopP(options.getTopP());
        chatOptions.setTopK(options.getTopK());
        chatOptions.setMaxTokens(options.getMaxTokens());
        chatOptions.setStop(options.getStop());

        AiMessageResponse response = chatModel.chat(prompt, chatOptions);
        if (response == null) {
            throw new RuntimeException("AgentsFlexLlm can not get response!");
        }

        if (response.isError()) {
            throw new RuntimeException("AgentsFlexLlm error: " + response.getErrorMessage());
        }

        AiMessage aiMessage = response.getMessage();
        if (aiMessage != null) {
            return aiMessage.getContent();
        }

        throw new RuntimeException("AgentsFlexLlm can not get aiMessage!");
    }
}
