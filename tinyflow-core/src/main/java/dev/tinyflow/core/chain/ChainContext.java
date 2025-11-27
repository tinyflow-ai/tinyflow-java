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
package dev.tinyflow.core.chain;


public class ChainContext {

    private static final ThreadLocal<Chain> TL_CHAIN = new ThreadLocal<>();

    private static final ThreadLocal<ChainNode> TL_NODE = new ThreadLocal<>();

    public static Chain getCurrentChain() {
        return TL_CHAIN.get();
    }

    public static ChainNode getCurrentNode() {
        return TL_NODE.get();
    }

    static void setChain(Chain chain) {
        TL_CHAIN.set(chain);
    }

    static void clearChain() {
        TL_CHAIN.remove();
    }

    static void setNode(ChainNode node) {
        TL_NODE.set(node);
    }

    static void clearNode() {
        TL_NODE.remove();
    }

}
