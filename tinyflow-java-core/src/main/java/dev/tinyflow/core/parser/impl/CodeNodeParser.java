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
package dev.tinyflow.core.parser.impl;

import com.alibaba.fastjson.JSONObject;
import dev.tinyflow.core.Tinyflow;
import dev.tinyflow.core.node.CodeNode;
import dev.tinyflow.core.parser.BaseNodeParser;

public class CodeNodeParser extends BaseNodeParser<CodeNode> {

    @Override
    public CodeNode doParse(JSONObject root, JSONObject data, Tinyflow tinyflow) {
        String engine = data.getString("engine");
        CodeNode codeNode = new CodeNode();
        codeNode.setEngine(engine);
        codeNode.setCode(data.getString("code"));
        return codeNode;
    }
}
