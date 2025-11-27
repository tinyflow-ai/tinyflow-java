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
package dev.tinyflow.core.chain.impl;


import dev.tinyflow.core.chain.Chain;
import dev.tinyflow.core.chain.ChainEdge;
import dev.tinyflow.core.chain.ChainNode;

public class SequentialChain extends Chain {

    @Override
    public void addNode(ChainNode chainNode) {
        super.addNode(chainNode);

        if (this.nodes.size() < 2) {
            return;
        }

        String sourceId = this.nodes.get(this.nodes.size() - 2).getId();
        String targetId = this.nodes.get(this.nodes.size() - 1).getId();

        ChainEdge edge = new ChainEdge();
        edge.setSource(sourceId);
        edge.setTarget(targetId);

        super.addEdge(edge);
    }
}
