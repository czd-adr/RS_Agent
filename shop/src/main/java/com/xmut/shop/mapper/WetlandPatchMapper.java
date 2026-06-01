package com.xmut.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xmut.shop.DTO.WetlandPatchDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WetlandPatchMapper extends BaseMapper<WetlandPatchDTO> {

    @Select("""
    WITH poi_geom AS (
        -- 获取目标POI并转换为米制投影坐标系
        SELECT ST_Transform(p.geom, 4527) AS geom_m
        FROM "spatial_poi" p
        WHERE p.poi_name = #{poiName}
        LIMIT 1
    ),

    calculated_patches AS (
        -- 获取1500m范围内目标地类图斑
        SELECT
            f.id,
            f.area_m2,
            ST_Distance(
                ST_Transform(f.geom, 4527),
                (SELECT geom_m FROM poi_geom)
            ) AS dist_m
        FROM "RF_area_export" f
        WHERE f."class" = #{type}
          AND ST_DWithin(
                ST_Transform(f.geom, 4527),
                (SELECT geom_m FROM poi_geom),
                1500.0
          )
    ),

    bucketed_patches AS (
        -- 距离环带划分
        SELECT
            id,
            area_m2,
            dist_m,

            CASE
                WHEN dist_m >= 0 AND dist_m < 500 THEN 500.0
                WHEN dist_m >= 500 AND dist_m < 1000 THEN 1000.0
                WHEN dist_m >= 1000 AND dist_m <= 1500 THEN 1500.0
                ELSE 9999.0
            END AS band_distance,

            CASE
                WHEN dist_m >= 0 AND dist_m < 500 THEN 1
                WHEN dist_m >= 500 AND dist_m < 1000 THEN 2
                WHEN dist_m >= 1000 AND dist_m <= 1500 THEN 3
                ELSE 4
            END AS band_order

        FROM calculated_patches
    )

    SELECT
        band_order::bigint AS patchId,
        SUM(area_m2)::double precision AS patchArea,
        band_distance::double precision AS spatialDistance
    FROM bucketed_patches
    GROUP BY band_order, band_distance
    ORDER BY band_order
""")
    List<WetlandPatchDTO> selectIntersectsPatches(
            @Param("poiName") String poiName,
            @Param("type") int type
    );
}