package dev.tinyflow.core.test;

import com.agentsflex.chain.node.JsExecNode;
import com.agentsflex.core.chain.Chain;
import com.agentsflex.core.chain.ChainEdge;
import com.agentsflex.core.chain.JsCodeCondition;
import org.junit.Test;

import java.util.HashMap;

public class ChainAsyncStringTest {

    @Test
    public void test() {

        System.out.println("start: "+ Thread.currentThread().getId());

        Chain chain = new Chain();

        JsExecNode a = new JsExecNode();
        a.setId("a");
        a.setCode("console.log('aaaa....')");
        chain.addNode(a);

        /// //bbbbb
        JsExecNode b = new JsExecNode();
        b.setId("b");
        b.setCode("console.log('bbbb....')");
        b.setAsync(true);
        chain.addNode(b);


        /// //////cccccc
        JsExecNode c = new JsExecNode();
        c.setId("c");
        c.setCode("console.log('cccc....')");
        c.setAsync(true);
        chain.addNode(c);


        /// /////dddd
        JsExecNode d = new JsExecNode();
        d.setCode("console.log('dddd....')");
        d.setId("d");
        d.setCondition(new JsCodeCondition("_context.isUpstreamFullyExecuted()"));
        chain.addNode(d);

        ChainEdge ab = new ChainEdge();
        ab.setSource("a");
        ab.setTarget("b");
        chain.addEdge(ab);

        ChainEdge ac = new ChainEdge();
        ac.setSource("a");
        ac.setTarget("c");
        chain.addEdge(ac);


        ChainEdge bd = new ChainEdge();
        bd.setSource("b");
        bd.setTarget("d");
        chain.addEdge(bd);

        ChainEdge cd = new ChainEdge();
        cd.setSource("c");
        cd.setTarget("d");
        chain.addEdge(cd);

        // A→B→D
        //  ↘C↗
        chain.executeForResult(new HashMap<>());

    }
}
