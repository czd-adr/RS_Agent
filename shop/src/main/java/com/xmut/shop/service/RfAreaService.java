package com.xmut.shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xmut.shop.entity.RfArea;
import com.xmut.shop.entity.SamplePoints;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Bennu
 * @since 2025-08-13
 */
public interface RfAreaService extends IService<RfArea> {
    List<RfArea> selectByType(int type);
    Double CalculateRfArea(int type);

    Double CalculateStkArea(int type);

    Double CalculateXgArea(int type);

    /**
     * Spatial-RAG 核心算子：
     * 联表空间查询，计算指定 POI 区域多边形内某种地物的相交面积总和
     * @param poiName 区域名称（如：青口河、临洪河口、青口渔场）
     * @param type 地物类型编码（class: 1-互花米草, 2-碱蓬, 3-芦苇...）
     * @return 局部相交区域的物理面积（平方米）
     */
    Double calculatePolygonIntersectsArea(String poiName, int type);
}
