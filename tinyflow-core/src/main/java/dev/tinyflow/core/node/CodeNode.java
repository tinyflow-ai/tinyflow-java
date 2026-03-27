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
import dev.tinyflow.core.code.CodeRuntimeEngine;
import dev.tinyflow.core.code.CodeRuntimeEngineManager;
import dev.tinyflow.core.util.StringUtil;
import dev.tinyflow.core.util.TextTemplate;

import java.util.Map;

public class CodeNode extends BaseNode {
    protected String engine;
    protected String code;

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public Map<String, Object> execute(Chain chain) {
        if (StringUtil.noText(code)) {
            throw new IllegalArgumentException("code is empty");
        }

        Map<String, Object> formatParameters = getFormatParameters(chain);
        String newCode = TextTemplate.of(code).formatToString(formatParameters);

        CodeRuntimeEngine codeRuntimeEngine = CodeRuntimeEngineManager.getInstance().getCodeRuntimeEngine(this.engine);
        return codeRuntimeEngine.execute(newCode, this, chain);
    }

}
