package com.xmut.shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xmut.shop.entity.LandCover;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Bennu
 * @since 2025-08-13
 */
public interface LandCoverService extends IService<LandCover> {


    List<LandCover> getLandCoverStats(@Param("geojson") String geojson,@Param("algorithm") String algorithm);
}
