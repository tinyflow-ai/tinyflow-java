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
import dev.tinyflow.core.util.StringUtil;
import dev.tinyflow.core.util.TextTemplate;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ChainState implements Serializable {

    private String instanceId;
    private String parentInstanceId;
    private String chainDefinitionId;
    private ConcurrentHashMap<String, Object> memory = new ConcurrentHashMap<>();

    private Map<String, Object> executeResult;
    private Map<String, Object> environment;

    private List<String> triggerEdgeIds;
    private List<String> triggerNodeIds;

    private List<String> uncheckedEdgeIds;
    private List<String> uncheckedNodeIds;

    // 算力消耗定义，积分消耗
    private long computeCost;
    private Set<String> suspendNodeIds;
    private List<Parameter> suspendForParameters;
    private ChainStatus status;
    private String message;
    private ExceptionSummary error;
    private long version;

    public ChainState() {
        this.instanceId = UUID.randomUUID().toString();
        this.status = ChainStatus.READY;
        this.computeCost = 0;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getParentInstanceId() {
        return parentInstanceId;
    }

    public void setParentInstanceId(String parentInstanceId) {
        this.parentInstanceId = parentInstanceId;
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


    public List<String> getTriggerEdgeIds() {
        return triggerEdgeIds;
    }

    public void setTriggerEdgeIds(List<String> triggerEdgeIds) {
        this.triggerEdgeIds = triggerEdgeIds;
    }

    public void addTriggerEdgeId(String edgeId) {
        if (triggerEdgeIds == null) {
            triggerEdgeIds = new ArrayList<>();
        }
        triggerEdgeIds.add(edgeId);
    }

    public List<String> getTriggerNodeIds() {
        return triggerNodeIds;
    }

    public void setTriggerNodeIds(List<String> triggerNodeIds) {
        this.triggerNodeIds = triggerNodeIds;
    }

    public void addTriggerNodeId(String nodeId) {
        if (triggerNodeIds == null) {
            triggerNodeIds = new ArrayList<>();
        }
        triggerNodeIds.add(nodeId);
    }

    public List<String> getUncheckedEdgeIds() {
        return uncheckedEdgeIds;
    }

    public void setUncheckedEdgeIds(List<String> uncheckedEdgeIds) {
        this.uncheckedEdgeIds = uncheckedEdgeIds;
    }

    public void addUncheckedEdgeId(String edgeId) {
        if (uncheckedEdgeIds == null) {
            uncheckedEdgeIds = new ArrayList<>();
        }
        uncheckedEdgeIds.add(edgeId);
    }

    public boolean removeUncheckedEdgeId(String edgeId) {
        if (uncheckedEdgeIds == null) {
            return false;
        }
        return uncheckedEdgeIds.remove(edgeId);
    }

    public List<String> getUncheckedNodeIds() {
        return uncheckedNodeIds;
    }

    public void setUncheckedNodeIds(List<String> uncheckedNodeIds) {
        this.uncheckedNodeIds = uncheckedNodeIds;
    }

    public void addUncheckedNodeId(String nodeId) {
        if (uncheckedNodeIds == null) {
            uncheckedNodeIds = new ArrayList<>();
        }
        uncheckedNodeIds.add(nodeId);
    }

    public boolean removeUncheckedNodeId(String nodeId) {
        if (uncheckedNodeIds == null) {
            return false;
        }
        return uncheckedNodeIds.remove(nodeId);
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

    public void addSuspendForParameter(Parameter parameter) {
        if (suspendForParameters == null) {
            suspendForParameters = new ArrayList<>();
        }
        suspendForParameters.add(parameter);
    }

    public void addSuspendForParameters(List<Parameter> parameters) {
        if (parameters == null) {
            return;
        }
        if (suspendForParameters == null) {
            suspendForParameters = new ArrayList<>();
        }
        suspendForParameters.addAll(parameters);
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


    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
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
        this.computeCost = 0;
        this.suspendNodeIds = null;
        this.suspendForParameters = null;
        this.status = ChainStatus.READY;
        this.message = null;
        this.error = null;
    }


    public void addComputeCost(Long value) {
        if (value == null) {
            value = 0L;
        }
        this.computeCost += value;
    }


    public Map<String, Object> getNodeExecuteResult(String nodeId) {
        if (memory == null || memory.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> result = new HashMap<>();
        memory.forEach((k, v) -> {
            if (k.startsWith(nodeId + ".")) {
                String newKey = k.substring(nodeId.length() + 1);
                result.put(newKey, v);
            }
        });
        return result;
    }

//    public Map<String, Object> getTriggerVariables() {
//        Trigger trigger = TriggerContext.getCurrentTrigger();
//        if (trigger != null) {
//            return trigger.getVariables();
//        }
//        return Collections.emptyMap();
//    }


    public Object resolveValue(String path) {
        Object result = MapUtil.getByPath(getMemory(), path);
        if (result == null) result = MapUtil.getByPath(getEnvironment(), path);
//        if (result == null) result = MapUtil.getByPath(getTriggerVariables(), path);
        return result;
    }

    public Map<String, Object> resolveParameters(Node node) {
        return resolveParameters(node, node.getParameters());
    }

    public Map<String, Object> resolveParameters(Node node, List<? extends Parameter> parameters) {
        return resolveParameters(node, parameters, null);
    }

    public Map<String, Object> resolveParameters(Node node, List<? extends Parameter> parameters, Map<String, Object> formatArgs) {
        return resolveParameters(node, parameters, formatArgs, false);
    }

    private boolean isNullOrBlank(Object value) {
        return value == null || value instanceof String && StringUtil.noText((String) value);
    }


    public Map<String, Object> getEnvMap() {
        Map<String, Object> formatArgsMap = new HashMap<>();
        formatArgsMap.put("env", getEnvironment());
        formatArgsMap.put("env.sys", System.getenv());
        return formatArgsMap;
    }

    public Map<String, Object> getStartParameters() {
        Map<String, Object> startParameters = new LinkedHashMap<>();
        ConcurrentHashMap<String, Object> memory = getMemory();
        if (memory != null) {
            memory.forEach((s, o) -> {
                if (!s.contains(".")) {
                    startParameters.put(s, o);
                }
            });
        }
        return startParameters;
    }


    public Map<String, Object> resolveParameters(Node node, List<? extends Parameter> parameters, Map<String, Object> formatArgs, boolean ignoreRequired) {
        if (parameters == null || parameters.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> variables = new LinkedHashMap<>();
        List<Parameter> suspendParameters = null;
        for (Parameter parameter : parameters) {
            RefType refType = parameter.getRefType();
            Object value = null;
            if (refType == RefType.FIXED) {
                value = TextTemplate.of(parameter.getValue())
                        .formatToString(Arrays.asList(formatArgs, getEnvMap(), getStartParameters()));
            } else if (refType == RefType.REF) {
                value = this.resolveValue(parameter.getRef());
            }
            // 单节点执行时，参数只会传入 name 内容。
            if (value == null) {
                value = this.resolveValue(parameter.getName());
            }

            if (value == null && parameter.getDefaultValue() != null) {
                value = parameter.getDefaultValue();
            }

            if (refType == RefType.INPUT && isNullOrBlank(value)) {
                if (!ignoreRequired && parameter.isRequired()) {
                    if (suspendParameters == null) {
                        suspendParameters = new ArrayList<>();
                    }
                    suspendParameters.add(parameter);
                    continue;
                }
            }

            if (parameter.isRequired() && isNullOrBlank(value)) {
                if (!ignoreRequired) {
                    throw new ChainException(node.getName() + " Missing required parameter:" + parameter.getName());
                }
            }

            if (value instanceof String) {
                value = ((String) value).trim();
                if (parameter.getDataType() == DataType.Boolean) {
                    value = "true".equalsIgnoreCase((String) value) || "1".equalsIgnoreCase((String) value);
                } else if (parameter.getDataType() == DataType.Number) {
                    value = Long.parseLong((String) value);
                } else if (parameter.getDataType() == DataType.Array) {
                    value = JSON.parseArray((String) value);
                }
            }

            variables.put(parameter.getName(), value);
        }

        if (suspendParameters != null && !suspendParameters.isEmpty()) {
            // 构建参数名称列表
            String missingParams = suspendParameters.stream()
                    .map(Parameter::getName)
                    .collect(Collectors.joining("', '", "'", "'"));

            String errorMessage = String.format(
                    "Node '%s' (type: %s) is suspended. Waiting for input parameters: %s.",
                    StringUtil.getFirstWithText(node.getName(), node.getId()),
                    node.getClass().getSimpleName(),
                    missingParams
            );

            throw new ChainSuspendException(errorMessage, this.instanceId, suspendParameters);
        }

        return variables;
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
                ", computeCost=" + computeCost +
                ", suspendNodeIds=" + suspendNodeIds +
                ", suspendForParameters=" + suspendForParameters +
                ", status=" + status +
                ", message='" + message + '\'' +
                ", error=" + error +
                ", version=" + version +
                '}';
    }
}
