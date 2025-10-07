/*
 *  Copyright (c) 2023-2025, Agents-Flex (fuhai999@gmail.com).
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package dev.tinyflow.core.node;

import com.alibaba.fastjson.JSON;
import dev.tinyflow.core.chain.Chain;
import dev.tinyflow.core.chain.Parameter;
import dev.tinyflow.core.llm.ChatModel;
import dev.tinyflow.core.llm.ChatModelManager;
import dev.tinyflow.core.util.*;

import java.io.File;
import java.util.*;

public class LlmNode extends BaseNode {

    protected String llmId;
    protected ChatModel.ChatOptions chatOptions;
    protected String userPrompt;

    protected String systemPrompt;
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

    //    public LlmNode(Llm llm, String userPrompt) {
//        this.llm = llm;
//        this.userPrompt = userPrompt;
//        this.userPromptTemplate = StringUtil.hasText(userPrompt)
//                ? TextPromptTemplate.of(userPrompt) : null;
//    }
//
//
//    public Llm getLlm() {
//        return llm;
//    }
//
//    public void setLlm(Llm llm) {
//        this.llm = llm;
//    }

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

    public ChatModel.ChatOptions getChatOptions() {
        return chatOptions;
    }

    public void setChatOptions(ChatModel.ChatOptions chatOptions) {
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
    protected Map<String, Object> execute(Chain chain) {
        Map<String, Object> parameterValues = chain.getParameterValues(this);

        if (StringUtil.noText(userPrompt)) {
            return Collections.emptyMap();
        }

        String userPromptString = TextTemplate.of(userPrompt).formatToString(parameterValues);


        ChatModel chatModel = ChatModelManager.getInstance().getChatModel(this.llmId);
        if (chatModel == null) {
            chain.stopError("Can not find llm: " + this.llmId);
            return Collections.emptyMap();
        }

        String systemPromptString = TextTemplate.of(this.systemPrompt).formatToString(parameterValues);


        ChatModel.MessageInfo messageInfo = new ChatModel.MessageInfo();
        messageInfo.setMessage(userPromptString);
        messageInfo.setSystemMessage(systemPromptString);

        if (images != null && !images.isEmpty()) {
            Map<String, Object> filesMap = chain.getParameterValues(this, images);
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


        String responseContent = chatModel.chat(messageInfo, chatOptions, this, chain);

        if (StringUtil.noText(responseContent)) {
            return Collections.emptyMap();
        } else {
            responseContent = responseContent.trim();
        }


        if ("json".equalsIgnoreCase(outType)) {
            Object jsonObjectOrArray;
            try {
                jsonObjectOrArray = JSON.parse(unWrapMarkdown(responseContent));
            } catch (Exception e) {
                chain.stopError("Can not parse json: " + responseContent + " " + e.getMessage());
                return Collections.emptyMap();
            }


            if (CollectionUtil.noItems(this.outputDefs)) {
                return Maps.of("root", jsonObjectOrArray);
            } else {
                Parameter parameter = this.outputDefs.get(0);
                return Maps.of(parameter.getName(), jsonObjectOrArray);
            }
//            if (this.outputDefs != null) {
//                JSONObject jsonObject;
//                try {
//                    jsonObject = JSON.parseObject(unWrapMarkdown(responseContent));
//                } catch (Exception e) {
//                    chain.stopError("Can not parse json: " + response.getResponse() + " " + e.getMessage());
//                    return Collections.emptyMap();
//                }
//                return getExecuteResultMap(outputDefs, jsonObject);
//            }
//            return Collections.emptyMap();
        }

//        if (outType == null || outType.equalsIgnoreCase("text") || outType.equalsIgnoreCase("markdown")) {
        else {
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

//    public static Map<String, Object> getExecuteResultMap(List<Parameter> outputDefs, JSONObject data) {
//        Map<String, Object> result = new HashMap<>();
//        outputDefs.forEach(output -> {
//            result.put(output.getName(), getOutputDefData(output, data, false));
//        });
//        return result;
//    }

//    private static Object getOutputDefData(Parameter output, JSONObject data, boolean sub) {
//        String name = output.getName();
//        DataType dataType = output.getDataType();
//        switch (dataType) {
//            case Array:
//            case Array_Object:
//                if (output.getChildren() == null || output.getChildren().isEmpty()) {
//                    return data.get(name);
//                }
//                List<Object> subResultList = new ArrayList<>();
//                Object dataObj = data.get(name);
//                if (dataObj instanceof JSONArray) {
//                    JSONArray contentFields = ((JSONArray) dataObj);
//                    if (!contentFields.isEmpty()) {
//                        contentFields.forEach(field -> {
//                            if (field instanceof JSONObject) {
//                                subResultList.add(getChildrenResult(output.getChildren(), (JSONObject) field, sub));
//                            }
//                        });
//                    }
//                }
//                return subResultList;
//            case Object:
//                return (output.getChildren() != null && !output.getChildren().isEmpty()) ? getChildrenResult(output.getChildren(), sub ? data : (JSONObject) data.get(name), sub) : data.get(name);
//            case String:
//            case Number:
//            case Boolean:
//                Object obj = data.get(name);
//                return (DataType.String == dataType) ? (obj instanceof String ? obj : "") : (DataType.Number == dataType) ? (obj instanceof Number ? obj : 0) : obj instanceof Boolean ? obj : false;
//            case Array_String:
//            case Array_Number:
//            case Array_Boolean:
//                Object arrayObj = data.get(name);
//                if (arrayObj instanceof JSONArray) {
//                    ((JSONArray) arrayObj).removeIf(o -> arrayRemoveFlag(dataType, o));
//                    return arrayObj;
//                }
//                return Collections.emptyList();
//            default:
//                return ""; // FILE和其他不支持的类型，默认空字符串
//        }
//    }

//    private static boolean arrayRemoveFlag(DataType dataType, Object arrayObj) {
//        boolean removeFlag = false;
//        if (DataType.Array_String == dataType) {
//            if (!(arrayObj instanceof String)) {
//                removeFlag = true;
//            }
//        } else if (DataType.Array_Number == dataType) {
//            if (!(arrayObj instanceof Number)) {
//                removeFlag = true;
//            }
//        } else {
//            if (!(arrayObj instanceof Boolean)) {
//                removeFlag = true;
//            }
//        }
//        return removeFlag;
//    }

//    private static Map<String, Object> getChildrenResult(List<Parameter> children, JSONObject data, boolean sub) {
//        Map<String, Object> childrenResult = new HashMap<>();
//        children.forEach(child -> {
//            String childName = child.getName();
//            Object subData = getOutputDefData(child, data, sub);
//            if ((subData instanceof JSONObject) && (child.getChildren() != null && !child.getChildren().isEmpty())) {
//                getChildrenResult(child.getChildren(), (JSONObject) subData, true);
//            } else {
//                childrenResult.put(childName, subData);
//            }
//        });
//        return childrenResult;
//    }


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
                ", async=" + async +
                ", inwardEdges=" + inwardEdges +
                ", outwardEdges=" + outwardEdges +
                ", condition=" + condition +
                ", memory=" + memory +
                ", nodeStatus=" + nodeStatus +
                ", validator=" + validator +
                ", loopEnable=" + loopEnable +
                ", loopIntervalMs=" + loopIntervalMs +
                ", loopBreakCondition=" + loopBreakCondition +
                ", maxLoopCount=" + maxLoopCount +
                ", computeCost=" + computeCost +
                '}';
    }
}
