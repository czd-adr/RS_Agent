package com.xmut.shop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 分析报告表 sys_analysis_report
 * </p>
 *
 * @author Bennu
 * @since 2026-11-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_analysis_report")
public class SysAnalysisReport implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 归属会话 ID */
    private String memoryId;

    /** 报告标题 */
    private String title;

    /** 报告类型：monthly_compare / monthly_trend */
    private String reportType;

    /** Markdown 全文 */
    private String content;

    /** 本次分析的参数快照（JSONB，字符串形式） */
    private String params;

    /** 状态：1-正常，0-已删除 */
    private Short status;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
