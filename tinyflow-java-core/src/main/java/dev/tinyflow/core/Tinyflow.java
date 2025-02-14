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
package dev.tinyflow.core;

import com.agentsflex.core.chain.Chain;
import dev.tinyflow.core.parser.ChainParser;
import dev.tinyflow.core.provder.LlmProvider;

import java.util.Map;

public class Tinyflow {

    private String data;
    private Chain chain;
    private LlmProvider llmProvider;


    public Tinyflow(String flowData) {
        this.data = flowData;
        this.chain = ChainParser.parse(this);
    }

    public Chain getChain() {
        return chain;
    }

    public void setChain(Chain chain) {
        this.chain = chain;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public LlmProvider getLlmProvider() {
        return llmProvider;
    }

    public void setLlmProvider(LlmProvider llmProvider) {
        this.llmProvider = llmProvider;
    }

    public void execute(Map<String, Object> variables) {
        chain.execute(variables);
    }

    public Map<String, Object> executeForResult(Map<String, Object> variables) {
        return chain.executeForResult(variables);
    }
}
