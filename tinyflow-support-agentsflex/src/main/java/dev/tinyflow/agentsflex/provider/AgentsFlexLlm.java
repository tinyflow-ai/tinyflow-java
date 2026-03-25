package dev.tinyflow.agentsflex.provider;

import com.agentsflex.core.message.AiMessage;
import com.agentsflex.core.message.SystemMessage;
import com.agentsflex.core.model.chat.ChatModel;
import com.agentsflex.core.model.chat.StreamResponseListener;
import com.agentsflex.core.model.chat.response.AiMessageResponse;
import com.agentsflex.core.model.client.StreamContext;
import com.agentsflex.core.prompt.SimplePrompt;
import com.agentsflex.core.util.Maps;
import dev.tinyflow.core.chain.Chain;
import dev.tinyflow.core.llm.Llm;
import dev.tinyflow.core.node.LlmNode;
import dev.tinyflow.core.util.StringUtil;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

public class AgentsFlexLlm implements Llm {

    public static final String METADATA_KEY_LLM_ID = "llm_id";
    public static final String METADATA_KEY_CHAIN_NODE_ID = "chain_node_id";
    public static final String METADATA_KEY_CHAIN_STATE_ID = "chain_state_id";
    public static final String AGENTS_FLEX_STREAM_LISTENER_NAME = "__agents_flex_stream_listener";

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
        if (StringUtil.hasText(messageInfo.getSystemMessage())) {
            prompt.setSystemMessage(SystemMessage.of(messageInfo.getSystemMessage()));
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

        chatOptions.putMetadata(METADATA_KEY_LLM_ID, llmNode.getLlmId());
        chatOptions.putMetadata(METADATA_KEY_CHAIN_NODE_ID, llmNode.getId());
        chatOptions.putMetadata(METADATA_KEY_CHAIN_STATE_ID, chain.getStateInstanceId());

        String outType = llmNode.getOutType();
        if ("json".equalsIgnoreCase(outType)) {
            String jsonSchema = llmNode.getJsonSchema();
            if (StringUtil.hasText(jsonSchema)) {
                chatOptions.setResponseFormat(Maps.of("type", "json_schema").set("json_schema", jsonSchema));
            } else {
                chatOptions.setResponseFormat(Maps.of("type", "json_object"));
            }
        }


        StreamResponseListener listener = chain.getEventManager().getOtherListener(AGENTS_FLEX_STREAM_LISTENER_NAME);
        // 流式执行
        if (listener != null) {
            CompletableFuture<String> future = new CompletableFuture<>();
            AtomicReference<Throwable> errorRef = new AtomicReference<>();
            chatModel.chatStream(prompt, new StreamResponseListener() {
                @Override
                public void onStart(StreamContext context) {
                    listener.onStart(context);
                }

                @Override
                public void onMessage(StreamContext context, AiMessageResponse response) {
                    listener.onMessage(context, response);
                }

                @Override
                public void onFailure(StreamContext context, Throwable throwable) {
                    try {
                        listener.onFailure(context, throwable);
                    } finally {
                        errorRef.set(throwable);
                    }
                }

                @Override
                public void onStop(StreamContext context) {
                    try {
                        listener.onStop(context);
                    } finally {
                        Throwable error = errorRef.get();
                        if (error != null) {
                            future.completeExceptionally(error);
                        } else {
                            future.complete(context.getFullMessage().getFullContent());
                        }
                    }
                }
            }, chatOptions);

            try {
                return future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
                throw new RuntimeException(cause);
            }
        }
        // 非流式执行
        else {
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
}
