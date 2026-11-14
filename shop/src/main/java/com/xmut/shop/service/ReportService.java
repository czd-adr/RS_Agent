package com.xmut.shop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xmut.shop.DTO.MonthlyCompareParam;
import com.xmut.shop.agent.ReportParamExtractor;
import com.xmut.shop.agent.ReportWriterAgent;
import com.xmut.shop.controller.DifferenceController;
import com.xmut.shop.controller.SamplePointsController;
import com.xmut.shop.entity.SysAnalysisReport;
import com.xmut.shop.mapper.SysAnalysisReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 分析报告生成服务：
 * 参数提取（LLM 结构化输出） -> 直调工具取原始数据 -> LLM 撰写 Markdown -> 落库
 */
@Service
public class ReportService {

    private static final Map<Integer, String> GRASS_NAMES = Map.of(
            1, "互花米草",
            2, "碱蓬",
            3, "芦苇",
            4, "其他");

    @Autowired
    private ReportParamExtractor reportParamExtractor;
    @Autowired
    private ReportWriterAgent reportWriterAgent;
    @Autowired
    private DifferenceController differenceController;
    @Autowired
    private SamplePointsController samplePointsController;
    @Autowired
    private SysAnalysisReportMapper sysAnalysisReportMapper;
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 生成分析报告并落库，返回 {reportId, title, reportType, content}
     */
    public Map<String, Object> generate(String memoryId, String message) throws Exception {
        // 1. LLM 结构化提取参数
        MonthlyCompareParam param = reportParamExtractor.extract(message);

        // 2. 按意图分支直调工具，获取原始数据
        Map<String, Object> data = new LinkedHashMap<>();
        String title;
        String reportType;

        if ("monthly_trend".equals(param.getIntent())) {
            int grass = param.getGrass() == null ? 1 : param.getGrass();
            int year = param.getYear() == null ? LocalDate.now().getYear() : param.getYear();
            Map<String, Double> monthly = samplePointsController.getMonthlyNDVIs(grass, year);
            String grassName = GRASS_NAMES.getOrDefault(grass, "植被");
            data.put("vegetation", grassName);
            data.put("year", year);
            data.put("monthlyNDVI", monthly);
            reportType = "monthly_trend";
            title = year + " 年 " + grassName + " NDVI 长势分析报告";
        } else {
            // 默认走双时相对比
            String date1 = param.getDate1();
            String date2 = param.getDate2();
            if (date1 == null || date2 == null) {
                throw new IllegalArgumentException("无法从问题中提取两个对比时相（yyyy-MM），请明确月份，例如：对比2026年3月和9月的NDVI");
            }
            Map<String, Object> diff = differenceController.diffNDVI(date1, date2);
            data.put("date1", date1);
            data.put("date2", date2);
            data.put("diff", diff);
            reportType = "monthly_compare";
            title = date1 + " 与 " + date2 + " NDVI 对比分析报告";
        }

        // 3. LLM 撰写 Markdown 报告
        String writerInput = "报告标题：" + title + "\n用户问题：" + message
                + "\n分析数据(JSON)：\n" + objectMapper.writeValueAsString(data);
        String markdown = reportWriterAgent.write(writerInput);

        // 4. 落库
        SysAnalysisReport report = new SysAnalysisReport();
        report.setMemoryId(memoryId);
        report.setTitle(title);
        report.setReportType(reportType);
        report.setContent(markdown);
        report.setParams(objectMapper.writeValueAsString(data));
        report.setStatus((short) 1);
        report.setCreatedAt(LocalDateTime.now());
        // 使用自定义 insert（params 需 CAST 为 jsonb）
        sysAnalysisReportMapper.insertReport(report);

        // 5. 返回
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reportId", report.getId());
        result.put("title", title);
        result.put("reportType", reportType);
        result.put("content", markdown);
        return result;
    }
}
