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
package dev.tinyflow.core.llm;

import java.util.ArrayList;
import java.util.List;

public class LlmManager {

    public List<LlmProvider> providers = new ArrayList<>();

    private static class ManagerHolder {
        private static final LlmManager INSTANCE = new LlmManager();
    }

    private LlmManager() {
    }

    public static LlmManager getInstance() {
        return ManagerHolder.INSTANCE;
    }

    public void registerProvider(LlmProvider provider) {
        providers.add(provider);
    }

    public void removeProvider(LlmProvider provider) {
        providers.remove(provider);
    }

    public Llm getChatModel(Object modelId) {
        for (LlmProvider provider : providers) {
            Llm llm = provider.getChatModel(modelId);
            if (llm != null) {
                return llm;
            }
        }
        return null;
    }
}
