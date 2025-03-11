package com.tinyflow.demo.service;

import com.alibaba.fastjson.JSONObject;

import java.util.List;
import java.util.Map;

public interface IWorkFlowService {

    int save(JSONObject wf);

    int update(JSONObject wf);

    List<Map<String, Object>> get();

    Map<String, Object> getById(int id);
}
