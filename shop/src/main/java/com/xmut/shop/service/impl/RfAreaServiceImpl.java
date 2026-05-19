package com.xmut.shop.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xmut.shop.entity.RfArea;
import com.xmut.shop.entity.SamplePoints;
import com.xmut.shop.mapper.RfAreaMapper;
import com.xmut.shop.service.RfAreaService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Bennu
 * @since 2025-08-13
 */
@Service
public class RfAreaServiceImpl extends ServiceImpl<RfAreaMapper,RfArea> implements RfAreaService {
    @Override
    public List<RfArea> selectByType(int type){
        return baseMapper.selectByType(type);
    }
    @Override
    public Double CalculateRfArea(int type){ return baseMapper.CalculateRfArea(type);}

    @Override
    public Double CalculateStkArea(int type){ return baseMapper.CalculateStkArea(type);}

    @Override
    public Double CalculateXgArea(int type){ return baseMapper.CalculateXgArea(type);}

    @Override
    public Double calculatePolygonIntersectsArea(String poiName, int type) {
        return baseMapper.calculatePolygonIntersectsArea(poiName, type);
    }
}
