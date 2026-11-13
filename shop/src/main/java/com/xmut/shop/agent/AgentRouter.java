package com.xmut.shop.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * 中枢编排智能体的下级智能体路由工具。
 * 每个会话请求创建一个实例（携带 memoryId 上下文），避免并发串话。
 */
public class AgentRouter {

    // 会话上下文 Key：记录该会话最近调用的下级智能体（用于承接追问）
    private static final String CONTEXT_KEY_PREFIX = "rs:session:";
    private static final String CONTEXT_KEY_SUFFIX = ":last_agent";
    private static final Duration CONTEXT_TTL = Duration.ofHours(2);

    private final String memoryId;
    private final GisInteractionAgent gisInteractionAgent;
    private final TemporalEvolutionAgent temporalEvolutionAgent;
    private final WetlandSpatialAgent wetlandSpatialAgent;
    private final WebGisChartAgent webGisChartAgent;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public AgentRouter(String memoryId,
                       GisInteractionAgent gisInteractionAgent,
                       TemporalEvolutionAgent temporalEvolutionAgent,
                       WetlandSpatialAgent wetlandSpatialAgent,
                       WebGisChartAgent webGisChartAgent,
                       StringRedisTemplate redisTemplate,
                       ObjectMapper objectMapper) {
        this.memoryId = memoryId;
        this.gisInteractionAgent = gisInteractionAgent;
        this.temporalEvolutionAgent = temporalEvolutionAgent;
        this.wetlandSpatialAgent = wetlandSpatialAgent;
        this.webGisChartAgent = webGisChartAgent;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Tool("GIS 空间交互智能体：当用户提出需要在地图前端执行的操作（显示/隐藏图层、像元点查、绘制 ROI 框选、地图视角聚焦等）时调用。")
    public String delegateToGisInteraction(@P("用户的完整原始问题") String message) {
        remember("gis");
        return gisInteractionAgent.chat(memoryId + ":gis", message);
    }

    @Tool("时空演变分析智能体：当用户询问植被 NDVI 长势、逐月趋势、季节性规律、生长变化、年际对比时调用。")
    public String delegateToTemporalEvolution(@P("用户的完整原始问题") String message) {
        remember("temporal");
        return temporalEvolutionAgent.chat(memoryId + ":temporal", message);
    }

    @Tool("湿地空间分析智能体：当用户询问湿地面积、占比、区域叠加统计、周边空间影响/衰减、生态压力时调用。")
    public String delegateToWetlandSpatial(@P("用户的完整原始问题") String message) {
        remember("spatial");
        return wetlandSpatialAgent.chat(memoryId + ":spatial", message);
    }

    @Tool("图表组装智能体：当用户要求生成 NDVI 图表/曲线时调用。")
    public String delegateToChart(@P("用户的完整原始问题") String message) {
        remember("chart");
        try {
            return objectMapper.writeValueAsString(webGisChartAgent.chatChart(memoryId + ":chart", message));
        } catch (Exception e) {
            return "图表生成失败：" + e.getMessage();
        }
    }

    @Tool("查询本会话上次调用的下级智能体，用于承接上文的追问。")
    public String getSessionContext() {
        String v = redisTemplate.opsForValue().get(contextKey());
        return v == null ? "无记录" : "上次调用的下级智能体：" + v;
    }

    private void remember(String agentCode) {
        redisTemplate.opsForValue().set(contextKey(), agentCode, CONTEXT_TTL);
    }

    private String contextKey() {
        return CONTEXT_KEY_PREFIX + memoryId + CONTEXT_KEY_SUFFIX;
    }
}
