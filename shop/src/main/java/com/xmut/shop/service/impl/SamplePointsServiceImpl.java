package com.xmut.shop.service.impl;

import com.xmut.shop.entity.SamplePoints;
import com.xmut.shop.mapper.SamplePointsMapper;
import com.xmut.shop.service.SamplePointsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
public class SamplePointsServiceImpl extends ServiceImpl<SamplePointsMapper,SamplePoints> implements SamplePointsService {
    @Override
    public List<Integer> getIdByGrass(int grass) {
        return baseMapper.selectIdByGrass(grass);
    }
    @Override
    public List<SamplePoints> getPointByGrass(int grass){
        return baseMapper.selectByGrass(grass);
    }
    @Override
    public List<SamplePoints> selectRealPoint(int grass){
        return baseMapper.selectRealPoint(grass);
    }

}
