package com.xmut.shop.DTO;

import lombok.Data;

import java.util.List;
@Data
public class NDVIChartDTO {
    private String type = "CHART_NDVI";
    private String plantName;
    private String color;
    private String year;
    private List<Double> data; // 12个月的数值，NaN 传 null

    // 省略 Getter/Setter 和构造函数
}
