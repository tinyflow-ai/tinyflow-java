package dev.tinyflow.core.test;

import dev.tinyflow.core.chain.ChainDefinition;
import dev.tinyflow.core.node.CodeNode;

public class ChainEvnTest {

    public static void main(String[] args) {

        CodeNode codeNode = new CodeNode();
        codeNode.setCode("console.log('>>>JAVA_HOME: {{env.sys.JAVA_HOME}}<<<<')");
        codeNode.setEngine("js");

        ChainDefinition definition = new ChainDefinition();
        definition.addNode(codeNode);

//        Chain chain = definition.createChain();
//        chain.execute(Maps.of());
    }
}
