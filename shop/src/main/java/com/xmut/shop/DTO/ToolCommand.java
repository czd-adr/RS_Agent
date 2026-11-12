package com.xmut.shop.DTO;

import lombok.Data;

import java.util.Map;

/**
 * <p>
 * 前端工具执行指令（结构化返回，供前端直接解析执行）
 * </p>
 *
 * @author Bennu
 * @since 2026-11-11
 */
@Data
public class ToolCommand {

    /** 前端工具编码，如 toggle_layer；无需执行工具时为空字符串 */
    private String toolCode;

    /** 工具参数，如 {"layerName":"628yangshi","visible":true} */
    private Map<String, Object> parameters;
}
