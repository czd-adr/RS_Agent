package com.xmut.shop.DTO;

import lombok.Data;

/**
 * <p>
 * 报告分析参数（LLM 结构化提取结果）
 * </p>
 *
 * @author Bennu
 * @since 2026-11-14
 */
@Data
public class MonthlyCompareParam {

    /** 植被编号：1 互花米草、2 碱蓬、3 芦苇、4 其他 */
    private Integer grass;

    /** 第一个时相，格式 yyyy-MM，如 2026-03（对比场景） */
    private String date1;

    /** 第二个时相，格式 yyyy-MM，如 2026-09（对比场景） */
    private String date2;

    /** 完整年份，如 2026（月度趋势场景） */
    private Integer year;

    /** 分析意图：monthly_compare 双时相对比 / monthly_trend 月度趋势 */
    private String intent;
}
