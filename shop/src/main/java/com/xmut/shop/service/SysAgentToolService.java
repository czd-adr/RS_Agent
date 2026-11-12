package com.xmut.shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xmut.shop.entity.SysAgentTool;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Bennu
 * @since 2026-11-02
 */
public interface SysAgentToolService extends IService<SysAgentTool> {

    /**
     * 查询启用的工具列表，可按分类过滤
     */
    List<SysAgentTool> listEnabledTools(String category);

    /**
     * 按工具编码查询单个工具
     */
    SysAgentTool getByToolCode(String toolCode);
}
