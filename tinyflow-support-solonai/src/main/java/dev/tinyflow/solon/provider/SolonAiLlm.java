package dev.tinyflow.solon.provider;

import dev.tinyflow.core.chain.Chain;
import dev.tinyflow.core.llm.Llm;
import dev.tinyflow.core.node.LlmNode;
import dev.tinyflow.core.util.StringUtil;
import org.noear.solon.ai.chat.ChatModel;
import org.noear.solon.ai.chat.ChatResponse;
import org.noear.solon.ai.chat.message.ChatMessage;
import org.noear.solon.ai.chat.prompt.Prompt;
import org.noear.solon.ai.media.Image;
import org.noear.solon.core.util.Assert;

import java.util.List;

/**
 *
 * @author noear 2025/11/27 created
 */
public class SolonAiLlm implements Llm {
    private ChatModel chatModel;

    public ChatModel getChatModel() {
        return chatModel;
    }

    public void setChatModel(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String chat(MessageInfo messageInfo, ChatOptions options, LlmNode llmNode, Chain chain) {
        Prompt prompt = new Prompt();

        // 系统提示词
        if (StringUtil.hasText(messageInfo.getSystemMessage())) {
            prompt.addMessage(ChatMessage.ofSystem(messageInfo.getSystemMessage()));
        }

        // 用户提示词
        if (StringUtil.hasText(messageInfo.getMessage())) {
            prompt.addMessage(ChatMessage.ofUser(messageInfo.getMessage()));
        }

        // 用户图片
        List<String> imageUrls = messageInfo.getImages();
        if (Assert.isNotEmpty(imageUrls)) {
            for (String url : imageUrls) {
                prompt.addMessage(ChatMessage.ofUser(Image.ofUrl(url)));
            }
        }

        ChatResponse response = null;

        try {
            response = chatModel.prompt(prompt)
                    .options(o -> {
                        o.temperature(options.getTemperature());
                        o.top_k(options.getTopK());
                        o.top_p(options.getTopP());
                        o.max_tokens(options.getMaxTokens());
                        o.optionAdd("seed", options.getSeed());
                        o.optionAdd("stop", options.getStop());
                    })
                    .call();

        } catch (Exception ex) {
            throw new RuntimeException("SolonAiLlm error: " + ex.getMessage(), ex);
        }

        if (response == null) {
            throw new RuntimeException("SolonAiLlm can not get response!");
        }

        if (response.getError() != null) {
            throw new RuntimeException("SolonAiLlm error: " + response.getError().getMessage(), response.getError());
        }

        if (response.hasContent()) {
            return response.getContent();
        }

        throw new RuntimeException("SolonAiLlm can not get aiMessage!");
    }
}