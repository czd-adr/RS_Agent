package com.xmut.shop.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xmut.shop.DTO.WetlandPatchDTO;
import com.xmut.shop.entity.SysFileRecord;
import com.xmut.shop.enums.FileStatus;
import com.xmut.shop.mapper.SysFileRecordMapper;
import com.xmut.shop.mapper.WetlandPatchMapper;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.bind.annotation.*;

import java.awt.image.Raster;
import java.io.File;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/diff")
public class DifferenceController {

    private static final String BASE_PATH = "D:/Arc/difference";

    @Autowired
    private SysFileRecordMapper sysFileRecordMapper;

    @PostMapping("/NDVIByDate")
    public Map<String, Object> diffNDVI(@RequestParam String date1, @RequestParam String date2) {
        Map<String, Object> result = new HashMap<>();
        DecimalFormat df = new DecimalFormat("#.####");

        try {
            // 1. 动态获取两个时相的文件路径
            String path1 = getFilePath(date1, "NDVI");
            String path2 = getFilePath(date2, "NDVI");

            // 2. 状态校验：文件记录 status=1 才允许计算，否则抛出异常
            checkFileStatus(date1, "NDVI");
            checkFileStatus(date2, "NDVI");

            // 3. 计算各个时相的均值
            double mean1 = calculateRasterMean(new File(path1));
            double mean2 = calculateRasterMean(new File(path2));

            // 4. 封装结果
            result.put("date1", date1);
            result.put("mean1", Double.parseDouble(df.format(mean1)));
            result.put("date2", date2);
            result.put("mean2", Double.parseDouble(df.format(mean2)));
            result.put("delta", Double.parseDouble(df.format(mean2 - mean1))); // 计算差异值
            result.put("status", "success");

            System.out.println("分析完成: " + date1 + " vs " + date2);
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "error");
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 高效率计算全栅格均值
     */
    private double calculateRasterMean(File tifFile) throws Exception {
        if (!tifFile.exists()) throw new Exception("文件不存在: " + tifFile.getName());

        GeoTiffReader reader = new GeoTiffReader(tifFile);
        GridCoverage2D coverage = reader.read(null);
        Raster raster = coverage.getRenderedImage().getData();

        double sum = 0;
        long count = 0;

        int width = raster.getWidth();
        int height = raster.getHeight();
        int minX = raster.getMinX();
        int minY = raster.getMinY();

        // 直接通过数组获取像素，比 getPixel(x, y) 更快
        double[] pixelBuffer = new double[1];
        for (int y = minY; y < minY + height; y++) {
            for (int x = minX; x < minX + width; x++) {
                raster.getPixel(x, y, pixelBuffer);
                double val = pixelBuffer[0];

                // 剔除无效值
                if (!Double.isNaN(val) && val >= -1 && val <= 1) {
                    sum += val;
                    count++;
                }
            }
        }

        reader.dispose(); // 释放资源
        return count > 0 ? sum / count : Double.NaN;
    }

    /**
     * 文件名生成逻辑：2026-03 -> 26_3_NDVI.tif
     */
    private String getFileName(String dateStr, String band) {
        String[] parts = dateStr.split("-");
        String yearSuffix = parts[0].substring(parts[0].length() - 2);
        int month = Integer.parseInt(parts[1]); // 自动去掉 03 中的 0
        return String.format("%s_%d_%s.tif", yearSuffix, month, band);
    }

    /**
     * 获取完整文件路径
     */
    private String getFilePath(String dateStr, String band) {
        return Paths.get(BASE_PATH, getFileName(dateStr, band)).toString();
    }

    /**
     * 校验文件记录状态：只有 sys_file_record 中 status=1 的文件才允许参与计算，否则抛出异常
     */
    private void checkFileStatus(String dateStr, String band) {
        String fileName = getFileName(dateStr, band);
        SysFileRecord record = sysFileRecordMapper.selectOne(new LambdaQueryWrapper<SysFileRecord>()
                .eq(SysFileRecord::getFileName, fileName)
                .last("LIMIT 1"));
        if (record == null || record.getStatus() == null
                || record.getStatus() != FileStatus.NORMAL.getCode()) {
            throw new RuntimeException("文件 " + fileName + " 未注册或已被删除（status 不为 1），无法参与计算");
        }
    }
    @MockBean
    private WetlandPatchMapper wetlandPatchMapper;
    @PostMapping("/testMapeer")
    public  Boolean testMapeer (){
        String poiName = "青口渔场";
        String landType = "互花米草";
        int classType = 1;

        double sigma = 500.0;

        System.out.println("\n=========================================================");
        System.out.println("🚀 启动 [RS-Spatial-RAG] 真实数据库穿透全链路集成测试...");
        System.out.println("=========================================================");
        System.out.println("【当前运行时参数配置】");
        System.out.println(" -> 评估核心 POI 锚点: " + poiName);
        System.out.println(" -> 监测目标生态地物: " + landType);
        System.out.println(" -> 高斯衰减场带宽 (Sigma): " + sigma + " 米");
        System.out.println("---------------------------------------------------------");

        List<WetlandPatchDTO> dbPatches =
                wetlandPatchMapper.selectIntersectsPatches(
                        poiName,
                        classType
                );
        System.out.println("dbPatches = " + dbPatches);
        return true;
    }
}
