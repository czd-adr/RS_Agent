package com.xmut.shop.mapper;

import com.xmut.shop.entity.Bare;
import com.xmut.shop.entity.PV;
import com.xmut.shop.entity.RfArea;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xmut.shop.entity.SamplePoints;
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
public interface SamplePointsMapper extends BaseMapper<SamplePoints> {
    @Select("SELECT id FROM sample_points WHERE grass = #{grass}")
    List<Integer> selectIdByGrass(int grass);
    @Select("SELECT id, ST_AsText(geom) as geom, grass FROM sample_points WHERE grass = #{grass}")
    List<SamplePoints> selectByGrass(int grass);

    @Select("SELECT id, geom, grass FROM sample_points WHERE grass = #{grass}")
    List<SamplePoints> selectRealPoint(int grass);

    @Select("SELECT geom FROM \"Bare\"")
    List<Bare> selectAllPoints();

    @Select("SELECT geom FROM \"Photovoltaic_Points1\"")
    List<PV> selectPhotovoltaicPoints();
}
