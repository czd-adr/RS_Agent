package com.xmut.shop.controller;

import com.xmut.shop.DTO.SpatialDecayVO;
import com.xmut.shop.DTO.WetlandPatchDTO;
import com.xmut.shop.Utils.GaussianDecayUtils;
import com.xmut.shop.mapper.WetlandPatchMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import dev.langchain4j.agent.tool.Tool;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/wetland")
public class SpatialController {

    @Autowired
    private WetlandPatchMapper wetlandPatchMapper;

    /**
     * 空间圈层检索接口
     *
     * 示例:
     * /wetland/patches?poiName=临洪河口&type=1
     */
    @GetMapping("/patches")
    public List<WetlandPatchDTO> getPatches(
            @RequestParam String poiName,
            @RequestParam Integer type) {

        System.out.println("=================================================");
        System.out.println("POI = " + poiName);
        System.out.println("TYPE = " + type);
        System.out.println("=================================================");

        List<WetlandPatchDTO> result =
                wetlandPatchMapper.selectIntersectsPatches(
                        poiName,
                        type
                );

        System.out.println("返回记录数：" + result.size());

        result.forEach(System.out::println);

        return result;
    }

    @Tool(
            name = "calculateSpatialDecayField",
            value = """
用于计算指定地理区域周边生态地物的空间衰减影响强度。

【适用场景】
当用户询问：

- 某区域周边互花米草影响程度
- 某区域周边芦苇扩散风险
- 某区域周边碱蓬分布情况
- 某区域周边生态压力
- 某区域周边景观连通性
- 某区域周边湿地植被空间影响
- 某区域附近生态风险有多大

必须调用本工具。

------------------------------------------------

【空间计算原理】

本工具基于空间RAG（Spatial Retrieval-Augmented Generation）框架。

首先从PostGIS中检索目标POI周边：

0-500m
500-1000m
1000-1500m

三个距离圈层内的目标生态地物。

随后利用高斯衰减函数：

f_s = exp(-(d²)/(2σ²))

计算距离衰减权重。

其中：

d = 图斑到POI距离（米）

σ = 空间影响带宽（默认500米）

距离越近，
空间影响权重越高；

距离越远，
空间影响权重越低。

------------------------------------------------

【输入参数】

poiName：

目标空间实体名称。

允许值：

- 青口河
- 青口渔场
- 临洪河口

type：

生态地物类型编码：

1 = 互花米草
2 = 碱蓬
3 = 芦苇
4 = 水体
5 = 其他

sigma：

高斯衰减带宽（米）。

默认：

500

------------------------------------------------

【返回结果说明】

patchId：

空间圈层编号：

1 = 0-500m核心区

2 = 500-1000m缓冲区

3 = 1000-1500m外围区

spatialDistance：

圈层代表距离（米）

originalArea：

圈层内目标地物原始面积（平方米）

originalAreaHm：

圈层内目标地物原始面积（公顷）

gaussianWeight：

高斯衰减权重

weightedAreaHm：

空间衰减后的生态影响面积（公顷）

------------------------------------------------

【结果解释规则】

Agent必须根据weightedAreaHm进行解释：

若核心区加权面积最大：

说明目标地物对POI存在显著空间影响。

若外围区面积较大但加权面积较低：

说明目标地物分布较广，
但实际影响已被距离衰减削弱。

若互花米草核心区加权面积较高：

说明存在较强入侵风险，
可能影响本地盐沼植被和水鸟栖息地。

若芦苇或碱蓬核心区加权面积较高：

说明本地植被生态支撑能力较强。

禁止仅罗列数字，
必须输出生态学解释。
"""
    )
    public List<SpatialDecayVO> getGaussianPatches(

            @RequestParam String poiName,

            @RequestParam Integer type,

            @RequestParam(defaultValue = "500")
            Double sigma) {

        List<WetlandPatchDTO> patches =
                wetlandPatchMapper.selectIntersectsPatches(
                        poiName,
                        type
                );

        List<SpatialDecayVO> result =
                new ArrayList<>();

        for (WetlandPatchDTO patch : patches) {

            SpatialDecayVO vo =
                    new SpatialDecayVO();

            double distance =
                    patch.getSpatialDistance();

            double areaM2 =
                    patch.getPatchArea();

            double areaHm =
                    areaM2 / 10000.0;

            double weight =
                    GaussianDecayUtils.gaussianWeight(
                            distance,
                            sigma
                    );

            double weightedArea =
                    areaHm * weight;

            vo.setPatchId(
                    patch.getPatchId()
            );

            vo.setSpatialDistance(
                    distance
            );

            vo.setOriginalArea(
                    areaM2
            );

            vo.setOriginalAreaHm(
                    areaHm
            );

            vo.setGaussianWeight(
                    weight
            );

            vo.setWeightedAreaHm(
                    weightedArea
            );

            result.add(vo);
        }

        return result;
    }
}