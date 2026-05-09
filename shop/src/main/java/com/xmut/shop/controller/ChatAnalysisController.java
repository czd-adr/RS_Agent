package com.xmut.shop.controller;

import com.xmut.shop.DTO.AiNDVIResponse;
import com.xmut.shop.DTO.ChatRequest;
import com.xmut.shop.DTO.MessageDTO;
import com.xmut.shop.DTO.NDVIChartDTO;
import com.xmut.shop.agent.WebGisAgent;
import com.xmut.shop.agent.WebGisChartAgent;
import com.xmut.shop.repository.RedisChatMemoryStore;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/WebGISAgent")
public class ChatAnalysisController {

    @Autowired
    private RedisChatMemoryStore redisChatMemoryStore;
    @Autowired
    private WebGisAgent webGisAgent; // 注入你定义的 AiService 接口
    @Autowired
    private WebGisChartAgent webGisChartAgent;
    @Autowired
    private SamplePointsController samplePointsController;

    @RequestMapping(value = "/chat",produces = "text/html;charset=utf-8")
    public Flux<String> chat(String memoryId,String message) {
        Flux<String> result = webGisAgent.chat(memoryId,message);
        return result;
    }//   用户id和会话id区分上下文方案：发起chat时，通过store获取用户id，拼接时间挫Date生成会话id，保存在数据库chat_message表中，表字段执
    //需要有conversation_id,user_id,id,List会话列表时，通过user_id关联查询message表，

    @GetMapping("/sessions")
    public List<Map<String, String>> getChatSessions() {
        // 1. 获取 memoryId -> title 的映射
        Map<Object, Object> summaries = redisChatMemoryStore.getAllSessionSummaries();

        List<Map<String, String>> sessionList = new ArrayList<>();

        // 2. 转换为 List 结构
        summaries.forEach((id, title) -> {
            Map<String, String> session = new HashMap<>();
            // 关键修正：使用 put 而非 setProperty
            session.put("memoryId", id.toString());
            session.put("title", title.toString());
            session.put("time", "最近更新");
            sessionList.add(session);
        });

        return sessionList;
    }
    // 在你的 ChatController.java 中
    @GetMapping("/history")
    public List<MessageDTO> getHistory(@RequestParam String memoryId) {
        // 从 Redis 获取原始消息
        List<ChatMessage> chatMessages = redisChatMemoryStore.getMessages(memoryId);

        // 转换为 DTO 列表
        return chatMessages.stream()
                .map(msg -> {
                    String role = msg.type().name().toLowerCase(); // user, ai, system
                    String content = "";

                    if (msg instanceof UserMessage) {
                        content = ((UserMessage) msg).singleText();
                    } else if (msg instanceof AiMessage) {
                        content = ((AiMessage) msg).text();
                    } else {
                        content = msg.toString(); // 针对 SystemMessage 等的处理
                    }

                    return new MessageDTO(role, content);
                })
                .collect(Collectors.toList());
    }
    @DeleteMapping("/session/{memoryId}")
    public ResponseEntity<String> deleteChatSession(@PathVariable String memoryId) {
        redisChatMemoryStore.deleteSession(memoryId);
        return ResponseEntity.ok("会话已删除");
    }

    @GetMapping(value = "/chatChart", produces = MediaType.APPLICATION_JSON_VALUE)
    public NDVIChartDTO chatChart(@RequestParam String memoryId, @RequestParam String message) {
        // AI 会在这里自动完成：意图识别 -> 调用 getMonthlyNDVIs -> 组装 NDVIChartDTO
        return webGisChartAgent.chatChart(memoryId, message);
    }
}
