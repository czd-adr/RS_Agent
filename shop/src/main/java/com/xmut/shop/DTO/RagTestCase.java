package com.xmut.shop.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@Builder
@NoArgsConstructor
public  class RagTestCase {
    String dimension;      // 实验维度名称
    String question;       // 测试问题
    List<String> expected; // 预设标准术语集

    public RagTestCase(String dimension, String question, List<String> expected) {
        this.dimension = dimension;
        this.question = question;
        this.expected = expected;
    }
}
