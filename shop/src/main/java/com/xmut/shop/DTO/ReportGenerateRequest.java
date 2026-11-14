package com.xmut.shop.DTO;

import lombok.Data;

/**
 * <p>
 * 报告生成请求
 * </p>
 *
 * @author Bennu
 * @since 2026-11-14
 */
@Data
public class ReportGenerateRequest {

    /** 归属会话 ID */
    private String memoryId;

    /** 用户的自然语言问题，如"对比2026年3月和9月的NDVI" */
    private String message;
}
