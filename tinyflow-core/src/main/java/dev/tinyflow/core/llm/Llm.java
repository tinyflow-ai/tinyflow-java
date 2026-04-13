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
package dev.tinyflow.core.llm;

import dev.tinyflow.core.chain.Chain;
import dev.tinyflow.core.node.LlmNode;

import java.io.Serializable;
import java.util.List;

/**
 * Llm接口定义了大语言模型的基本功能规范
 * <p>
 * 该接口提供了与大型语言模型交互的标准方法，
 * 包括文本生成、对话处理等核心功能
 */
public interface Llm {


    /**
     * 执行聊天对话操作
     *
     * @param messageInfo 消息信息对象，包含用户输入的原始消息内容及相关元数据
     * @param options     聊天配置选项，用于控制对话行为和模型参数
     * @param llmNode     大语言模型节点，指定使用的具体语言模型实例
     * @param chain       对话链对象，管理对话历史和上下文状态
     * @return 返回模型生成的回复字符串
     */
    String chat(MessageInfo messageInfo, ChatOptions options, LlmNode llmNode, Chain chain);


    /**
     * 消息信息类，用于封装消息相关的信息
     */
    class MessageInfo implements Serializable {
        private static final long serialVersionUID = 1L;
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

    /**
     * ChatOptions 类用于存储聊天相关的配置选项
     * 该类实现了Serializable接口，支持序列化操作
     */
    /**
     * ChatOptions类用于配置聊天模型的参数选项
     * 实现了Serializable接口，支持序列化
     */
    class ChatOptions implements Serializable {

        private String seed;
        private Float temperature = 0.8f;
        private Float topP;
        private Integer topK;
        private Integer maxTokens;
        private List<String> stop;

        /**
         * 获取随机种子值
         *
         * @return 返回随机种子字符串，用于控制生成结果的随机性
         */
        public String getSeed() {
            return seed;
        }

        /**
         * 设置随机种子值
         *
         * @param seed 随机种子字符串，用于控制生成结果的随机性
         */
        public void setSeed(String seed) {
            this.seed = seed;
        }

        /**
         * 获取温度参数
         *
         * @return 返回温度值，控制生成文本的随机性，值越高越随机
         */
        public Float getTemperature() {
            return temperature;
        }

        /**
         * 设置温度参数
         *
         * @param temperature 温度值，控制生成文本的随机性，值越高越随机
         */
        public void setTemperature(Float temperature) {
            this.temperature = temperature;
        }

        /**
         * 获取Top-P参数
         *
         * @return 返回Top-P值，用于 nucleus sampling，控制生成词汇的概率阈值
         */
        public Float getTopP() {
            return topP;
        }

        /**
         * 设置Top-P参数
         *
         * @param topP Top-P值，用于 nucleus sampling，控制生成词汇的概率阈值
         */
        public void setTopP(Float topP) {
            this.topP = topP;
        }

        /**
         * 获取Top-K参数
         *
         * @return 返回Top-K值，限制每步选择词汇的范围
         */
        public Integer getTopK() {
            return topK;
        }

        /**
         * 设置Top-K参数
         *
         * @param topK Top-K值，限制每步选择词汇的范围
         */
        public void setTopK(Integer topK) {
            this.topK = topK;
        }

        /**
         * 获取最大令牌数
         *
         * @return 返回最大令牌数，限制生成文本的最大长度
         */
        public Integer getMaxTokens() {
            return maxTokens;
        }

        /**
         * 设置最大令牌数
         *
         * @param maxTokens 最大令牌数，限制生成文本的最大长度
         */
        public void setMaxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
        }

        /**
         * 获取停止词列表
         *
         * @return 返回停止词字符串列表，当生成文本遇到这些词时会停止生成
         */
        public List<String> getStop() {
            return stop;
        }

        /**
         * 设置停止词列表
         *
         * @param stop 停止词字符串列表，当生成文本遇到这些词时会停止生成
         */
        public void setStop(List<String> stop) {
            this.stop = stop;
        }
    }


}
