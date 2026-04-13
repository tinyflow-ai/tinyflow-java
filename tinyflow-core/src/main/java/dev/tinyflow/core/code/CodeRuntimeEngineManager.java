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
package dev.tinyflow.core.code;

import dev.tinyflow.core.code.impl.JavascriptRuntimeEngine;

import java.util.ArrayList;
import java.util.List;

public class CodeRuntimeEngineManager {

    public List<CodeRuntimeEngineProvider> providers = new ArrayList<>();

    private static class ManagerHolder {
        private static final CodeRuntimeEngineManager INSTANCE = new CodeRuntimeEngineManager();
    }

    private CodeRuntimeEngineManager() {
        JavascriptRuntimeEngine javascriptRuntimeEngine = new JavascriptRuntimeEngine();
        providers.add(engineId -> {
            if ("js".equals(engineId) || "javascript".equals(engineId)) {
                return javascriptRuntimeEngine;
            }
            return null;
        });
    }

    public static CodeRuntimeEngineManager getInstance() {
        return ManagerHolder.INSTANCE;
    }

    public void registerProvider(CodeRuntimeEngineProvider provider) {
        providers.add(provider);
    }

    public void removeProvider(CodeRuntimeEngineProvider provider) {
        providers.remove(provider);
    }

    public CodeRuntimeEngine getCodeRuntimeEngine(Object engineId) {
        for (CodeRuntimeEngineProvider provider : providers) {
            CodeRuntimeEngine codeRuntimeEngine = provider.getCodeRuntimeEngine(engineId);
            if (codeRuntimeEngine != null) {
                return codeRuntimeEngine;
            }
        }
        return null;
    }
}
