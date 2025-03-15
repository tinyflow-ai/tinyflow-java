package com.tinyflow.demo.controller;

import com.agentsflex.llm.openai.OpenAILlm;
import com.agentsflex.llm.openai.OpenAILlmConfig;
import com.agentsflex.llm.qwen.QwenLlm;
import com.agentsflex.llm.qwen.QwenLlmConfig;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tinyflow.demo.service.IWorkFlowService;
import dev.tinyflow.core.Tinyflow;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Controller
@CrossOrigin
public class WorkFlowController {

    @Resource
    private IWorkFlowService workFlowService;

    @PostMapping("/workflow/save")
    public ResponseEntity<String> save(@RequestBody JSONObject wf) {
        if (wf.containsKey("id") && wf.getString("id") != null && !"".equals(wf.getString("id"))) {
            workFlowService.update(wf);
        } else {
            workFlowService.save(wf);
        }
        return new ResponseEntity<>("Success", HttpStatus.OK);
    }

    @GetMapping("/workflow/get")
    public ResponseEntity<Map<String, Object>> get(@RequestParam int id) {
        return new ResponseEntity<>(workFlowService.getById(id), HttpStatus.OK);
    }

    @GetMapping("/workflow/getAll")
    public ResponseEntity<List<Map<String, Object>>> getAll() {
        return new ResponseEntity<>(workFlowService.get(), HttpStatus.OK);
    }

    @PostMapping("/workflow/exe")
    public ResponseEntity<Map<String, Object>> exe(@RequestBody JSONObject wf) {
        Tinyflow tinyflow = parseFlowParam(wf.getJSONObject("data").toJSONString());
        Map<String, Object> variables = wf.getJSONObject("param").getInnerMap();
        Map<String, Object> result = tinyflow.executeForResult(variables);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    private Tinyflow parseFlowParam(String graph) {
        JSONObject json = JSONObject.parseObject(graph);
        JSONArray nodeArr = json.getJSONArray("nodes");
        for (int i = 0; i < nodeArr.size(); i++) {
            JSONObject node = nodeArr.getJSONObject(i);
            if (node.getString("type").equals("llmNode")) {
                node.getJSONObject("data").put("topK", 10);
                node.getJSONObject("data").put("topP", 0.8);
                node.getJSONObject("data").put("temperature", 0.8);
                node.getJSONObject("data").put("maxTokens", 2048);
            }
        }
        Tinyflow tinyflow = null;
        for (int i = 0; i < nodeArr.size(); i++) {
            JSONObject node = nodeArr.getJSONObject(i);
            switch (node.getString("type")) {
                case "llmNode":
                    JSONObject data = node.getJSONObject("data");
                    QwenLlmConfig qwenLlmConfig = new QwenLlmConfig();
                    //  千问apikey
                    qwenLlmConfig.setApiKey("sk-xxxxxxxxxxx");
                    qwenLlmConfig.setModel("qwen-plus");
                    tinyflow = new Tinyflow(id -> new QwenLlm(qwenLlmConfig), json.toJSONString());
                    break;
                case "zsk":

                    break;
                default:
                    break;
            }
        }
        return tinyflow;
    }

}
