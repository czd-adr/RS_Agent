package com.xmut.shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xmut.shop.entity.SysAnalysisReport;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Bennu
 * @since 2026-11-14
 */
public interface SysAnalysisReportService extends IService<SysAnalysisReport> {

    /**
     * 查询会话下的正常状态报告列表（按创建时间倒序）
     */
    List<SysAnalysisReport> listByMemoryId(String memoryId);
}
