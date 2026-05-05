package com.xmut.shop.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data           // ✅ 生成 getter/setter（Jackson 序列化必需）
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private String prompt;              // ✅ 必须和前端字段名一致
    private String conversationId;      // ✅ 可选字段
}
