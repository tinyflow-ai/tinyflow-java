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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import dev.tinyflow.core.util.MapUtil;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChainState implements Serializable {

    private String instanceId;
    private String chainDefinitionId;
    private ConcurrentHashMap<String, Object> memory = new ConcurrentHashMap<>();

    private Map<String, Object> executeResult;
    private Map<String, Object> environment;

    private ConcurrentHashMap<String, NodeState> nodeStates = new ConcurrentHashMap<>();

    // 算力消耗定义，积分消耗
    private long computeCost;

    private Set<String> suspendNodeIds;
    private List<Parameter> suspendForParameters;
    private ChainStatus status;
    private String message;
    private ExceptionSummary error;

    public ChainState() {
        this.status = ChainStatus.READY;
        this.computeCost = 0;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getChainDefinitionId() {
        return chainDefinitionId;
    }

    public void setChainDefinitionId(String chainDefinitionId) {
        this.chainDefinitionId = chainDefinitionId;
    }

    public ConcurrentHashMap<String, Object> getMemory() {
        return memory;
    }

    public void setMemory(ConcurrentHashMap<String, Object> memory) {
        this.memory = memory;
    }

    public Map<String, Object> getExecuteResult() {
        return executeResult;
    }

    public void setExecuteResult(Map<String, Object> executeResult) {
        this.executeResult = executeResult;
    }

    public Map<String, Object> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map<String, Object> environment) {
        this.environment = environment;
    }

    public ConcurrentHashMap<String, NodeState> getNodeStates() {
        return nodeStates;
    }

    public void setNodeStates(ConcurrentHashMap<String, NodeState> nodeStates) {
        this.nodeStates = nodeStates;
    }

    public void addNodeState(String nodeId, NodeState nodeState) {
        if (nodeStates == null) {
            nodeStates = new ConcurrentHashMap<>();
        }
        nodeStates.put(nodeId, nodeState);
    }

    public Long getComputeCost() {
        return computeCost;
    }

    public void setComputeCost(Long computeCost) {
        this.computeCost = computeCost;
    }

    public void setComputeCost(long computeCost) {
        this.computeCost = computeCost;
    }

    public Set<String> getSuspendNodeIds() {
        return suspendNodeIds;
    }

    public void setSuspendNodeIds(Set<String> suspendNodeIds) {
        this.suspendNodeIds = suspendNodeIds;
    }

    public void addSuspendNodeId(String nodeId) {
        if (suspendNodeIds == null) {
            suspendNodeIds = new HashSet<>();
        }
        suspendNodeIds.add(nodeId);
    }

    public void removeSuspendNodeId(String nodeId) {
        if (suspendNodeIds == null) {
            return;
        }
        suspendNodeIds.remove(nodeId);
    }

    public List<Parameter> getSuspendForParameters() {
        return suspendForParameters;
    }

    public void setSuspendForParameters(List<Parameter> suspendForParameters) {
        this.suspendForParameters = suspendForParameters;
    }

    public ChainStatus getStatus() {
        return status;
    }

    public void setStatus(ChainStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ExceptionSummary getError() {
        return error;
    }

    public void setError(ExceptionSummary error) {
        this.error = error;
    }

    public static ChainState fromJSON(String jsonString) {
        ParserConfig config = new ParserConfig();
        config.putDeserializer(ChainState.class, new ChainDeserializer());
        return JSON.parseObject(jsonString, ChainState.class, config, Feature.SupportAutoType);
    }

    public String toJSON() {
        SerializeConfig config = new SerializeConfig();
        config.put(ChainState.class, new ChainSerializer());
        return JSON.toJSONString(this, config, SerializerFeature.WriteClassName);
    }

    public void reset() {
        this.instanceId = null;
        this.chainDefinitionId = null;
        this.memory.clear();
        this.executeResult = null;
        this.environment = null;
        this.nodeStates = null;
        this.computeCost = 0;
        this.suspendNodeIds = null;
        this.suspendForParameters = null;
        this.status = ChainStatus.READY;
        this.message = null;
        this.error = null;
    }

    public NodeState getOrCreateNodeState(String nodeId) {
        return MapUtil.computeIfAbsent(nodeStates, nodeId, NodeState::new);
    }

    public void addComputeCost(Long value) {
        if (value == null) {
            value = 0L;
        }
        this.computeCost += value;
    }


    public static class ChainSerializer implements ObjectSerializer {
        @Override
        public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
            if (object == null) {
                serializer.writeNull();
                return;
            }
            ChainState chain = (ChainState) object;
            serializer.write(chain.toJSON());
        }
    }

    public static class ChainDeserializer implements ObjectDeserializer {
        @Override
        public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
            String value = parser.parseObject(String.class);
            //noinspection unchecked
            return (T) ChainState.fromJSON(value);
        }
    }


    @Override
    public String toString() {
        return "ChainState{" +
                "instanceId='" + instanceId + '\'' +
                ", chainDefinitionId='" + chainDefinitionId + '\'' +
                ", memory=" + memory +
                ", executeResult=" + executeResult +
                ", environment=" + environment +
                ", nodeStates=" + nodeStates +
                ", computeCost=" + computeCost +
                ", suspendNodeIds=" + suspendNodeIds +
                ", suspendForParameters=" + suspendForParameters +
                ", status=" + status +
                ", message='" + message + '\'' +
                ", error=" + error +
                '}';
    }
}
