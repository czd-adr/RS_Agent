package com.xmut.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xmut.shop.entity.RfArea;
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
public interface RfAreaMapper extends BaseMapper<RfArea> {
    @Select("SELECT id, ST_AsText(geom) as geom, \"class\" as className, count,area_m2 as area FROM \"RF_area_export\" WHERE \"class\" = #{type}")
    List<RfArea> selectByType(int type);

    @Select("SELECT SUM(area_m2) AS total_area FROM \"RF_area_export\" WHERE  \"class\" = #{type}")
    Double CalculateRfArea(int type);

    @Select("SELECT SUM(area_m2) AS total_area FROM \"stk_area_export\" WHERE  \"class\" = #{type}")
    Double CalculateStkArea(int type);

    @Select("SELECT SUM(area_m2) AS total_area FROM \"xg_area_export\" WHERE  \"class\" = #{type}")
    Double CalculateXgArea(int type);

    @Select("SELECT SUM(area_m2) AS total_area FROM \"xg_area_export\" WHERE  \"class\" = #{type}")
    Double CalculateSvmArea(int type);
}
