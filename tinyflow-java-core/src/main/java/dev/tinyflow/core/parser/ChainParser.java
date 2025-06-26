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
package dev.tinyflow.core.parser;

import com.agentsflex.core.chain.Chain;
import com.agentsflex.core.chain.ChainEdge;
import com.agentsflex.core.chain.ChainNode;
import com.agentsflex.core.chain.JsCodeCondition;
import com.agentsflex.core.util.CollectionUtil;
import com.agentsflex.core.util.StringUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import dev.tinyflow.core.Tinyflow;
import dev.tinyflow.core.parser.impl.*;

import java.util.HashMap;
import java.util.Map;

public class ChainParser {

    private Map<String, NodeParser> nodeParserMap = new HashMap<>();

    public ChainParser() {

        initDefaultParsers();
    }

    private void initDefaultParsers() {
        nodeParserMap.put("startNode", new StartNodeParser());
        nodeParserMap.put("codeNode", new CodeNodeParser());

        nodeParserMap.put("httpNode", new HttpNodeParser());
        nodeParserMap.put("knowledgeNode", new KnowledgeNodeParser());
        nodeParserMap.put("loopNode", new LoopNodeParser());
        nodeParserMap.put("searchEngineNode", new SearchEngineNodeParser());
        nodeParserMap.put("templateNode", new TemplateNodeParser());

        nodeParserMap.put("endNode", new EndNodeParser());
        nodeParserMap.put("llmNode", new LlmNodeParser());
    }

    public Map<String, NodeParser> getNodeParserMap() {
        return nodeParserMap;
    }

    public void setNodeParserMap(Map<String, NodeParser> nodeParserMap) {
        this.nodeParserMap = nodeParserMap;
    }

    public void addNodeParser(String type, NodeParser nodeParser) {
        this.nodeParserMap.put(type, nodeParser);
    }

    public Chain parse(Tinyflow tinyflow) {
        String jsonString = tinyflow.getData();
        if (StringUtil.noText(jsonString)) {
            return null;
        }

        JSONObject root = JSON.parseObject(jsonString);
        JSONArray nodes = root.getJSONArray("nodes");
        JSONArray edges = root.getJSONArray("edges");

        return parse(tinyflow, nodes, edges, null);
    }

    public Chain parse(Tinyflow tinyflow, JSONArray nodes, JSONArray edges, JSONObject parentNode) {
        if (CollectionUtil.noItems(nodes) || CollectionUtil.noItems(edges)) {
            return null;
        }

        Chain chain = new Chain();
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject nodeObject = nodes.getJSONObject(i);
            if ((parentNode == null && StringUtil.noText(nodeObject.getString("parentId")))
                    || (parentNode != null && parentNode.getString("id").equals(nodeObject.getString("parentId")))) {
                ChainNode node = parseNode(tinyflow, nodeObject);
                if (node != null) {

                    node.setId(nodeObject.getString("id"));
                    node.setName(nodeObject.getString("label"));
                    node.setDescription(nodeObject.getString("description"));

                    JSONObject dataJsonObject = nodeObject.getJSONObject("data");
                    if (dataJsonObject != null && !dataJsonObject.isEmpty()) {
                        String conditionString = dataJsonObject.getString("condition");

                        if (StringUtil.hasText(conditionString)) {
                            node.setCondition(new JsCodeCondition(conditionString.trim()));
                        }

                        Boolean async = dataJsonObject.getBoolean("async");
                        if (async != null) {
                            node.setAsync(async);
                        }

                        String name = dataJsonObject.getString("title");
                        if (StringUtil.hasText(name)) {
                            node.setName(name);
                        }

                        String description = dataJsonObject.getString("description");
                        if (StringUtil.hasText(description)) {
                            node.setDescription(description);
                        }

                        Boolean loopEnable = dataJsonObject.getBoolean("loopEnable");
                        if (loopEnable != null) {
                            node.setLoopEnable(loopEnable);
                        }

                        Long loopIntervalMs = dataJsonObject.getLong("loopIntervalMs");
                        if (loopIntervalMs != null) {
                            node.setLoopIntervalMs(loopIntervalMs);
                        }

                        Integer maxLoopCount = dataJsonObject.getInteger("maxLoopCount");
                        if (maxLoopCount != null) {
                            node.setMaxLoopCount(maxLoopCount);
                        }

                        String loopBreakCondition = dataJsonObject.getString("loopBreakCondition");
                        if (StringUtil.hasText(loopBreakCondition)) {
                            node.setLoopBreakCondition(new JsCodeCondition(loopBreakCondition.trim()));
                        }
                    }

                    chain.addNode(node);
                }
            }
        }

        for (int i = 0; i < edges.size(); i++) {
            JSONObject edgeObject = edges.getJSONObject(i);
            JSONObject edgeData = edgeObject.getJSONObject("data");

            if ((parentNode == null && (edgeData == null || StringUtil.noText(edgeData.getString("parentNodeId"))))
                    || (parentNode != null && edgeData != null && edgeData.getString("parentNodeId").equals(parentNode.getString("id"))
                    //不添加子流程里的第一条 edge（也就是父节点连接子节点的第一条线）
                    && !parentNode.getString("id").equals(edgeObject.getString("source")))) {
                ChainEdge edge = parseEdge(edgeObject);
                if (edge != null) {
                    chain.addEdge(edge);
                }
            }
        }

        return chain;
    }

    private ChainNode parseNode(Tinyflow tinyflow, JSONObject nodeObject) {
        String type = nodeObject.getString("type");
        if (StringUtil.noText(type)) {
            return null;
        }

        NodeParser nodeParser = nodeParserMap.get(type);
        return nodeParser == null ? null : nodeParser.parse(nodeObject, tinyflow);
    }


    private ChainEdge parseEdge(JSONObject edgeObject) {
        if (edgeObject == null) return null;
        ChainEdge edge = new ChainEdge();
        edge.setId(edgeObject.getString("id"));
        edge.setSource(edgeObject.getString("source"));
        edge.setTarget(edgeObject.getString("target"));

        JSONObject data = edgeObject.getJSONObject("data");
        if (data == null || data.isEmpty()) {
            return edge;
        }

        String conditionString = data.getString("condition");
        if (StringUtil.hasText(conditionString)) {
            edge.setCondition(new JsCodeCondition(conditionString.trim()));
        }
        return edge;
    }
}
