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
package dev.tinyflow.core.chain;

import dev.tinyflow.core.util.StringUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;


public class ChainDefinition implements Serializable {
    protected String id;
    protected String name;
    protected String description;
    protected List<Node> nodes;
    protected List<Edge> edges;

    public ChainDefinition() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public void setEdges(List<Edge> edges) {
        this.edges = edges;
    }

    public List<Edge> getOutwardEdge(String nodeId) {
        List<Edge> result = new ArrayList<>();
        for (Edge edge : edges) {
            if (nodeId.equals(edge.getSource())) {
                result.add(edge);
            }
        }
        return result;
    }


    public List<Edge> getInwardEdge(String nodeId) {
        List<Edge> result = new ArrayList<>();
        for (Edge edge : edges) {
            if (nodeId.equals(edge.getTarget())) {
                result.add(edge);
            }
        }
        return result;
    }

    public void addNode(Node node) {
        if (nodes == null) {
            this.nodes = new ArrayList<>();
        }

        if (StringUtil.noText(node.getId())) {
            node.setId(UUID.randomUUID().toString());
        }

        nodes.add(node);

//        if (this.edges != null) {
//            for (Edge edge : edges) {
//                if (node.getId().equals(edge.getSource())) {
//                    node.addOutwardEdge(edge);
//                } else if (node.getId().equals(edge.getTarget())) {
//                    node.addInwardEdge(edge);
//                }
//            }
//        }
    }


    public Node getNodeById(String id) {
        if (id == null || StringUtil.noText(id)) {
            return null;
        }

        for (Node node : this.nodes) {
            if (id.equals(node.getId())) {
                return node;
            }
        }

        return null;
    }


    public void addEdge(Edge edge) {
        if (this.edges == null) {
            this.edges = new ArrayList<>();
        }
        this.edges.add(edge);

//        boolean findSource = false, findTarget = false;
//        for (Node node : this.nodes) {
//            if (node.getId().equals(edge.getSource())) {
//                node.addOutwardEdge(edge);
//                findSource = true;
//            } else if (node.getId().equals(edge.getTarget())) {
//                node.addInwardEdge(edge);
//                findTarget = true;
//            }
//            if (findSource && findTarget) {
//                break;
//            }
//        }
    }


    public Edge getEdgeById(String edgeId) {
        for (Edge edge : this.edges) {
            if (edgeId.equals(edge.getId())) {
                return edge;
            }
        }
        return null;
    }

    public List<Node> getStartNodes() {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }

        List<Node> result = new ArrayList<>();

        for (Node node : nodes) {
//            if (CollectionUtil.noItems(node.getInwardEdges())) {
//                result.add(node);
//            }
            List<Edge> inwardEdge = getInwardEdge(node.getId());
            if (inwardEdge == null || inwardEdge.isEmpty()) {
                result.add(node);
            }
        }
        return result;
    }


    public List<Parameter> getStartParameters() {
        List<Node> startNodes = this.getStartNodes();
        if (startNodes == null || startNodes.isEmpty()) {
            return Collections.emptyList();
        }

        List<Parameter> parameters = new ArrayList<>();
        for (Node node : startNodes) {
            List<Parameter> nodeParameters = node.getParameters();
            if (nodeParameters != null) parameters.addAll(nodeParameters);
        }
        return parameters;
    }


    @Override
    public String toString() {
        return "ChainDefinition{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", nodes=" + nodes +
                ", edges=" + edges +
                '}';
    }
}
