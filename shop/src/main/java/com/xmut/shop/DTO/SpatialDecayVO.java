package com.xmut.shop.DTO;

import lombok.Data;

@Data
public class SpatialDecayVO {

    private Long patchId;

    /**
     * 距离(m)
     */
    private Double spatialDistance;

    /**
     * 原始面积(m²)
     */
    private Double originalArea;

    /**
     * 原始面积(hm²)
     */
    private Double originalAreaHm;

    /**
     * 高斯权重
     */
    private Double gaussianWeight;

    /**
     * 加权面积(hm²)
     */
    private Double weightedAreaHm;
}
