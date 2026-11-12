package com.xmut.shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xmut.shop.entity.SysAgentTool;
import com.xmut.shop.mapper.SysAgentToolMapper;
import com.xmut.shop.service.SysAgentToolService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Bennu
 * @since 2026-11-02
 */
@Service
public class SysAgentToolServiceImpl extends ServiceImpl<SysAgentToolMapper, SysAgentTool> implements SysAgentToolService {

    @Override
    public List<SysAgentTool> listEnabledTools(String category) {
        return this.list(new LambdaQueryWrapper<SysAgentTool>()
                .eq(SysAgentTool::getIsEnabled, (short) 1)
                .eq(category != null && !category.isBlank(), SysAgentTool::getCategory, category)
                .orderByAsc(SysAgentTool::getId));
    }

    @Override
    public SysAgentTool getByToolCode(String toolCode) {
        return this.getOne(new LambdaQueryWrapper<SysAgentTool>()
                .eq(SysAgentTool::getToolCode, toolCode)
                .last("LIMIT 1"));
    }
}
