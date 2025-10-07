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

import dev.tinyflow.core.chain.Chain;
import dev.tinyflow.core.parser.ChainParser;
import dev.tinyflow.core.util.StringUtil;

public class Tinyflow {

    private String data;
    private ChainParser chainParser;

    public Tinyflow() {
        this.chainParser = new ChainParser();
        this.chainParser.addAllParsers(TinyflowConfig.getDefaultNodeParsers());
    }

    public Tinyflow(String flowData) {
        this();
        this.data = flowData;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public ChainParser getChainParser() {
        return chainParser;
    }

    public void setChainParser(ChainParser chainParser) {
        this.chainParser = chainParser;
    }

    public Chain toChain() {
        if (StringUtil.noText(data)) {
            throw new IllegalStateException("data is empty");
        }
        return chainParser.parse(this);
    }

}
