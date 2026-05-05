package com.xmut.shop.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiNDVIResponse {
    private String type;

    private String content;      // 存放文本 Token

    private List<Double> ndviList; // 存放 12 个月的 NDVI 数值数组
}
