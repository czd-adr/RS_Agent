package com.xmut.shop.controller;

import com.xmut.shop.common.Result;
import com.xmut.shop.repository.ActiveLayerStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 活动图层接口：管理每个会话当前地图上显示的活动图层（Redis 存储，2 小时过期）
 */
@RestController
@RequestMapping("/active-layer")
public class ActiveLayerController {

    @Autowired
    private ActiveLayerStore activeLayerStore;

    /**
     * 活动图层列表
     * 例：GET /active-layer/list?memoryId=xx
     */
    @GetMapping("/list")
    public Result list(@RequestParam String memoryId) {
        if (memoryId == null || memoryId.isBlank()) {
            return Result.fail();
        }
        List<String> layers = activeLayerStore.list(memoryId);
        return Result.suc(layers, (long) layers.size());
    }

    /**
     * 新增活动图层
     * 例：POST /active-layer/add?memoryId=xx&layerName=628yangshi
     */
    @PostMapping("/add")
    public Result add(@RequestParam String memoryId, @RequestParam String layerName) {
        if (memoryId == null || memoryId.isBlank() || layerName == null || layerName.isBlank()) {
            return Result.fail();
        }
        activeLayerStore.add(memoryId, layerName);
        // 返回最新列表，前端可直接刷新状态
        List<String> layers = activeLayerStore.list(memoryId);
        return Result.suc(layers, (long) layers.size());
    }

    /**
     * 删除活动图层
     * 例：DELETE /active-layer/remove?memoryId=xx&layerName=628yangshi
     */
    @DeleteMapping("/remove")
    public Result remove(@RequestParam String memoryId, @RequestParam String layerName) {
        if (memoryId == null || memoryId.isBlank() || layerName == null || layerName.isBlank()) {
            return Result.fail();
        }
        activeLayerStore.remove(memoryId, layerName);
        List<String> layers = activeLayerStore.list(memoryId);
        return Result.suc(layers, (long) layers.size());
    }
}
