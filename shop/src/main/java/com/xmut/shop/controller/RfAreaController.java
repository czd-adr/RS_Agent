package com.xmut.shop.controller;


import com.xmut.shop.entity.RfArea;
import com.xmut.shop.entity.SamplePoints;
import com.xmut.shop.service.RfAreaService;
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
}
