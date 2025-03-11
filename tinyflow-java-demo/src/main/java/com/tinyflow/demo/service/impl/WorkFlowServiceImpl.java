package com.tinyflow.demo.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.tinyflow.demo.service.IWorkFlowService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Service
public class WorkFlowServiceImpl implements IWorkFlowService {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public int save(JSONObject wf) {
        return jdbcTemplate.update("insert into workflows (app_id, name, graph) values (?, ?, ?)", 1, wf.getString("name"), wf.getJSONObject("data").toJSONString());
    }

    @Override
    public int update(JSONObject wf) {
        return jdbcTemplate.update("update workflows set graph = ? where id = ?", wf.getJSONObject("data").toJSONString(), wf.getInteger("id"));
    }

    @Override
    public List<Map<String, Object>> get() {
        return jdbcTemplate.queryForList("select * from workflows");
    }

    @Override
    public Map<String, Object> getById(int id) {
        return jdbcTemplate.queryForMap("select * from workflows where id = ?", id);
    }
}
