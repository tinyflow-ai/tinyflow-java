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

import dev.tinyflow.core.util.JsConditionUtil;
import dev.tinyflow.core.util.StringUtil;
import org.slf4j.Logger;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Node implements Serializable {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(Node.class);

    protected String id;
    protected String parentId;
    protected String name;
    protected String description;

//    protected List<Edge> inwardEdges;
//    protected List<Edge> outwardEdges;

    protected NodeCondition condition;
    protected NodeValidator validator;

    // 循环执行相关属性
    protected boolean loopEnable = false;           // 是否启用循环执行
    protected long loopIntervalMs = 3000;            // 循环间隔时间（毫秒）
    protected NodeCondition loopBreakCondition;      // 跳出循环的条件
    protected int maxLoopCount = 0;                  // 0 表示不限制循环次数

    protected boolean retryEnable = false;
    protected boolean resetRetryCountAfterNormal = false;
    protected int maxRetryCount = 0;
    protected long retryIntervalMs = 3000;

    // 算力消耗定义，积分消耗
    protected String computeCostExpr;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
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

//    public List<Edge> getInwardEdges() {
//        return inwardEdges;
//    }
//
//    public void setInwardEdges(List<Edge> inwardEdges) {
//        this.inwardEdges = inwardEdges;
//    }
//
//    public List<Edge> getOutwardEdges() {
////        return outwardEdges;
//    }
//
//    public void setOutwardEdges(List<Edge> outwardEdges) {
//        this.outwardEdges = outwardEdges;
//    }

    public NodeCondition getCondition() {
        return condition;
    }

    public void setCondition(NodeCondition condition) {
        this.condition = condition;
    }

    public NodeValidator getValidator() {
        return validator;
    }

    public void setValidator(NodeValidator validator) {
        this.validator = validator;
    }

//    protected void addOutwardEdge(Edge edge) {
//        if (this.outwardEdges == null) {
//            this.outwardEdges = new ArrayList<>();
//        }
//        this.outwardEdges.add(edge);
//    }
//
//    protected void addInwardEdge(Edge edge) {
//        if (this.inwardEdges == null) {
//            this.inwardEdges = new ArrayList<>();
//        }
//        this.inwardEdges.add(edge);
//    }

    public boolean isLoopEnable() {
        return loopEnable;
    }

    public void setLoopEnable(boolean loopEnable) {
        this.loopEnable = loopEnable;
    }

    public long getLoopIntervalMs() {
        return loopIntervalMs;
    }

    public void setLoopIntervalMs(long loopIntervalMs) {
        this.loopIntervalMs = loopIntervalMs;
    }

    public NodeCondition getLoopBreakCondition() {
        return loopBreakCondition;
    }

    public void setLoopBreakCondition(NodeCondition loopBreakCondition) {
        this.loopBreakCondition = loopBreakCondition;
    }

    public int getMaxLoopCount() {
        return maxLoopCount;
    }

    public void setMaxLoopCount(int maxLoopCount) {
        this.maxLoopCount = maxLoopCount;
    }

    public List<Parameter> getParameters() {
        return null;
    }

    public boolean isRetryEnable() {
        return retryEnable;
    }

    public void setRetryEnable(boolean retryEnable) {
        this.retryEnable = retryEnable;
    }

    public boolean isResetRetryCountAfterNormal() {
        return resetRetryCountAfterNormal;
    }

    public void setResetRetryCountAfterNormal(boolean resetRetryCountAfterNormal) {
        this.resetRetryCountAfterNormal = resetRetryCountAfterNormal;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public long getRetryIntervalMs() {
        return retryIntervalMs;
    }

    public void setRetryIntervalMs(long retryIntervalMs) {
        this.retryIntervalMs = retryIntervalMs;
    }

    public String getComputeCostExpr() {
        return computeCostExpr;
    }

    public void setComputeCostExpr(String computeCostExpr) {
        if (computeCostExpr != null) {
            computeCostExpr = computeCostExpr.trim();
        }
        this.computeCostExpr = computeCostExpr;
    }

    public NodeValidResult validate() throws Exception {
        return validator != null ? validator.validate(this) : NodeValidResult.ok();
    }


    public abstract Map<String, Object> execute(Chain chain);

    public long calculateComputeCost(Chain chain, Map<String, Object> executeResult) {

        if (StringUtil.noText(computeCostExpr)) {
            return 0;
        }

        if (computeCostExpr.startsWith("{{") && computeCostExpr.endsWith("}}")) {
            String expr = computeCostExpr.substring(2, computeCostExpr.length() - 2);
            return doCalculateComputeCost(expr, chain, executeResult);
        } else {
            try {
                return Long.parseLong(computeCostExpr);
            } catch (NumberFormatException e) {
                log.error(e.toString(), e);
            }
            return 0;
        }
    }

    protected long doCalculateComputeCost(String expr, Chain chain, Map<String, Object> result) {
//        Map<String, Object> parameterValues = chain.getState().getParameterValuesOnly(this, this.getParameters(), null);
        Map<String, Object> parameterValues = chain.getState().resolveParameters(this, this.getParameters(), null,true);
        Map<String, Object> newMap = new HashMap<>(result);
        newMap.putAll(parameterValues);
        return JsConditionUtil.evalLong(expr, chain, newMap);
    }
}
