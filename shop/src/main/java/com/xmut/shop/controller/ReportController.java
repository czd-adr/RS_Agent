package com.xmut.shop.controller;

import com.xmut.shop.DTO.ReportGenerateRequest;
import com.xmut.shop.common.MarkdownUtils;
import com.xmut.shop.common.Result;
import com.xmut.shop.entity.SysAnalysisReport;
import com.xmut.shop.service.ReportService;
import com.xmut.shop.service.SysAnalysisReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 分析报告接口：生成、列表、详情、HTML 预览、下载（md/doc）、逻辑删除
 */
@RestController
@RequestMapping("/report")
public class ReportController {

    @Autowired
    private ReportService reportService;
    @Autowired
    private SysAnalysisReportService sysAnalysisReportService;

    /**
     * 生成分析报告
     * 例：POST /report/generate  body: {"memoryId":"xx","message":"对比2026年3月和9月的NDVI"}
     */
    @PostMapping("/generate")
    public Result generate(@RequestBody ReportGenerateRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            return Result.fail();
        }
        try {
            return Result.suc(reportService.generate(request.getMemoryId(), request.getMessage()));
        } catch (Exception e) {
            Result r = Result.fail();
            r.setMsg(e.getMessage());
            return r;
        }
    }

    /**
     * 报告列表（可按会话过滤）
     * 例：GET /report/list?memoryId=xx
     */
    @GetMapping("/list")
    public Result list(@RequestParam(required = false) String memoryId) {
        List<SysAnalysisReport> list = sysAnalysisReportService.listByMemoryId(memoryId);
        return Result.suc(list, (long) list.size());
    }

    /**
     * 报告详情（Markdown 原文）
     * 例：GET /report/1
     */
    @GetMapping("/{id}")
    public Result detail(@PathVariable Long id) {
        SysAnalysisReport report = sysAnalysisReportService.getById(id);
        if (report == null || report.getStatus() == null || report.getStatus() != 1) {
            return Result.fail();
        }
        return Result.suc(report);
    }

    /**
     * 报告 HTML 预览
     * 例：GET /report/1/html
     */
    @GetMapping(value = "/{id}/html", produces = "text/html;charset=UTF-8")
    public String html(@PathVariable Long id) {
        SysAnalysisReport report = sysAnalysisReportService.getById(id);
        if (report == null || report.getStatus() == null || report.getStatus() != 1) {
            return "<h3>报告不存在或已删除</h3>";
        }
        return MarkdownUtils.wrapHtmlPage(report.getTitle(), MarkdownUtils.toHtml(report.getContent()));
    }

    /**
     * 报告下载：format=md 原始 Markdown；format=doc Word 可打开的文档（默认）
     * 例：GET /report/1/download?format=doc
     */
    @GetMapping("/{id}/download")
    public void download(@PathVariable Long id,
                         @RequestParam(defaultValue = "doc") String format,
                         HttpServletResponse response) throws IOException {
        SysAnalysisReport report = sysAnalysisReportService.getById(id);
        if (report == null || report.getStatus() == null || report.getStatus() != 1) {
            response.setStatus(404);
            response.getWriter().write("报告不存在或已删除");
            return;
        }
        String title = report.getTitle() == null ? "analysis_report" : report.getTitle();
        if ("md".equalsIgnoreCase(format)) {
            response.setContentType("text/markdown;charset=UTF-8");
            setAttachment(response, title + ".md");
            response.getWriter().write(report.getContent() == null ? "" : report.getContent());
        } else {
            // Word 兼容方案：HTML 内容 + application/msword，零第三方依赖
            response.setContentType("application/msword;charset=UTF-8");
            setAttachment(response, title + ".doc");
            response.getWriter().write(MarkdownUtils.wrapHtmlPage(title, MarkdownUtils.toHtml(report.getContent())));
        }
    }

    /**
     * 逻辑删除报告
     * 例：DELETE /report/1
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        SysAnalysisReport report = sysAnalysisReportService.getById(id);
        if (report == null || report.getStatus() == null || report.getStatus() != 1) {
            return Result.fail();
        }
        report.setStatus((short) 0);
        sysAnalysisReportService.updateById(report);
        return Result.suc();
    }

    private void setAttachment(HttpServletResponse response, String fileName) {
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
    }
}
