package com.xmut.shop.controller;


import com.xmut.shop.DTO.RegionalSpatialParam;
import com.xmut.shop.entity.RfArea;
import com.xmut.shop.entity.SamplePoints;
import com.xmut.shop.service.RfAreaService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.xmut.shop.common.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author Bennu
 * @since 2025-08-13
 */
@RestController
@RequestMapping("/RF")
public class RfAreaController {
    @Autowired
    public  RfAreaService rfareaService;
    @GetMapping("/geom")
    public List<RfArea> selectByType(@RequestParam int type) {
        return rfareaService.selectByType(type);
    }

    @GetMapping("/Area")
    public Result getArea(@RequestParam int type) {
        Double area = rfareaService.CalculateRfArea(type);
        if (area != null) {
            return Result.suc(area); // 将 Double 作为 data 返回
        } else {
            return Result.fail();
        }
    }
    @GetMapping("/getRfAllArea")
    public Result getAllArea() {
        // 用于存储五个面积值的 map，key 为地物名称
        Map<String, Double> areaMap = new HashMap<>();

        for (int i = 1; i <= 5; i++) {
            Double areaClass = rfareaService.CalculateRfArea(i); // 获取面积
            String landType = ""; // 地物类型名称

            switch (i) {
                case 1:
                    landType = "互花米草";
                    break;
                case 2:
                    landType = "碱蓬";
                    break;
                case 3:
                    landType = "芦苇";
                    break;
                case 4:
                    landType = "水体";
                    break;
                case 5:
                    landType = "其他";
                    break;
                default:
                    landType = "未知";
            }

            areaMap.put(landType, areaClass); // 地物名作为 key
        }

        return Result.suc(areaMap);
    }

    @GetMapping("/getStkAllArea")
    @Tool("""
查询当前湿地各地物类型的面积信息。

返回：
互花米草、碱蓬、芦苇、水体、其他
对应的面积数据。

当用户询问：
- 各植被面积
- 湿地面积分布
- 哪种植被最多
- 面积占比
- 湿地现状
- 各类地物情况
时，应调用此工具。
""")
    public Result getStkAllArea() {
        // 用于存储五个面积值的 map，key 为地物名称
        Map<String, Double> areaMap = new HashMap<>();

        for (int i = 1; i <= 5; i++) {
            Double areaClass = rfareaService.CalculateStkArea(i); // 获取面积
            String landType = ""; // 地物类型名称

            switch (i) {
                case 1:
                    landType = "互花米草";
                    break;
                case 2:
                    landType = "碱蓬";
                    break;
                case 3:
                    landType = "芦苇";
                    break;
                case 4:
                    landType = "水体";
                    break;
                case 5:
                    landType = "其他";
                    break;
                default:
                    landType = "未知";
            }

            areaMap.put(landType, areaClass); // 地物名作为 key
        }

        return Result.suc(areaMap);
    }

    @GetMapping("/getXgAllArea")
    public Result getXgAllArea() {
        // 用于存储五个面积值的 map，key 为地物名称
        Map<String, Double> areaMap = new HashMap<>();

        for (int i = 1; i <= 5; i++) {
            Double areaClass = rfareaService.CalculateXgArea(i); // 获取面积
            String landType = ""; // 地物类型名称

            switch (i) {
                case 1:
                    landType = "互花米草";
                    break;
                case 2:
                    landType = "碱蓬";
                    break;
                case 3:
                    landType = "芦苇";
                    break;
                case 4:
                    landType = "水体";
                    break;
                case 5:
                    landType = "其他";
                    break;
                default:
                    landType = "未知";
            }

            areaMap.put(landType, areaClass); // 地物名作为 key
        }

        return Result.suc(areaMap);
    }



    @Tool("""
    空间RAG专用算子：根据用户指定的地理空间区域（青口河、青口渔场、临洪河口）和地物类型（互花米草、碱蓬、芦苇、水体），
    驱动后端 PostGIS 执行多边形拓扑相交分析（ST_Intersects），实时统计该局部边界内的地物覆盖面积（单位：公顷）。
    """)
    public String calculateRegionalLandArea(RegionalSpatialParam param) {
        // 1. 文字地物转为你数据库原本的编号系统
        int classType = 5;
        switch (param.getLandType()) {
            case "互花米草": classType = 1; break;
            case "碱蓬":     classType = 2; break;
            case "芦苇":     classType = 3; break;
            case "水体":     classType = 4; break;
        }

        // 2. 动态跑你底层的多边形叠加 SQL：
        // SELECT SUM(area_m2) FROM "RF_area_export" WHERE "class" = #{classType} AND ST_Intersects(geom, (SELECT geom FROM "spatial_poi" WHERE "poi_name" = #{poiName}))
        Double areaM2 = rfareaService.calculatePolygonIntersectsArea(param.getPoiName(), classType);

        if (areaM2 == null || areaM2 == 0) {
            return String.format("【空间计算结果】在【%s】区域内，暂未检测到【%s】的空间几何分布。",
                    param.getPoiName(), param.getLandType());
        }

        // 3. 平方米转换为公顷（hm²）
        double areaHm2 = areaM2 / 10000.0;

        // 4. 返回标准文本给大模型，模型会根据你的【规则6】继续做深度的生态解释
        return String.format("经PostGIS空间叠加分析，在【%s】监测多边形边界内部，目标地物【%s】的当前实际覆盖面积为【%.2f】公顷。",
                param.getPoiName(), param.getLandType(), areaHm2);
    }
}
