package com.xmut.shop.service;

public interface WetlandAgentService {
    /**
     * 驱动Spatial-RAG双流协同计算，获取指定区域内目标地物的量化评估报告
     */
    String executeSpatialSemanticAnalysis(String poiName, String landType);
}