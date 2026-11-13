package com.xmut.shop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xmut.shop.agent.AgentRouter;
import com.xmut.shop.agent.GisInteractionAgent;
import com.xmut.shop.agent.OrchestratorAgent;
import com.xmut.shop.agent.TemporalEvolutionAgent;
import com.xmut.shop.agent.WebGisChartAgent;
import com.xmut.shop.agent.WetlandSpatialAgent;
import com.xmut.shop.repository.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 中枢编排服务：按会话构建一级编排智能体，路由到二级业务智能体。
 * 与原有 4 个 Agent 接口文件完全隔离，互不影响。
 */
@Service
public class AgentOrchestrationService {

    @Autowired
    private OpenAiStreamingChatModel streamingChatModel;
    @Autowired
    private RedisChatMemoryStore redisChatMemoryStore;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private GisInteractionAgent gisInteractionAgent;
    @Autowired
    private TemporalEvolutionAgent temporalEvolutionAgent;
    @Autowired
    private WetlandSpatialAgent wetlandSpatialAgent;
    @Autowired
    private WebGisChartAgent webGisChartAgent;

    /**
     * 一级中枢编排入口
     *
     * @param memoryId 会话 ID（中枢记忆使用独立命名空间 memoryId:orch）
     * @param message  用户消息
     */
    public Flux<String> orchestrate(String memoryId, String message) {
        // 每个请求创建独立的路由器实例，携带会话上下文，避免并发串话
        AgentRouter router = new AgentRouter(memoryId,
                gisInteractionAgent, temporalEvolutionAgent, wetlandSpatialAgent,
                webGisChartAgent, redisTemplate, objectMapper);

        // 按会话构建中枢编排智能体（独立记忆命名空间）
        OrchestratorAgent orchestrator = AiServices.builder(OrchestratorAgent.class)
                .streamingChatModel(streamingChatModel)
                .chatMemory(MessageWindowChatMemory.builder()
                        .id(memoryId + ":orch")
                        .maxMessages(20)
                        .chatMemoryStore(redisChatMemoryStore)
                        .build())
                .tools(router)
                .build();

        return orchestrator.chat(message);
    }
}
