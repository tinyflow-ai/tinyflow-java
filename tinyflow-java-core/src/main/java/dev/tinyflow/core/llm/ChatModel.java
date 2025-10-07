package dev.tinyflow.core.llm;

import dev.tinyflow.core.chain.Chain;
import dev.tinyflow.core.node.LlmNode;

import java.io.Serializable;
import java.util.List;

public interface ChatModel {

    String chat(MessageInfo messageInfo, ChatOptions options, LlmNode llmNode, Chain chain);


    class MessageInfo implements Serializable {
        private String message;
        private String systemMessage;
        private List<String> images;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getSystemMessage() {
            return systemMessage;
        }

        public void setSystemMessage(String systemMessage) {
            this.systemMessage = systemMessage;
        }

        public List<String> getImages() {
            return images;
        }

        public void setImages(List<String> images) {
            this.images = images;
        }
    }

    class ChatOptions implements Serializable {

        private String seed;
        private Float temperature = 0.8f;
        private Float topP;
        private Integer topK;
        private Integer maxTokens;
        private List<String> stop;

        public String getSeed() {
            return seed;
        }

        public void setSeed(String seed) {
            this.seed = seed;
        }

        public Float getTemperature() {
            return temperature;
        }

        public void setTemperature(Float temperature) {
            this.temperature = temperature;
        }

        public Float getTopP() {
            return topP;
        }

        public void setTopP(Float topP) {
            this.topP = topP;
        }

        public Integer getTopK() {
            return topK;
        }

        public void setTopK(Integer topK) {
            this.topK = topK;
        }

        public Integer getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
        }

        public List<String> getStop() {
            return stop;
        }

        public void setStop(List<String> stop) {
            this.stop = stop;
        }
    }

}
