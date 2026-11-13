package com.xmut.shop.config;

import com.xmut.shop.agent.GisInteractionAgent;
import com.xmut.shop.agent.TemporalEvolutionAgent;
import com.xmut.shop.agent.WetlandSpatialAgent;
import com.xmut.shop.controller.DifferenceController;
import com.xmut.shop.controller.RfAreaController;
import com.xmut.shop.controller.SamplePointsController;
import com.xmut.shop.controller.SpatialController;
import com.xmut.shop.controller.SysAgentToolController;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 二级业务智能体装配：程序化构建，实现工具隔离
 * （每个智能体只暴露自己业务域内的 @Tool，互不串扰）
 */
@Configuration
public class MultiAgentConfig {

    /**
     * GIS 空间交互智能体：只暴露工具注册表查询工具
     */
    @Bean
    public GisInteractionAgent gisInteractionAgent(OpenAiChatModel chatModel,
                                                   ChatMemoryProvider chatMemoryProvider,
                                                   SysAgentToolController sysAgentToolController) {
        return AiServices.builder(GisInteractionAgent.class)
                .chatModel(chatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(sysAgentToolController)
                .build();
    }

    /**
     * 时空演变分析智能体：只暴露逐月 NDVI 工具和双时相差异工具
     */
    @Bean
    public TemporalEvolutionAgent temporalEvolutionAgent(OpenAiChatModel chatModel,
                                                         ChatMemoryProvider chatMemoryProvider,
                                                         SamplePointsController samplePointsController,
                                                         DifferenceController differenceController) {
        return AiServices.builder(TemporalEvolutionAgent.class)
                .chatModel(chatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(samplePointsController, differenceController)
                .build();
    }

    /**
     * 湿地空间分析智能体：只暴露面积与空间衰减工具
     */
    @Bean
    public WetlandSpatialAgent wetlandSpatialAgent(OpenAiChatModel chatModel,
                                                   ChatMemoryProvider chatMemoryProvider,
                                                   RfAreaController rfAreaController,
                                                   SpatialController spatialController) {
        return AiServices.builder(WetlandSpatialAgent.class)
                .chatModel(chatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(rfAreaController, spatialController)
                .build();
    }
}
