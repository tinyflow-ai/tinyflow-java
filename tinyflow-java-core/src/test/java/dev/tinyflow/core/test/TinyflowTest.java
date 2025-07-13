package dev.tinyflow.core.test;

import com.agentsflex.core.chain.Chain;
import com.agentsflex.core.chain.ChainEvent;
import com.agentsflex.core.chain.ChainNode;
import com.agentsflex.core.chain.listener.ChainEventListener;
import com.agentsflex.core.chain.listener.ChainOutputListener;
import com.agentsflex.llm.openai.OpenAILlm;
import dev.tinyflow.core.Tinyflow;

import java.util.HashMap;
import java.util.Map;

public class TinyflowTest {

    //    static String data1 = "";
    static String data1 = "{\"nodes\":[{\"id\":\"2\",\"type\":\"llmNode\",\"data\":{\"title\":\"大模型\",\"description\":\"处理大模型相关问题\",\"expand\":true,\"outputDefs\":[{\"id\":\"pyiig8ntGWZhVdVz\",\"dataType\":\"Object\",\"name\":\"param\",\"children\":[{\"id\":\"1\",\"name\":\"newParam1\",\"dataType\":\"String\"},{\"id\":\"2\",\"name\":\"newParam2\",\"dataType\":\"String\"}]}]},\"position\":{\"x\":600,\"y\":50},\"measured\":{\"width\":334,\"height\":687},\"selected\":false},{\"id\":\"3\",\"type\":\"startNode\",\"data\":{\"title\":\"开始节点\",\"description\":\"开始定义输入参数\",\"expand\":true,\"parameters\":[{\"id\":\"Q37GZ5KKvPpCD7Cs\",\"name\":\"name\"}]},\"position\":{\"x\":150,\"y\":25},\"measured\":{\"width\":306,\"height\":209},\"selected\":false},{\"id\":\"4\",\"type\":\"endNode\",\"data\":{\"title\":\"结束节点\",\"description\":\"结束定义输出参数\",\"expand\":true,\"outputDefs\":[{\"id\":\"z7fOwoTjQ7AbUJdm\",\"ref\":\"3.name\",\"name\":\"test\"}]},\"position\":{\"x\":994,\"y\":218},\"measured\":{\"width\":334,\"height\":209},\"selected\":false,\"dragging\":false}],\"edges\":[{\"markerEnd\":{\"type\":\"arrowclosed\",\"width\":20,\"height\":20},\"source\":\"3\",\"target\":\"2\",\"id\":\"xy-edge__3-2\"},{\"markerEnd\":{\"type\":\"arrowclosed\",\"width\":20,\"height\":20},\"source\":\"2\",\"target\":\"4\",\"id\":\"xy-edge__2-4\"}],\"viewport\":{\"x\":250,\"y\":100,\"zoom\":1}}";

    public static void main(String[] args) {
        Tinyflow tinyflow = new Tinyflow(data1);
        tinyflow.setLlmProvider(id -> OpenAILlm.of(""));

        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "michael");

        Chain chain = tinyflow.toChain();
        chain.addEventListener(new ChainEventListener() {
            @Override
            public void onEvent(ChainEvent event, Chain chain) {
                System.out.println(event.toString());
            }
        });

        chain.addOutputListener(new ChainOutputListener() {
            @Override
            public void onOutput(Chain chain, ChainNode node, Object outputMessage) {
                System.out.println("outputMessage: " + outputMessage);
            }
        });

        Map<String, Object> result = chain.executeForResult(variables);

        System.out.println(result);
    }
}
