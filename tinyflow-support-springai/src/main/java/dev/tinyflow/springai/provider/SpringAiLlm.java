package dev.tinyflow.springai.provider;

import dev.tinyflow.core.chain.Chain;
import dev.tinyflow.core.llm.Llm;
import dev.tinyflow.core.node.LlmNode;
import dev.tinyflow.core.util.StringUtil;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SpringAiLlm implements Llm {

    private ChatModel chatModel;

    public ChatModel getChatModel() {
        return chatModel;
    }

    public void setChatModel(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String chat(MessageInfo messageInfo, ChatOptions options, LlmNode llmNode, Chain chain) {

        List<Message> messages = new ArrayList<>();

        if (StringUtil.hasText(messageInfo.getSystemMessage())) {
            messages.add(new SystemMessage(messageInfo.getSystemMessage()));
        }

        List<String> images = messageInfo.getImages();
        if (images != null && !images.isEmpty()) {

            StringBuilder contentBuilder = new StringBuilder();
            if (StringUtils.hasText(messageInfo.getMessage())) {
                contentBuilder.append(messageInfo.getMessage()).append("\n");
            }
            contentBuilder.append("包含以下图片：");
            for (String imageUrl : images) {
                contentBuilder.append("\n").append(imageUrl);
            }
            messages.add(new UserMessage(contentBuilder.toString()));
        } else {

            if (StringUtils.hasText(messageInfo.getMessage())) {
                messages.add(new UserMessage(messageInfo.getMessage()));
            }
        }
        // TODO 目前不支持随机种子
        org.springframework.ai.chat.prompt.ChatOptions chatOptions = org.springframework.ai.chat.prompt.ChatOptions.builder()
                .temperature(Optional.ofNullable(options.getTemperature())
                        .map(Float::doubleValue)
                        .orElse(null))
                .topP(Optional.ofNullable(options.getTopP())
                        .map(Float::doubleValue)
                        .orElse(null))
                .topK(options.getTopK())
                .maxTokens(options.getMaxTokens())
                .stopSequences(options.getStop())
                .build();

        Prompt prompt = new Prompt(messages, chatOptions);

        ChatResponse response = chatModel.call(prompt);

        if (response == null) {
            throw new RuntimeException("SpringAiLLm cannot get response!");
        }

        org.springframework.ai.chat.messages.AssistantMessage assistantMessage = response.getResult().getOutput();
        if (assistantMessage != null && assistantMessage.getText() != null) {
            return assistantMessage.getText();
        } else {
            throw new RuntimeException("SpringAiLLm: AssistantMessage content is null or empty");
        }
    }
}