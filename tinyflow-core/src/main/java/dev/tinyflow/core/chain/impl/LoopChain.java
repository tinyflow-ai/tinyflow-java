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


import dev.tinyflow.core.chain.ChainEdge;
import dev.tinyflow.core.chain.NodeContext;
import dev.tinyflow.core.node.StartNode;

public class LoopChain extends SequentialChain {

    private int maxLoopCount = Integer.MAX_VALUE;
    private int executeCount = 0;

    public LoopChain() {
        this.addNode(new StartNode());
    }

    public int getMaxLoopCount() {
        return maxLoopCount;
    }

    public void setMaxLoopCount(int maxLoopCount) {
        this.maxLoopCount = maxLoopCount;
    }

    public void close() {
        if (this.nodes.size() < 2) {
            return;
        }

        String sourceId = this.nodes.get(this.nodes.size() - 1).getId();
        String targetId = this.nodes.get(1).getId();

        ChainEdge edge = new ChainEdge();
        edge.setSource(sourceId);
        edge.setTarget(targetId);

        super.addEdge(edge);
    }

    @Override
    protected void onNodeExecuteAfter(NodeContext nodeContext) {
        if (executeCount++ >= maxLoopCount) {
            stopNormal("Loop to the maxLoopCount limit");
        }
    }

}
