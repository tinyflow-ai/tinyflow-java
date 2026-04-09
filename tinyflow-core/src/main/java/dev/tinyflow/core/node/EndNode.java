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

import dev.tinyflow.core.chain.*;
import dev.tinyflow.core.util.StringUtil;
import dev.tinyflow.core.util.TextTemplate;

import java.util.HashMap;
import java.util.Map;

public class EndNode extends BaseNode {
    private boolean normal = true;
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isNormal() {
        return normal;
    }

    public void setNormal(boolean normal) {
        this.normal = normal;
    }

    public EndNode() {
        this.name = "end";
    }

    @Override
    public Map<String, Object> execute(Chain chain) {

        Map<String, Object> output = new HashMap<>();
        if (normal) {
            output.put(ChainConsts.CHAIN_STATE_STATUS_KEY, ChainStatus.SUCCEEDED);
        } else {
            output.put(ChainConsts.CHAIN_STATE_STATUS_KEY, ChainStatus.FAILED);
        }

        if (StringUtil.hasText(message)) {
            output.put(ChainConsts.CHAIN_STATE_MESSAGE_KEY, message);
        }

        Map<String, Object> formatParameters = getFormatParameters(chain);


        if (this.outputDefs != null) {
            for (Parameter outputDef : this.outputDefs) {
                Object refObject = chain.getState().resolveValue(outputDef.getRef());
                if (outputDef.getRefType() == RefType.REF) {
                    output.put(outputDef.getName(), refObject);
                } else if (outputDef.getRefType() == RefType.INPUT) {
                    output.put(outputDef.getName(), outputDef.getRef());
                } else if (outputDef.getRefType() == RefType.FIXED) {
                    String value = TextTemplate.of(outputDef.getValue()).formatToString(formatParameters);
                    output.put(outputDef.getName(), StringUtil.getFirstWithText(value, outputDef.getDefaultValue()));
                }
                // default is ref type
                else if (StringUtil.hasText(outputDef.getRef())) {
                    output.put(outputDef.getName(), refObject);
                }
            }
        }

        return output;
    }


    @Override
    public String toString() {
        return "EndNode{" +
                "normal=" + normal +
                ", message='" + message + '\'' +
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
