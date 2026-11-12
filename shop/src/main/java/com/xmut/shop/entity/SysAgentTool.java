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
 * RS-AGENT 智能体动态工具注册表 sys_agent_tool
 * </p>
 *
 * @author Bennu
 * @since 2026-11-02
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_agent_tool")
public class SysAgentTool implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 工具唯一编码 (对应前端/后端执行函数名) */
    private String toolCode;

    /** 工具中文名称 */
    private String toolName;

    /** 工具分类：GIS_CLIENT(前端工具), ALGORITHM(后端算法), RAG(知识库) */
    private String category;

    /** 大模型意图匹配描述 */
    private String description;

    /** 异构参数的 JSON Schema 结构定义（JSONB，字符串形式存储） */
    private String parametersSchema;

    /** 状态：1-启用，0-禁用 */
    private Short isEnabled;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
