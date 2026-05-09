package com.xmut.shop.repository;

import dev.langchain4j.data.message.*;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public class RedisChatMemoryStore implements ChatMemoryStore {

    private static final String SUMMARY_KEY = "chat_memory_summaries";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public List<ChatMessage> getMessages(Object memoryId){
        //获取会话消息
        String json = redisTemplate.opsForValue().get(memoryId);
        //json转为chatMessage List
        List<ChatMessage> list = ChatMessageDeserializer.messagesFromJson(json);
        return list;
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> list) {
        String idStr = memoryId.toString();
        String json = ChatMessageSerializer.messagesToJson(list);

        // 1. 存储消息内容 (String结构)
        redisTemplate.opsForValue().set(idStr, json, Duration.ofDays(20));

        // 2. 存储摘要映射 (Hash结构)
        // 如果该 ID 还没有摘要，则生成一个默认摘要（如：第一条消息的内容）
        if (!redisTemplate.opsForHash().hasKey(SUMMARY_KEY, idStr)) {
            String summary = generateSummary(list);
            redisTemplate.opsForHash().put(SUMMARY_KEY, idStr, summary);
        }
    }
    private String generateSummary(List<ChatMessage> list) {
        if (list == null || list.isEmpty()) return "新会话";

        // 寻找第一条用户消息作为标题
        return list.stream()
                .filter(m -> m.type() == ChatMessageType.USER)
                .findFirst()
                .map(m -> {
                    String content = "";
                    // 核心修正：判断类型并提取文本
                    if (m instanceof UserMessage) {
                        // UserMessage 提供了 contents() 或直接的 text() 取决于版本
                        // 推荐使用这种通用的获取方式
                        content = ((UserMessage) m).singleText();
                    } else {
                        content = m.toString(); // 兜底方案
                    }

                    return content.length() > 15 ? content.substring(0, 15) + "..." : content;
                })
                .orElse("分析会话 " + System.currentTimeMillis() % 10000);
    }

    // 获取完整的会话列表对象
    public Map<Object, Object> getAllSessionSummaries() {
        return redisTemplate.opsForHash().entries(SUMMARY_KEY);
    }
    @Override
    public void deleteMessages(Object memoryId) {
        String idStr = memoryId.toString();
        redisTemplate.delete(idStr);
        // 同时从集合中移除
        redisTemplate.opsForSet().remove("chat_memory_ids", idStr);
    }
    public Set<String> getAllMemoryIds() {
        return redisTemplate.opsForSet().members("chat_memory_ids");
    }
    //删除会话
    public void deleteSession(String memoryId) {
        // 1. 删除聊天记录原文 (String 结构)
        redisTemplate.delete(memoryId);

        // 2. 从摘要映射表中移除该 ID (Hash 结构)
        redisTemplate.opsForHash().delete(SUMMARY_KEY, memoryId);

        // 3. (可选) 如果你之前维护了 chat_memory_ids 的 Set，也一并移除
        redisTemplate.opsForSet().remove("chat_memory_ids", memoryId);
    }
}
