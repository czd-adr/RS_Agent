package com.xmut.shop.service;

import com.xmut.shop.entity.Bare;
import com.xmut.shop.entity.PV;
import com.xmut.shop.entity.SamplePoints;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xmut.shop.mapper.SamplePointsMapper;
import org.springframework.beans.factory.annotation.Autowired;

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

    List<Bare> getAllPoints();

    List<PV> selectPhotovoltaicPoints();
}
