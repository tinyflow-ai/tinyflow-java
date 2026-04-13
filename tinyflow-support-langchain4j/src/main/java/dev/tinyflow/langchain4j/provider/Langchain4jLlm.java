package dev.tinyflow.langchain4j.provider;

import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.tinyflow.core.chain.Chain;
import dev.tinyflow.core.llm.Llm;
import dev.tinyflow.core.node.LlmNode;
import dev.tinyflow.core.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class Langchain4jLlm implements Llm {
    private ChatModel chatModel;

    public ChatModel getChatModel() {
        return chatModel;
    }

    public void setChatModel(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String chat(MessageInfo messageInfo, ChatOptions options, LlmNode llmNode, Chain chain) {
        // 构建消息列表
        List<ChatMessage> chatMessages = new ArrayList<>();
        //系统提示词
        if (StringUtil.hasText(messageInfo.getSystemMessage())) {
            chatMessages.add(SystemMessage.from(messageInfo.getSystemMessage()));
        }
        // 构建用户消息内容（文本+图片）
        List<Content> userContents = new ArrayList<>();
        // 用户消息
        if (StringUtil.hasText(messageInfo.getMessage())) {
            userContents.add(TextContent.from(messageInfo.getMessage()));
        }
        // 添加图片内容
        List<String> images = messageInfo.getImages();
        if (images != null && !images.isEmpty()) {
            for (String imageUrl : images) {
                userContents.add(ImageContent.from(imageUrl));
            }
        }
        // 添加用户消息
        chatMessages.add(UserMessage.from(userContents));

        // 发送请求并获取响应
        ChatResponse response;

        if (options != null) {
            //TODO 如果有配置选项，使用模型特定的配置方式 可进行拦截model
            response = chatModel.chat(chatMessages);
        } else {
            // 无配置选项的直接调用
            response = chatModel.chat(chatMessages);
        }

        if (response == null) {
            throw new RuntimeException("Langchain4jLLm can not get response!");
        }

        AiMessage aiMessage = response.aiMessage();
        if (aiMessage != null && aiMessage.text() != null) {
            return aiMessage.text();
        } else {
            throw new RuntimeException("Langchain4jLLm: AiMessage content is null or empty");
        }
    }
}