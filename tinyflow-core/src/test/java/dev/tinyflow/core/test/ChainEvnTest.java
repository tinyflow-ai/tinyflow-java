package dev.tinyflow.core.test;

import dev.tinyflow.core.chain.Chain;
import dev.tinyflow.core.node.CodeNode;
import dev.tinyflow.core.util.Maps;

public class ChainEvnTest {

    public static void main(String[] args) {
        Chain chain = new Chain();
        CodeNode codeNode = new CodeNode();
        codeNode.setCode("console.log('>>>JAVA_HOME: {{env.sys.JAVA_HOME}}<<<<')");
        codeNode.setEngine("js");
        chain.addNode(codeNode);
        chain.execute(Maps.of());
    }
}
