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
package dev.tinyflow.core.node;

import com.alibaba.fastjson.JSON;
import dev.tinyflow.core.chain.Chain;
import dev.tinyflow.core.chain.Parameter;
import dev.tinyflow.core.llm.Llm;
import dev.tinyflow.core.llm.LlmManager;
import dev.tinyflow.core.util.*;

import java.io.File;
import java.util.*;

public class LlmNode extends BaseNode {

    protected String llmId;
    protected Llm.ChatOptions chatOptions;
    protected String userPrompt;
    protected String systemPrompt;
    protected String jsonSchema;
    protected String outType = "text"; //text markdown json
    protected List<Parameter> images;

    public LlmNode() {
    }

    public String getLlmId() {
        return llmId;
    }

    public void setLlmId(String llmId) {
        this.llmId = llmId;
    }

    public String getUserPrompt() {
        return userPrompt;
    }

    public void setUserPrompt(String userPrompt) {
        this.userPrompt = userPrompt;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getJsonSchema() {
        return jsonSchema;
    }

    public void setJsonSchema(String jsonSchema) {
        this.jsonSchema = jsonSchema;
    }

    public Llm.ChatOptions getChatOptions() {
        return chatOptions;
    }

    public void setChatOptions(Llm.ChatOptions chatOptions) {
        this.chatOptions = chatOptions;
    }

    public String getOutType() {
        return outType;
    }

    public void setOutType(String outType) {
        this.outType = outType;
    }

    public List<Parameter> getImages() {
        return images;
    }

    public void setImages(List<Parameter> images) {
        this.images = images;
    }

    @Override
    public Map<String, Object> execute(Chain chain) {
        Map<String, Object> formatParameters = getFormatParameters(chain);

        if (StringUtil.noText(userPrompt)) {
            throw new RuntimeException("Can not find user prompt");
        }

        String userPromptString = TextTemplate.of(userPrompt).formatToString(formatParameters);


        Llm llm = LlmManager.getInstance().getChatModel(this.llmId);
        if (llm == null) {
            throw new RuntimeException("Can not find llm: " + this.llmId);
        }

        String systemPromptString = TextTemplate.of(this.systemPrompt).formatToString(formatParameters);

        Llm.MessageInfo messageInfo = new Llm.MessageInfo();
        messageInfo.setMessage(userPromptString);
        messageInfo.setSystemMessage(systemPromptString);

        if (images != null && !images.isEmpty()) {
            Map<String, Object> filesMap = chain.getState().resolveParameters(this, images);
            List<String> imagesUrls = new ArrayList<>();
            filesMap.forEach((s, o) -> {
                if (o instanceof String) {
                    imagesUrls.add((String) o);
                } else if (o instanceof File) {
                    byte[] bytes = IOUtil.readBytes((File) o);
                    String base64 = Base64.getEncoder().encodeToString(bytes);
                    imagesUrls.add(base64);
                }
            });
            messageInfo.setImages(imagesUrls);
        }


        String responseContent = llm.chat(messageInfo, chatOptions, this, chain);

        if (StringUtil.noText(responseContent)) {
            throw new RuntimeException("Can not get response from llm");
        } else {
            responseContent = responseContent.trim();
        }


        if ("json".equalsIgnoreCase(outType)) {
            Object jsonObjectOrArray;
            try {
                jsonObjectOrArray = JSON.parse(unWrapMarkdown(responseContent));
            } catch (Exception e) {
                throw new RuntimeException("Can not parse json: " + responseContent + " " + e.getMessage());
            }

            if (CollectionUtil.noItems(this.outputDefs)) {
                return Maps.of("root", jsonObjectOrArray);
            } else {
                Parameter parameter = this.outputDefs.get(0);
                return Maps.of(parameter.getName(), jsonObjectOrArray);
            }
        } else {
            if (CollectionUtil.noItems(this.outputDefs)) {
                return Maps.of("output", responseContent);
            } else {
                Parameter parameter = this.outputDefs.get(0);
                return Maps.of(parameter.getName(), responseContent);
            }
        }
    }


    /**
     * 移除 ``` 或者 ```json 等
     *
     * @param markdown json内容
     * @return 方法 json 内容
     */
    public static String unWrapMarkdown(String markdown) {
        // 移除开头的 ```json 或 ```
        if (markdown.startsWith("```")) {
            int newlineIndex = markdown.indexOf('\n');
            if (newlineIndex != -1) {
                markdown = markdown.substring(newlineIndex + 1);
            } else {
                // 如果没有换行符，直接去掉 ``` 部分
                markdown = markdown.substring(3);
            }
        }

        // 移除结尾的 ```
        if (markdown.endsWith("```")) {
            markdown = markdown.substring(0, markdown.length() - 3);
        }
        return markdown.trim();
    }


    @Override
    public String toString() {
        return "LlmNode{" +
                "llmId='" + llmId + '\'' +
                ", chatOptions=" + chatOptions +
                ", userPrompt='" + userPrompt + '\'' +
                ", systemPrompt='" + systemPrompt + '\'' +
                ", outType='" + outType + '\'' +
                ", images=" + images +
                ", parameters=" + parameters +
                ", outputDefs=" + outputDefs +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", condition=" + condition +
                ", validator=" + validator +
                ", loopEnable=" + loopEnable +
                ", loopIntervalMs=" + loopIntervalMs +
                ", loopBreakCondition=" + loopBreakCondition +
                ", maxLoopCount=" + maxLoopCount +
                ", retryEnable=" + retryEnable +
                ", resetRetryCountAfterNormal=" + resetRetryCountAfterNormal +
                ", maxRetryCount=" + maxRetryCount +
                ", retryIntervalMs=" + retryIntervalMs +
                ", computeCostExpr='" + computeCostExpr + '\'' +
                '}';
    }
}
