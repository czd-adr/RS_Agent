package com.xmut.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xmut.shop.entity.SysAnalysisReport;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author Bennu
 * @since 2026-11-14
 */
@Mapper
public interface SysAnalysisReportMapper extends BaseMapper<SysAnalysisReport> {

    /**
     * 自定义插入：params 字段为 jsonb，需显式 CAST 转换（MyBatis-Plus 默认按 varchar 传参会报类型不匹配）
     */
    @Insert("INSERT INTO sys_analysis_report (memory_id, title, report_type, content, params, status, created_at) " +
            "VALUES (#{memoryId}, #{title}, #{reportType}, #{content}, CAST(#{params} AS jsonb), #{status}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertReport(SysAnalysisReport report);
}
