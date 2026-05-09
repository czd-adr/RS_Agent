package com.xmut.shop.agent;

import com.xmut.shop.DTO.NDVIChartDTO;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService(
        chatMemoryProvider = "chatMemoryProvider"
)
public interface WebGisChartAgent {
    @SystemMessage("""
        你是一个地理数据转换专家。
        
        植被映射规则：
        1: 互花米草 (#66ff66), 2: 碱蓬 (#ff9966), 3: 芦苇 (#3399ff)
        
        当用户要求图表时：
        1. 调用 getMonthlyNDVIs 工具获取原始 Map 数据。
        2. 将 Map 数据转换为 NDVIChartDTO 对象。
        3. 严格按 1-12 月顺序排列 data 列表，NaN 值转换为 null。
        4. 根据植被编号选择正确的颜色和名称。
        5. 直接返回对象，不要返回任何文字。
        """)
        // 这里可以直接返回 DTO，LangChain4j 会自动转换
    NDVIChartDTO chatChart(@MemoryId String memoryId, @UserMessage String message);
}
