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


import dev.tinyflow.core.chain.Chain;
import dev.tinyflow.core.chain.ChainState;
import dev.tinyflow.core.chain.Node;
import dev.tinyflow.core.chain.Parameter;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseNode extends Node {

    protected List<Parameter> parameters;
    protected List<Parameter> outputDefs;

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public void addInputParameter(Parameter parameter) {
        if (parameters == null) {
            parameters = new java.util.ArrayList<>();
        }
        parameters.add(parameter);
    }


    public List<Parameter> getOutputDefs() {
        return outputDefs;
    }

    public void setOutputDefs(List<Parameter> outputDefs) {
        this.outputDefs = outputDefs;
    }

    public void addOutputDef(Parameter parameter) {
        if (outputDefs == null) {
            outputDefs = new java.util.ArrayList<>();
        }
        outputDefs.add(parameter);
    }

    public void addOutputDefs(Collection<Parameter> parameters) {
        if (outputDefs == null) {
            outputDefs = new java.util.ArrayList<>();
        }
        outputDefs.addAll(parameters);
    }


    public Map<String, Object> getFormatParameters(Chain chain) {
        Map<String, Object> allParameters = new HashMap<>();
        ChainState state = chain.getState();
        Map<String, Object> parameterValues = state.resolveParameters(this);
        if (parameterValues != null) {
            allParameters.putAll(parameterValues);
        }

        Map<String, Object> envMap = state.getEnvMap();
        if (envMap != null) {
            allParameters.putAll(envMap);
        }

        allParameters.putAll(state.getStartParameters());
        return allParameters;
    }
}
