package com.xmut.shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xmut.shop.entity.SysAnalysisReport;
import com.xmut.shop.mapper.SysAnalysisReportMapper;
import com.xmut.shop.service.SysAnalysisReportService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Bennu
 * @since 2026-11-14
 */
@Service
public class SysAnalysisReportServiceImpl extends ServiceImpl<SysAnalysisReportMapper, SysAnalysisReport> implements SysAnalysisReportService {

    @Override
    public List<SysAnalysisReport> listByMemoryId(String memoryId) {
        return this.list(new LambdaQueryWrapper<SysAnalysisReport>()
                .eq(SysAnalysisReport::getStatus, (short) 1)
                .eq(memoryId != null && !memoryId.isBlank(), SysAnalysisReport::getMemoryId, memoryId)
                .orderByDesc(SysAnalysisReport::getCreatedAt));
    }
}
