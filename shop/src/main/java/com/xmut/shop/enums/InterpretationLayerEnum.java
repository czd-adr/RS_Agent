package com.xmut.shop.enums;

import lombok.Getter;

/**
 * <p>
 * 遥感解译算法 -> 前端图层全称映射
 * 智能体只负责识别算法关键字，图层全称由本枚举确定性映射，避免大模型拼写错误
 * </p>
 *
 * @author Bennu
 * @since 2026-11-26
 */
@Getter
public enum InterpretationLayerEnum {

    /** 随机森林算法 -> test:RF_image 图层 */
    RANDOM_FOREST("RF", "test:RF_image",
            new String[]{"随机森林", "RF", "rf", "random forest"}),

    /** 集成算法（Stacking）-> test:Stacking 图层 */
    STACKING("STACKING", "test:Stacking",
            new String[]{"集成算法", "集成学习", "Stacking", "stacking"});

    /** 算法标准编码 */
    private final String algorithmCode;

    /** 前端图层全称（供 toggle_layer 的 layerName 参数使用） */
    private final String layerName;

    /** 可识别的关键字别名（大小写不敏感、包含匹配） */
    private final String[] aliases;

    InterpretationLayerEnum(String algorithmCode, String layerName, String[] aliases) {
        this.algorithmCode = algorithmCode;
        this.layerName = layerName;
        this.aliases = aliases;
    }

    /**
     * 按文本匹配解译算法，命中返回枚举，未命中返回 null
     */
    public static InterpretationLayerEnum match(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String lower = text.toLowerCase();
        for (InterpretationLayerEnum e : values()) {
            for (String alias : e.aliases) {
                if (lower.contains(alias.toLowerCase())) {
                    return e;
                }
            }
        }
        return null;
    }

    /**
     * 将 layerName 中的算法关键字映射为图层全称；未命中时原样返回（用户直接指定图层名的场景）
     */
    public static String mapLayerName(String layerName) {
        InterpretationLayerEnum e = match(layerName);
        return e == null ? layerName : e.getLayerName();
    }
}
