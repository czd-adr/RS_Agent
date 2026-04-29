package com.xmut.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xmut.shop.entity.LandCover;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author Bennu
 * @since 2025-08-13
 */
@Mapper
public interface LandCoverMapper extends BaseMapper<LandCover> {

    @Select(
            "WITH geom_union AS ( " +
                    "  SELECT ST_Transform(ST_GeomFromGeoJSON(#{geojson}), 3857) AS geom " +
                    "), clipped AS ( " +
                    "  SELECT ST_Clip(ST_Transform(rast, 3857), 1, g.geom, true) AS rast " +
                    "  FROM ${algorithm}, geom_union g " +
                    "  WHERE ST_Intersects(ST_Transform(rast, 3857), g.geom) " +
                    "), vals AS ( " +
                    "  SELECT (ST_ValueCount(rast, 1, true)).* FROM clipped " +
                    ") " +
                    "SELECT (gv).value AS index, " +
                    "       ROUND((SUM((gv).count) * " +
                    "              ST_PixelWidth((SELECT rast FROM clipped LIMIT 1)) * " +
                    "              ST_PixelHeight((SELECT rast FROM clipped LIMIT 1)))::numeric, 2) AS area " +
                    "FROM vals gv " +
                    "GROUP BY (gv).value " +
                    "ORDER BY (gv).value"
    )

    List<LandCover> getLandCoverStats(@Param("geojson") String geojson,
                                      @Param("algorithm") String algorithm);
}


