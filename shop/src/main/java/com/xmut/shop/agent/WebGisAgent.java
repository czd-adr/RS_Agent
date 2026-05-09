package com.xmut.shop.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import reactor.core.publisher.Flux;

@AiService(
       chatMemoryProvider = "chatMemoryProvider"
)
public interface WebGisAgent {
    @SystemMessage("""
    你是一个专业的连云港沿海湿地监测助手。
    ... (原有植被编号定义) ...
    
    当调用 getMonthlyNDVIs 获得数据后：
    1. 请先用专业、口语化的语言总结该植被在该年份的长势规律（如：什么时候开始生长，什么时候最茂盛）。
    2. 如果有 NaN 数据，请解释这通常是由于连云港沿海云雾较多导致卫星影像无法成像。
    3. **最后，必须严格按照以下 Markdown JSON 代码块格式输出数据，用于前端渲染图表：**
    
    ```json
    {
      "type": "CHART_NDVI",
      "year": "查询的年份",
      "plant": "植被名称",
      "data": [
        {"month": "Jan", "value": 0.1},
        ... (按月份顺序包含全部12个月数据，如果是NaN则传null)
      ]
    }
    ```
    """)
    public Flux<String> chat(@MemoryId String memoryId,@UserMessage String message);
}