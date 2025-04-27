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

import com.agentsflex.core.chain.Chain;
import com.agentsflex.core.chain.Parameter;
import com.agentsflex.core.chain.node.BaseNode;
import com.agentsflex.core.util.Maps;
import com.agentsflex.core.util.StringUtil;
import com.jfinal.template.Engine;
import com.jfinal.template.Template;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

public class TemplateNode extends BaseNode {

    private static final Engine engine;
    private String template;

    static {
        engine = Engine.create("template", e -> {
            e.addSharedStaticMethod(StringUtil.class);
        });
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }


    @Override
    protected Map<String, Object> execute(Chain chain) {
        Map<String, Object> parameters = chain.getParameterValues(this);

        ByteArrayOutputStream result = new ByteArrayOutputStream();

        Template templateByString = engine.getTemplateByString(template);
        templateByString.render(parameters, result);

        String outputDef = "output";
        List<Parameter> outputDefs = getOutputDefs();
        if (outputDefs != null && !outputDefs.isEmpty()) {
            String parameterName = outputDefs.get(0).getName();
            if (StringUtil.hasText(parameterName)) outputDef = parameterName;
        }

        return Maps.of(outputDef, result.toString());
    }


    @Override
    public String toString() {
        return "TemplateNode{" +
                "template='" + template + '\'' +
                ", description='" + description + '\'' +
                ", parameters=" + parameters +
                ", outputDefs=" + outputDefs +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", async=" + async +
                ", inwardEdges=" + inwardEdges +
                ", outwardEdges=" + outwardEdges +
                ", condition=" + condition +
                ", memory=" + memory +
                ", nodeStatus=" + nodeStatus +
                '}';
    }
}
