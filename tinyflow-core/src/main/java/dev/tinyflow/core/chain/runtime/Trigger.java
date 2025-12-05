package dev.tinyflow.core.chain.runtime;

import java.io.Serializable;
import java.util.Map;

public class Trigger implements Serializable {
    private String id;
    private String stateInstanceId;
    private String parentInstanceId;
    private String edgeId;
    private String nodeId; // 可以为 null，代表触发整个 chain
    private TriggerType type;
    private long triggerAt; // epoch ms
    private Map<String, Object> payload;

    public Trigger() {
    }

    // getters / setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStateInstanceId() {
        return stateInstanceId;
    }

    public void setStateInstanceId(String stateInstanceId) {
        this.stateInstanceId = stateInstanceId;
    }

    public String getParentInstanceId() {
        return parentInstanceId;
    }

    public void setParentInstanceId(String parentInstanceId) {
        this.parentInstanceId = parentInstanceId;
    }

    public String getEdgeId() {
        return edgeId;
    }

    public void setEdgeId(String edgeId) {
        this.edgeId = edgeId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public TriggerType getType() {
        return type;
    }

    public void setType(TriggerType type) {
        this.type = type;
    }

    public long getTriggerAt() {
        return triggerAt;
    }

    public void setTriggerAt(long triggerAt) {
        this.triggerAt = triggerAt;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "Trigger{" +
                "id='" + id + '\'' +
                ", stateInstanceId='" + stateInstanceId + '\'' +
                ", parentInstanceId='" + parentInstanceId + '\'' +
                ", edgeId='" + edgeId + '\'' +
                ", nodeId='" + nodeId + '\'' +
                ", type=" + type +
                ", triggerAt=" + triggerAt +
                ", payload=" + payload +
                '}';
    }
}

