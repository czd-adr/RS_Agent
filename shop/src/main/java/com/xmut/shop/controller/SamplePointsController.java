package com.xmut.shop.controller;


import com.xmut.shop.entity.SamplePoints;
import com.xmut.shop.service.SamplePointsService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverage.processing.Operations;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.factory.Hints;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;
import org.opengis.geometry.DirectPosition;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.awt.image.Raster;
import java.io.File;
import java.io.StringReader;
import java.text.DecimalFormat;
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
@Component
@RequestMapping("/sample-points")
public class SamplePointsController {
    @Autowired
    public  SamplePointsService samplePointsService;

    public List<Double> getListCache() { return orderedListCache.get(); }

    // 关键：在对话结束后清理内存
    public void clear() { orderedListCache.remove(); }
    private final ThreadLocal<List<Double>> orderedListCache = new ThreadLocal<>();
    @GetMapping("/list")
    public List<SamplePoints> list(){
        return samplePointsService.list();
    }
    @GetMapping("/id")
    public List<Integer> getIdByGrass(@RequestParam int grass) {
        return samplePointsService.getIdByGrass(grass);
    }
    @GetMapping("/geom")
    public List<SamplePoints> selectByGrass(@RequestParam int grass) {
        return samplePointsService.getPointByGrass(grass);
    }
    @GetMapping("/realPoint")
    public List<SamplePoints> selectRealPoint(@RequestParam int grass) {
        return samplePointsService.selectRealPoint(grass);
    }
    @PostMapping("/mean")
    public Map<String, Double> getIndexMeans(@RequestBody String geojson) {
        Map<String, Double> result = new HashMap<>();
        DecimalFormat df = new DecimalFormat("#.####");

        try {
            GeometryJSON gjson = new GeometryJSON();
            Geometry polygon = gjson.read(new StringReader(geojson));

            Map<String, String> tifPaths = new HashMap<>();
            tifPaths.put("NDVI", "D:/Arc/23NDVI_mean.tif");
            tifPaths.put("NDWI", "D:/Arc/23_NDWI_mean.tif");
            tifPaths.put("LSWI", "D:/Arc/LSWI_mean.tif");

            for (Map.Entry<String, String> entry : tifPaths.entrySet()) {
                String indexName = entry.getKey();
                File tiffFile = new File(entry.getValue());

                GeoTiffReader reader = new GeoTiffReader(tiffFile);
                GridCoverage2D coverage = reader.read(null);
                GridGeometry2D gridGeom = coverage.getGridGeometry();
                Raster raster = coverage.getRenderedImage().getData();

                MathTransform transform = CRS.findMathTransform(DefaultGeographicCRS.WGS84, coverage.getCoordinateReferenceSystem(), true);
                Geometry transformedPolygon = JTS.transform(polygon, transform);

                List<Double> values = new ArrayList<>();
                int minX = raster.getMinX();
                int minY = raster.getMinY();
                int width = raster.getWidth();
                int height = raster.getHeight();

                for (int y = minY; y < minY + height; y++) {
                    for (int x = minX; x < minX + width; x++) {
                        GridCoordinates2D gridCoord = new GridCoordinates2D(x, y);
                        DirectPosition worldPos = gridGeom.gridToWorld(gridCoord);
                        Coordinate coord = new Coordinate(worldPos.getOrdinate(0), worldPos.getOrdinate(1));
                        Point point = new GeometryFactory().createPoint(coord);

                        if (transformedPolygon.contains(point)) {
                            double[] pixel = new double[1];
                            raster.getPixel(x, y, pixel);
                            double value = pixel[0];
                            if (!Double.isNaN(value) && value >= -1 && value <= 1) {
                                values.add(value);
                            }
                        }
                    }
                }

                double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
                result.put(indexName, Double.parseDouble(df.format(mean)));
            }

            // RGB 处理
            File rgbTiffFile = new File("D:/Arc/realDT.tif");
            if (rgbTiffFile.exists()) {
                GeoTiffReader rgbReader = new GeoTiffReader(rgbTiffFile);
                GridCoverage2D rgbCoverage = rgbReader.read(null);
                GridGeometry2D gridGeom = rgbCoverage.getGridGeometry();
                Raster raster = rgbCoverage.getRenderedImage().getData();

                MathTransform transform = CRS.findMathTransform(DefaultGeographicCRS.WGS84, rgbCoverage.getCoordinateReferenceSystem(), true);
                Geometry transformedPolygon = JTS.transform(polygon, transform);

                List<Double> reds = new ArrayList<>();
                List<Double> greens = new ArrayList<>();
                List<Double> blues = new ArrayList<>();

                int minX = raster.getMinX();
                int minY = raster.getMinY();
                int width = raster.getWidth();
                int height = raster.getHeight();

                for (int y = minY; y < minY + height; y++) {
                    for (int x = minX; x < minX + width; x++) {
                        GridCoordinates2D gridCoord = new GridCoordinates2D(x, y);
                        DirectPosition worldPos = gridGeom.gridToWorld(gridCoord);
                        Coordinate coord = new Coordinate(worldPos.getOrdinate(0), worldPos.getOrdinate(1));
                        Point point = new GeometryFactory().createPoint(coord);

                        if (transformedPolygon.contains(point)) {
                            double[] pixel = new double[3];
                            raster.getPixel(x, y, pixel);
                            reds.add(pixel[0]);
                            greens.add(pixel[1]);
                            blues.add(pixel[2]);
                        }
                    }
                }

                result.put("R", Double.parseDouble(df.format(reds.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN))));
                result.put("G", Double.parseDouble(df.format(greens.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN))));
                result.put("B", Double.parseDouble(df.format(blues.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN))));
            }

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @PostMapping("/means")
    public Map<String, Double> getIndexMean(@RequestBody String geojson) {
        Map<String, Double> result = new HashMap<>();

        try {
            // 1. 设置全局 Hints，强制指定坐标系为 EPSG:4326，解决 EngineeringCRS 报错问题
            Hints hints = new Hints();
            try {
                // 确保导入了 org.geotools.referencing.CRS
                hints.put(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, CRS.decode("EPSG:4326"));
                hints.put(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
            } catch (Exception e) {
                System.err.println("坐标参考系统初始化失败: " + e.getMessage());
            }

            // 2. 解析前端传入的 GeoJSON
            GeometryJSON gjson = new GeometryJSON();
            Geometry polygon = gjson.read(new StringReader(geojson));

            // 3. 处理 NDVI、NDWI、LSWI 三个单波段 TIF
            Map<String, String> tifPaths = new HashMap<>();
            tifPaths.put("NDVI", "D:/Arc/23NDVI_mean.tif");
            tifPaths.put("NDWI", "D:/Arc/23_NDWI_mean.tif");
            tifPaths.put("LSWI", "D:/Arc/LSWI_mean.tif");

            for (Map.Entry<String, String> entry : tifPaths.entrySet()) {
                String indexName = entry.getKey();
                String path = entry.getValue();

                File tiffFile = new File(path);
                if (!tiffFile.exists()) continue;

                // 使用 hints 初始化 Reader
                GeoTiffReader reader = new GeoTiffReader(tiffFile, hints);
                GridCoverage2D coverage = reader.read(null);

                // 裁剪影像
                ReferencedEnvelope bbox = new ReferencedEnvelope(polygon.getEnvelopeInternal(), coverage.getCoordinateReferenceSystem());
                GridCoverage2D subCoverage = (GridCoverage2D) Operations.DEFAULT.crop(coverage, bbox);
                Raster raster = subCoverage.getRenderedImage().getData();

                int minX = raster.getMinX();
                int minY = raster.getMinY();
                int width = raster.getWidth();
                int height = raster.getHeight();

                List<Double> values = new ArrayList<>();
                for (int y = minY; y < minY + height; y++) {
                    for (int x = minX; x < minX + width; x++) {
                        double[] pixel = new double[1];
                        raster.getPixel(x, y, pixel);
                        double value = pixel[0];

                        // 过滤无效值（常见 TIF 无效值为 -3.4028...E38 或 NaN）
                        if (!Double.isNaN(value) && value >= -1 && value <= 1) {
                            values.add(value);
                        }
                    }
                }

                double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
                result.put(indexName, mean);
                // 释放资源
                reader.dispose();
            }

            // 4. 处理 RGB 三个波段
            File rgbTiffFile = new File("D:/Arc/realDT.tif");
            if (rgbTiffFile.exists()) {
                GeoTiffReader rgbReader = new GeoTiffReader(rgbTiffFile, hints);
                GridCoverage2D rgbCoverage = rgbReader.read(null);

                ReferencedEnvelope rgbBox = new ReferencedEnvelope(polygon.getEnvelopeInternal(), rgbCoverage.getCoordinateReferenceSystem());
                GridCoverage2D rgbSubCoverage = (GridCoverage2D) Operations.DEFAULT.crop(rgbCoverage, rgbBox);
                Raster rgbRaster = rgbSubCoverage.getRenderedImage().getData();

                int rMinX = rgbRaster.getMinX();
                int rMinY = rgbRaster.getMinY();
                int rWidth = rgbRaster.getWidth();
                int rHeight = rgbRaster.getHeight();

                List<Double> reds = new ArrayList<>();
                List<Double> greens = new ArrayList<>();
                List<Double> blues = new ArrayList<>();

                for (int y = rMinY; y < rMinY + rHeight; y++) {
                    for (int x = rMinX; x < rMinX + rWidth; x++) {
                        double[] pixel = new double[3];
                        rgbRaster.getPixel(x, y, pixel);
                        reds.add(pixel[0]);
                        greens.add(pixel[1]);
                        blues.add(pixel[2]);
                    }
                }

                result.put("R", reds.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN));
                result.put("G", greens.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN));
                result.put("B", blues.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN));
                rgbReader.dispose();
            }

            // 5. 格式化输出结果，保留四位小数
            DecimalFormat df = new DecimalFormat("#.####");
            Map<String, Double> formattedResult = new HashMap<>();
            for (Map.Entry<String, Double> entry : result.entrySet()) {
                Double val = entry.getValue();
                if (val != null && !val.isNaN()) {
                    formattedResult.put(entry.getKey(), Double.valueOf(df.format(val)));
                } else {
                    formattedResult.put(entry.getKey(), val);
                }
            }
            return formattedResult;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    @PostMapping("/meanMask")
        public Map<String, Double> meanMask(@RequestBody String geojson) {
        Map<String, Double> result = new HashMap<>();

        try {
            // 1. 解析 GeoJSON
            GeometryJSON gjson = new GeometryJSON();
            Geometry polygon = gjson.read(new StringReader(geojson));

            // 2. NDVI / NDWI / LSWI 波段处理
            Map<String, String> tifPaths = new HashMap<>();
            tifPaths.put("NDVI", "D:/Arc/23NDVI_mean.tif");
            tifPaths.put("NDWI", "D:/Arc/23_NDWI_mean.tif");
            tifPaths.put("LSWI", "D:/Arc/LSWI_mean.tif");

            CoverageProcessor processor = null;
            for (Map.Entry<String, String> entry : tifPaths.entrySet()) {
                String indexName = entry.getKey();
                String path = entry.getValue();

                File tiffFile = new File(path);
                GeoTiffReader reader = new GeoTiffReader(tiffFile);
                GridCoverage2D coverage = reader.read(null);

                CoordinateReferenceSystem targetCRS = coverage.getCoordinateReferenceSystem();
                MathTransform transform = CRS.findMathTransform(DefaultGeographicCRS.WGS84, targetCRS);
                Geometry transformedPolygon = JTS.transform(polygon, transform);

                // 裁剪操作（掩膜）
                processor = CoverageProcessor.getInstance();
                ParameterValueGroup params = processor.getOperation("CoverageCrop").getParameters();
                params.parameter("Source").setValue(coverage);
                params.parameter("ROI").setValue(transformedPolygon);

                GridCoverage2D cropped = (GridCoverage2D) processor.doOperation(params);
                Raster raster = cropped.getRenderedImage().getData();

                List<Double> values = new ArrayList<>();
                int minX = raster.getMinX();
                int minY = raster.getMinY();
                int width = raster.getWidth();
                int height = raster.getHeight();

                for (int y = minY; y < minY + height; y++) {
                    for (int x = minX; x < minX + width; x++) {
                        double[] pixel = new double[1];
                        raster.getPixel(x, y, pixel);
                        double value = pixel[0];
                        if (!Double.isNaN(value) && value >= -1 && value <= 1) {
                            values.add(value);
                        }
                    }
                }

                double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
                result.put(indexName, mean);
            }

            // 3. RGB 图像处理（掩膜 + 三通道分开统计）
            GeoTiffReader rgbReader = new GeoTiffReader(new File("D:/Arc/realDT.tif"));
            GridCoverage2D rgbCoverage = rgbReader.read(null);

            CoordinateReferenceSystem rgbCRS = rgbCoverage.getCoordinateReferenceSystem();
            MathTransform transformToRGB = CRS.findMathTransform(DefaultGeographicCRS.WGS84, rgbCRS, true);
            Geometry polygonForRGB = JTS.transform(polygon, transformToRGB);

// 掩膜裁剪
            ParameterValueGroup rgbParams = processor.getOperation("CoverageCrop").getParameters();
            rgbParams.parameter("Source").setValue(rgbCoverage);
            rgbParams.parameter("ROI").setValue(polygonForRGB);

            GridCoverage2D rgbCropped = (GridCoverage2D) processor.doOperation(rgbParams);
            Raster rgbRaster = rgbCropped.getRenderedImage().getData();

            int minX = rgbRaster.getMinX();
            int minY = rgbRaster.getMinY();
            int width = rgbRaster.getWidth();
            int height = rgbRaster.getHeight();

            List<Double> reds = new ArrayList<>();
            List<Double> greens = new ArrayList<>();
            List<Double> blues = new ArrayList<>();

            for (int y = minY; y < minY + height; y++) {
                for (int x = minX; x < minX + width; x++) {
                    int[] pixel = new int[3]; // RGB 通道用 int 更安全
                    rgbRaster.getPixel(x, y, pixel);

                    if (!(pixel[0] == 0 && pixel[1] == 0 && pixel[2] == 0)) {
                        reds.add((double) pixel[0]);
                        greens.add((double) pixel[1]);
                        blues.add((double) pixel[2]);
                    }
                }
            }

            result.put("R", reds.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN));
            result.put("G", greens.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN));
            result.put("B", blues.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN));

            // 4. 小数保留四位
            DecimalFormat df = new DecimalFormat("#.####");
            Map<String, Double> formattedResult = new HashMap<>();
            for (Map.Entry<String, Double> entry : result.entrySet()) {
                Double val = entry.getValue();
                if (val != null && !val.isNaN()) {
                    formattedResult.put(entry.getKey(), Double.parseDouble(df.format(val)));
                } else {
                    formattedResult.put(entry.getKey(), val);
                }
            }

            return formattedResult;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    @PostMapping("/timeNDVI")
    public Double getTimeNDVI(@RequestParam int grass) {
        try {
            // 1. NDVI 文件路径（这里假设你使用的是 2023 年 1 月 NDVI 文件）
            File ndviFile = new File("D:/Arc/23NDVI_mean.tif");

            // 2. 初始化 GeoTiff 读取器
            GeoTiffReader reader = new GeoTiffReader(ndviFile, new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE));
            GridCoverage2D coverage = reader.read(null);
            GridGeometry2D gridGeometry = coverage.getGridGeometry();

            // 3. 获取植被类型对应的采样点
            List<SamplePoints> points = samplePointsService.selectRealPoint(grass); // 返回 JTS 的 Point 对象

            // 4. 遍历所有点并获取 NDVI 值
            List<Double> ndviValues = new ArrayList<>();
            for (SamplePoints point : points) {
                Coordinate coord = point.getCoord(); // 获取经纬度
                DirectPosition2D posWorld = new DirectPosition2D(coord.x, coord.y); // 经纬度位置
                GridCoordinates2D gridCoord = gridGeometry.worldToGrid(posWorld); // 转为栅格行列

                Raster raster = coverage.getRenderedImage().getData();
                double[] pixel = new double[1];
                raster.getPixel(gridCoord.x, gridCoord.y, pixel);
                double ndvi = pixel[0];

                if (!Double.isNaN(ndvi) && ndvi >= -1 && ndvi <= 1) {
                    ndviValues.add(ndvi);
                }
            }

            // 5. 计算均值
            double meanNDVI = ndviValues.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
            System.out.println("NDVI 均值：" + meanNDVI);
            return meanNDVI;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @PostMapping("/monthNDVIs")
    @Tool("查询特定植被类型在指定年份的逐月平均 NDVI（植被覆盖指数）数据。" +
            "当你需要分析某种植物全年的生长趋势、长势变化或季节性规律时，请调用此工具。" +
            "返回结果是一个 Map，包含 12 个月的月份缩写及其对应的 NDVI 均值（-1.0 到 1.0 之间）。")
    public Map<String, Double> getMonthlyNDVIs(
            @P("grass: 植被类型的分类编号。1: 互花米草, 2: 碱蓬 , 3: 芦苇, 4: 其他。") @RequestParam int grass,
            @P("查询的完整年份，例如 2022 或 2023。") @RequestParam int year
    ) {
        Map<String, Double> result = new HashMap<>();
        String yearSuffix = String.valueOf(year).substring(2);
        try {
            // 1. 获取样点 WKT 字符串
            List<SamplePoints> points = samplePointsService.getPointByGrass(grass);
            if (points.isEmpty()) return result;

            // 2. 构建坐标列表
            WKTReader wktReader = new WKTReader();
            List<Coordinate> coords = new ArrayList<>();
            for (SamplePoints sp : points) {
                if (sp.getGeom() == null || sp.getGeom().isEmpty()) continue;
                Geometry geom = wktReader.read(sp.getGeom());
                if (!(geom instanceof Point)) continue;
                coords.add(geom.getCoordinate());
            }
            if (coords.isEmpty()) return result;

            // 3. 月份缩写列表
            String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

            for (String month : months) {
                String filePath = "D:/Arc/" + yearSuffix + "NDVIset/set/" + month + ".tif";
                File ndviFile = new File(filePath);
                if (!ndviFile.exists()) {
                    result.put(month, Double.NaN);
                    continue;
                }

                GeoTiffReader reader = new GeoTiffReader(ndviFile, new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE));
                GridCoverage2D coverage = reader.read(null);
                Raster raster = coverage.getRenderedImage().getData();

                List<Double> values = new ArrayList<>();
                for (Coordinate coord : coords) {
                    try {
                        DirectPosition2D pos = new DirectPosition2D(coord.x, coord.y);
                        GridCoordinates2D gridCoord = coverage.getGridGeometry().worldToGrid(pos);
                        double[] pixel = new double[1];
                        raster.getPixel(gridCoord.x, gridCoord.y, pixel);
                        double value = pixel[0];
                        if (!Double.isNaN(value) && value >= -1 && value <= 1) {
                            values.add(value);
                        }
                    } catch (Exception ex) {
                        // 某点转换失败跳过
                        continue;
                    }
                }

                double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
                result.put(month, mean);
            }

            List<Double> orderedList = new ArrayList<>();
            for (String m : months) {
                // 严格按照 months 数组的顺序取值，确保数组索引与月份对应
                // 如果某月没数据，取 0.0 或 Double.NaN
                orderedList.add(result.getOrDefault(m, 0.0));
            }

            // 3. 【存入缓存】：供 Controller 的 Flux onComplete 使用
            orderedListCache.set(orderedList);
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }





}
