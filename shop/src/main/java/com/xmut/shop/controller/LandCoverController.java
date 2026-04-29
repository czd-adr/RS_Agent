package com.xmut.shop.controller;


import com.xmut.shop.common.Result;
import com.xmut.shop.entity.LandCover;
import com.xmut.shop.entity.RfArea;
import com.xmut.shop.service.LandCoverService;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
@RequestMapping("/LC")
public class LandCoverController {
    @Autowired
    public LandCoverService landCoverService;
    @PostMapping("/areaStats")
    public List<LandCover> getLandCoverStats(@RequestBody String geojson,@Param("algorithm") String algorithm) {
        return landCoverService.getLandCoverStats(geojson,algorithm);
    }
}
