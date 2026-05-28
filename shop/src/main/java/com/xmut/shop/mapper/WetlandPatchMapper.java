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
            SELECT p.geom FROM "spatial_poi" p WHERE p.poi_name = #{poiName} LIMIT 1
        )
        SELECT 
            f.id AS patchId,
            f.area_m2 AS patchArea,
            -- 实时动态计算每一个图斑到目标POI的空间物理距离
            ST_Distance(f.geom, (SELECT geom FROM poi_geom)) AS spatialDistance
        FROM "RF_area_export" f
        WHERE f."class" = #{type}
          -- 采用硬相交(ST_Intersects)或软范围(ST_DWithin)作为第一道空间粗筛门槛
          -- 这里以ST_DWithin为例，允许搜寻POI周围2000米内的潜在扩散图斑
          AND ST_DWithin(f.geom, (SELECT geom FROM poi_geom), 2000.0)
    """)
    List<WetlandPatchDTO> selectIntersectsPatches(@Param("poiName") String poiName, @Param("type") int type);
}