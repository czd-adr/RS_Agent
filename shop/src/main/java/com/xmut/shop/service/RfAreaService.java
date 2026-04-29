package com.xmut.shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xmut.shop.entity.RfArea;
import com.xmut.shop.entity.SamplePoints;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Bennu
 * @since 2025-08-13
 */
public interface RfAreaService extends IService<RfArea> {
    List<RfArea> selectByType(int type);
    Double CalculateRfArea(int type);

    Double CalculateStkArea(int type);

    Double CalculateXgArea(int type);
}
