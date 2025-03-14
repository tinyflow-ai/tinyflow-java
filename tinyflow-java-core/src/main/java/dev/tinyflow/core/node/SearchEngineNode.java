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
import com.agentsflex.core.chain.node.BaseNode;
import com.agentsflex.core.document.Document;
import com.agentsflex.core.util.Maps;
import dev.tinyflow.core.searchengine.SearchEngine;

import java.util.List;
import java.util.Map;

public class SearchEngineNode extends BaseNode {

    private SearchEngine searchEngine;
    private int queryCount;

    public SearchEngine getSearchEngine() {
        return searchEngine;
    }

    public void setSearchEngine(SearchEngine searchEngine) {
        this.searchEngine = searchEngine;
    }

    public int getQueryCount() {
        return queryCount;
    }

    public void setQueryCount(int queryCount) {
        this.queryCount = queryCount;
    }

    @Override
    protected Map<String, Object> execute(Chain chain) {
        Map<String, Object> argsMap = getParameters(chain);

        String query = (String) argsMap.get("query");
        List<Document> result = searchEngine.search(query, 10);

        return Maps.of("documents", result);
    }

    @Override
    public String toString() {
        return "SearchEngineNode{" +
                "searchEngine=" + searchEngine +
                ", queryCount=" + queryCount +
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
