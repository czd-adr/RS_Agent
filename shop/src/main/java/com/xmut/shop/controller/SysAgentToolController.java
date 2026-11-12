package com.xmut.shop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xmut.shop.common.Result;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import com.xmut.shop.entity.SysAgentTool;
import com.xmut.shop.service.SysAgentToolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * RS-AGENT 工具集注册表接口：供前端/编排层查询可用工具
 * </p>
 *
 * @author Bennu
 * @since 2026-11-02
 */
@RestController
@RequestMapping("/agent-tool")
public class SysAgentToolController {

    @Autowired
    private SysAgentToolService sysAgentToolService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 工具集列表（仅返回启用状态）
     * 例：GET /agent-tool/list 或 GET /agent-tool/list?category=GIS_CLIENT
     */
    @GetMapping("/list")
    @Tool(name = "getAgentToolList", value = """
            查询 RS-AGENT 系统中当前可用的智能体工具清单（仅返回启用状态的工具）。
            每个工具包含：工具编码 toolCode、中文名称 toolName、分类 category、功能描述 description、参数 Schema parametersSchema。

            【适用场景】
            当用户提出需要在地图前端执行的操作时（例如：显示/隐藏图层、像元点查、绘制 ROI 框选、地图视角聚焦等），
            必须先调用本工具查询可用的前端工具及其参数要求，再告诉前端执行哪个工具、传什么参数。

            【参数】
            category：可选，工具分类过滤（GIS_CLIENT 前端工具 / ALGORITHM 后端算法 / RAG 知识库）。不传则返回全部启用工具。
            """)
    public Result list(@P("工具分类，可选：GIS_CLIENT、ALGORITHM、RAG；不传则返回全部启用工具") @RequestParam(required = false) String category) {
        List<SysAgentTool> tools = sysAgentToolService.listEnabledTools(category);
        List<Map<String, Object>> data = tools.stream()
                .map(this::toMap)
                .collect(Collectors.toList());
        return Result.suc(data, (long) data.size());
    }

    /**
     * 单个工具详情（按工具编码）
     * 例：GET /agent-tool/toggle_layer
     */
    @GetMapping("/{toolCode}")
    @Tool(name = "getAgentToolDetail", value = """
            按工具编码查询单个智能体工具的详细信息，尤其是参数 Schema（parametersSchema），
            用于确认调用该工具需要哪些必填参数、参数的取值范围和含义。
            """)
    public Result getByCode(@P("工具唯一编码，例如：toggle_layer、trigger_inspector、fly_to_extent") @PathVariable String toolCode) {
        SysAgentTool tool = sysAgentToolService.getByToolCode(toolCode);
        if (tool == null) {
            return Result.fail();
        }
        return Result.suc(toMap(tool));
    }

    /**
     * 实体转 Map，并把 parameters_schema 解析为 JSON 对象便于前端直接使用
     */
    private Map<String, Object> toMap(SysAgentTool tool) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", tool.getId());
        map.put("toolCode", tool.getToolCode());
        map.put("toolName", tool.getToolName());
        map.put("category", tool.getCategory());
        map.put("description", tool.getDescription());
        map.put("parametersSchema", parseJson(tool.getParametersSchema()));
        map.put("isEnabled", tool.getIsEnabled());
        map.put("createdAt", tool.getCreatedAt());
        map.put("updatedAt", tool.getUpdatedAt());
        return map;
    }

    /**
     * 解析 JSONB 字段，解析失败时原样返回字符串
     */
    private Object parseJson(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return json;
        }
    }
}
