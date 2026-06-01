package com.xmut.shop.controller;

import com.xmut.shop.DTO.WetlandPatchDTO;
import com.xmut.shop.mapper.WetlandPatchMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SpatialDebugController {

    @Autowired
    private WetlandPatchMapper wetlandPatchMapper;

    /**
     * 测试 Mapper 是否真正访问 PostGIS
     *
     * 示例:
     * http://localhost:8099/debug/spatial?poiName=临洪河口&type=1
     */
    @GetMapping("/debug/spatial")
    public List<WetlandPatchDTO> spatialDebug(
            @RequestParam String poiName,
            @RequestParam Integer type) {

        System.out.println("================================");
        System.out.println("POI = " + poiName);
        System.out.println("TYPE = " + type);

        List<WetlandPatchDTO> result =
                wetlandPatchMapper.selectIntersectsPatches(
                        poiName,
                        type
                );

        System.out.println("RESULT SIZE = " + result.size());

        for (WetlandPatchDTO dto : result) {
            System.out.println(dto);
        }

        System.out.println("================================");

        return result;
    }
}