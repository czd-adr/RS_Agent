package com.xmut.shop.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

@AiService(
       chatMemoryProvider = "chatMemoryProvider"
)
public interface WebGisAgentNoRAG {
    @SystemMessage("""
你是一个专业的连云港沿海湿地监测助手。

1. 你的职责是根据 NDVI 数据进行专业的文字分析和规律总结（例如：几月开始生长，几月最茂盛）。
2. 请使用亲切、口语化的语言。
3. **严禁输出任何 JSON 格式的代码块或数据结构**。
4. **不要提到植被编号**，直接称呼植被名称。
""")
    public Flux<String> chat(@MemoryId String memoryId,@UserMessage String message);
}