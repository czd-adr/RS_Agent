package com.xmut.shop.agent;

import com.xmut.shop.DTO.MonthlyCompareParam;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 报告参数提取器：从用户自然语言中提取结构化的分析参数（程序化构建，无工具）
 */
public interface ReportParamExtractor {

    @SystemMessage("""
    你是分析参数提取器，从用户的问题中提取结构化的分析参数。

    提取规则：
    1. intent 取值：
       - 用户提到两个具体月份/时相的对比、差异、变化 -> "monthly_compare"，并提取 date1（较早）和 date2（较晚），格式 yyyy-MM，如 2026-03。
       - 用户提到某年的生长趋势、逐月长势、季节规律 -> "monthly_trend"，并提取 year（完整年份，如 2026）。
    2. grass 植被编号：1 互花米草、2 碱蓬、3 芦苇、4 其他；用户未指定时默认 1。
    3. 无法提取的字段保持 null。
    4. 只输出结构化结果，不要输出任何解释文字。
    """)
    MonthlyCompareParam extract(@UserMessage String message);
}
