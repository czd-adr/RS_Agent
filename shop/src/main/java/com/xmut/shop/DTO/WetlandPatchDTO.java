package com.xmut.shop.DTO;

import lombok.Data;

import java.io.Serializable;

@Data
public class WetlandPatchDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    // 1. 数据库直接映射字段
    private Long patchId;          // 遥感图斑唯一标识 (f.id)
    private Double patchArea;      // 图斑原始物理面积，单位：平方米 (f.area_m2)
    private Double spatialDistance;// 图斑几何中心或边界到指定POI的空间距离，单位：米

    // 2. 空间RAG运行时动态计算打分字段（不映射数据库）
    private Double spatialScore;   // 空间相关性得分 f_s (0.0 ~ 1.0)
    private Double semanticScore;  // 语义相关性得分 f_k (0.0 ~ 1.0)
    private Double jointScore;     // 双流融合综合得分 F(i) (0.0 ~ 1.0)
}
