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
import com.agentsflex.core.util.StringUtil;
import dev.tinyflow.core.file.FileStorage;
import dev.tinyflow.core.parser.ChainParser;
import dev.tinyflow.core.provider.KnowledgeProvider;
import dev.tinyflow.core.provider.LlmProvider;
import dev.tinyflow.core.provider.SearchEngineProvider;

public class Tinyflow {

    private String data;
    private LlmProvider llmProvider;
    private KnowledgeProvider knowledgeProvider;
    private SearchEngineProvider searchEngineProvider;
    private FileStorage fileStorage;
    private ChainParser chainParser = new ChainParser();

    public Tinyflow() {
    }

    public Tinyflow(String flowData) {
        this.data = flowData;
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

    public KnowledgeProvider getKnowledgeProvider() {
        return knowledgeProvider;
    }

    public void setKnowledgeProvider(KnowledgeProvider knowledgeProvider) {
        this.knowledgeProvider = knowledgeProvider;
    }

    public ChainParser getChainParser() {
        return chainParser;
    }

    public void setChainParser(ChainParser chainParser) {
        this.chainParser = chainParser;
    }

    public SearchEngineProvider getSearchEngineProvider() {
        return searchEngineProvider;
    }

    public void setSearchEngineProvider(SearchEngineProvider searchEngineProvider) {
        this.searchEngineProvider = searchEngineProvider;
    }

    public FileStorage getFileStorage() {
        return fileStorage;
    }

    public void setFileStorage(FileStorage fileStorage) {
        this.fileStorage = fileStorage;
    }

    public Chain toChain() {
        if (StringUtil.noText(data)) {
            throw new IllegalStateException("data is empty");
        }
        return chainParser.parse(this);
    }

}
