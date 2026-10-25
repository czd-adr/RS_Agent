package com.xmut.shop.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.util.List;
@Component
@Primary // 确保优先使用数据库实现
public class DbChatHistoryRepository implements ChatHistoryRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void save(String userId, String chatId) {
        // 使用 INSERT IGNORE (MySQL) 或 逻辑判断 (PostgreSQL)
        // 确保不会因为重复插入相同的会话 ID 而报错
        String sql = """
            INSERT INTO chat_messages (conversation_id, user_id, role, content) 
            VALUES (?, ?, 'SYSTEM', 'CONVERSATION_STARTED')
            ON CONFLICT (conversation_id) DO NOTHING
            """;
        jdbcTemplate.update(sql, chatId, userId);
    }

    @Override
    public List<String> getChatIds(String userId) {
        String sql = "SELECT conversation_id FROM chat_sessions WHERE user_id = ?";
        return jdbcTemplate.queryForList(sql, String.class, userId);
    }
}