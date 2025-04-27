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
import com.agentsflex.core.prompt.template.TextPromptTemplate;
import com.agentsflex.core.util.Maps;
import com.agentsflex.core.util.StringUtil;
import dev.tinyflow.core.knowledge.Knowledge;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class KnowledgeNode extends BaseNode {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(KnowledgeNode.class);

    private Object knowledgeId;
    private Knowledge knowledge;
    private String keyword;
    private String limit;

    public Object getKnowledgeId() {
        return knowledgeId;
    }

    public void setKnowledgeId(Object knowledgeId) {
        this.knowledgeId = knowledgeId;
    }

    public Knowledge getKnowledge() {
        return knowledge;
    }

    public void setKnowledge(Knowledge knowledge) {
        this.knowledge = knowledge;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getLimit() {
        return limit;
    }

    public void setLimit(String limit) {
        this.limit = limit;
    }

    @Override
    protected Map<String, Object> execute(Chain chain) {
        Map<String, Object> argsMap = chain.getParameterValues(this);
        String realKeyword = TextPromptTemplate.create(keyword).formatToString(argsMap);
        String realLimitString = TextPromptTemplate.create(limit).formatToString(argsMap);
        int realLimit = 10;
        if (StringUtil.hasText(realLimitString)) {
            try {
                realLimit = Integer.parseInt(realLimitString);
            } catch (Exception e) {
                logger.error(e.toString(), e);
            }
        }

        if (knowledge == null) {
            return Collections.emptyMap();
        }

        List<Document> result = knowledge.search(realKeyword, realLimit, this, chain);
        return Maps.of("documents", result);
    }

    @Override
    public String toString() {
        return "KnowledgeNode{" +
                "knowledgeId=" + knowledgeId +
                ", knowledge=" + knowledge +
                ", keyword='" + keyword + '\'' +
                ", limit='" + limit + '\'' +
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
