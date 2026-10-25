package com.xmut.shop.controller;

import com.xmut.shop.DTO.SpatialDecayVO;
import com.xmut.shop.DTO.WetlandPatchDTO;
import com.xmut.shop.Utils.GaussianDecayUtils;
import com.xmut.shop.entity.SamplePoints;
import com.xmut.shop.mapper.WetlandPatchMapper;
import com.xmut.shop.service.RfAreaService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.xmut.shop.common.Result;
import com.xmut.shop.service.SamplePointsService;
import dev.langchain4j.agent.tool.P;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.util.factory.Hints;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;

import java.awt.image.Raster;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class WebGisEnglishToolsController {

    @Autowired
    public  RfAreaService rfareaService;

    @Autowired
    public  SamplePointsService samplePointsService;

    @Autowired
    private WetlandPatchMapper wetlandPatchMapper;
    public List<Double> getListCache() { return orderedListCache.get(); }

    // 关键：在对话结束后清理内存
    public void clear() { orderedListCache.remove(); }
    private final ThreadLocal<List<Double>> orderedListCache = new ThreadLocal<>();
    @GetMapping("/getStkAllArea")
    @Tool("""
Query the macro-scale geodetic surface area configuration for all primary wetland land cover categories.

Returns:
A Map containing geometric area statistics for '互花米草' (Spartina alterniflora), '碱蓬' (Suaeda salsa), 
'芦苇' (Phragmites australis), '水体' (Water Body), and '其他' (Others).

Invoke this tool strictly when the user inquiries about:
- Area statistics / Quantitative coverage / Spatial extent
- Wetland distribution patterns / Landscape proportions
- Dominant vegetation community in the current ecosystem
- Present state or general census of specific coastal land covers
""")
    public Result getStkAllArea() {
        Map<String, Double> areaMap = new HashMap<>();

        for (int i = 1; i <= 5; i++) {
            // 调用原有计算逻辑，i 映射：1-米草, 2-碱蓬, 3-芦苇, 4-水体, 5-其他
            Double areaClass = rfareaService.CalculateStkArea(i);
            String landType = "";

            switch (i) {
                case 1: landType = "互花米草"; break;
                case 2: landType = "碱蓬"; break;
                case 3: landType = "芦苇"; break;
                case 4: landType = "水体"; break;
                case 5: landType = "其他"; break;
                default: landType = "未知";
            }
            areaMap.put(landType, areaClass);
        }
        return Result.suc(areaMap);
    }

    /**
     * 工具二：逐月平均 NDVI 趋势查询
     * 英文描述高度对齐 Agent Rules 中的 NDVI change patterns / growth trajectory 意图触发词
     */
    @PostMapping("/monthNDVIs")
    @Tool("""
Query the historical monthly mean Normalized Difference Vegetation Index (NDVI) telemetry data 
for a specific vegetation classification within a designated full calendar year.

Invoke this tool strictly when the user requests to:
- Analyze temporal NDVI change patterns / Phenological variations
- Investigate the annual growth trajectory or seasonal lifecycle trends of specific plants
- Inspect the dense remote sensing vegetation index history (-1.0 to 1.0) of a target community

Returns:
A chronological Map containing 12 monthly abbreviated keys (Jan to Dec) mapped to their respective mean NDVI values.
""")
    public Map<String, Double> getMonthlyNDVIs(
            @P("The standardized internal classification index for the target vegetation flora. " +
                    "Must map as follows: 1 for 'Spartina alterniflora', 2 for 'Suaeda salsa', 3 for 'Phragmites australis', 4 for 'Others'.")
            @RequestParam int grass,

            @P("The four-digit calendar year for historical telemetry analysis, e.g., 2022 or 2023.")
            @RequestParam int year
    ) {
        Map<String, Double> result = new HashMap<>();
        String yearSuffix = String.valueOf(year).substring(2);
        try {
            List<SamplePoints> points = samplePointsService.getPointByGrass(grass);
            if (points.isEmpty()) return result;

            WKTReader wktReader = new WKTReader();
            List<Coordinate> coords = new ArrayList<>();
            for (SamplePoints sp : points) {
                if (sp.getGeom() == null || sp.getGeom().isEmpty()) continue;
                Geometry geom = wktReader.read(sp.getGeom());
                if (!(geom instanceof Point)) continue;
                coords.add(geom.getCoordinate());
            }
            if (coords.isEmpty()) return result;

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
                        continue;
                    }
                }

                double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
                result.put(month, mean);
            }

            List<Double> orderedList = new ArrayList<>();
            for (String m : months) {
                orderedList.add(result.getOrDefault(m, 0.0));
            }

            // 存入当前线程缓存，确保响应式流 Flux 在 onComplete 时能够正确捕获图表数组
            orderedListCache.set(orderedList);
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 工具三：高斯空间距离衰减场计算算子
     * 英文描述高度对齐 Agent Rules 中的 Spatial-RAG 连续场变换与生态影响修正意图
     */
    @GetMapping("/getGaussianPatches")
    @Tool("""
            Calculate the spatially attenuated ecological impact intensity and weighted effective area of target land covers around a specific POI via a Gaussian Spatial Decay Model.
            
            [Applicable Scenarios]
            Invoke this tool as the DEFAULT option whenever the user inquiries about the generic status, current situation,\s
            or distribution of vegetation invasion near/around specific POIs (e.g., 'What is the situation regarding the invasion...').\s
            It is specifically designed to handle proximity analysis, localized ecological pressure, and distance-weighted spatial threats.

------------------------------------------------

[Parameters Mapping Guide]
- poiName: Precise legal name of the target geographic point, must map to: '青口河(Qingkou River)', '青口渔场(Qingkou fishing ground)', or '临洪河口(LinHong River)'.
- type: Integer token for target ecological land cover: 1=Spartina alterniflora, 2=Suaeda salsa, 3=Phragmites australis, 4=Water Body, 5=Others.
- sigma: Bandwidth (in meters) of the Gaussian kernel factor for spatial decay scale regulation. Default is 500.

------------------------------------------------

[Interpretation & Reranking Rules]
The Agent must synthesize responses based on 'weightedAreaHm' (Spatially Aware Ecological Area):
- High weighted area in the Core Zone (patchId=1): Indicates severe, direct spatial ecological threats or strong habitat displacement.
- High original area in the Periphery Zone (patchId=3) but low weighted area: Indicates widespread distribution whose actual local ecological impact has been heavily attenuated by spatial distance.
- High weighted area of Spartina alterniflora in the core ring: Signifies intense bio-invasion risk, severe competitive exclusion against native salt marshes, and critical degradation of waterfowl habitats.
Raw statistics listing is strictly forbidden; professional landscape ecology interpretations must be formulated.
""")
    public List<SpatialDecayVO> getGaussianPatches(
            @P("The geospatial target entity name. Must be one of: '青口河', '青口渔场', or '临洪河口'")
            @RequestParam String poiName,

            @P("The integer classification index of target land cover vegetation: 1=米草, 2=碱蓬, 3=芦苇, 4=水体, 5=其他")
            @RequestParam Integer type,

            @P("The Gaussian decay scale bandwidth parameter (in meters). Default value is 500.0")
            @RequestParam(defaultValue = "500")
            Double sigma
    ) {
        // 调用底层的 PostGIS 叠加坡长检索与高斯衰减算法（保持原有底层完全不动）
        List<WetlandPatchDTO> patches = wetlandPatchMapper.selectIntersectsPatches(poiName, type);
        List<SpatialDecayVO> result = new ArrayList<>();

        for (WetlandPatchDTO patch : patches) {
            SpatialDecayVO vo = new SpatialDecayVO();
            double distance = patch.getSpatialDistance();
            double areaM2 = patch.getPatchArea();
            double areaHm = areaM2 / 10000.0;

            // 原有高斯连续场解算
            double weight = GaussianDecayUtils.gaussianWeight(distance, sigma);
            double weightedArea = areaHm * weight;

            vo.setPatchId(patch.getPatchId());
            vo.setSpatialDistance(distance);
            vo.setOriginalArea(areaM2);
            vo.setOriginalAreaHm(areaHm);
            vo.setGaussianWeight(weight);
            vo.setWeightedAreaHm(weightedArea);

            result.add(vo);
        }
        return result;
    }
}
