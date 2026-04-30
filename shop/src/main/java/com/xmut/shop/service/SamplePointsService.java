package com.xmut.shop.service;

import com.xmut.shop.entity.SamplePoints;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Bennu
 * @since 2025-08-13
 */
public interface SamplePointsService extends IService<SamplePoints> {
    List<Integer> getIdByGrass(int grass);
    List<SamplePoints> getPointByGrass(int grass);

    List<SamplePoints> selectRealPoint(int grass);

}
