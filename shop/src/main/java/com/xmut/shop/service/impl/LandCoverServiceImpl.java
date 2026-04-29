package com.xmut.shop.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xmut.shop.entity.LandCover;
import com.xmut.shop.entity.RfArea;
import com.xmut.shop.mapper.LandCoverMapper;
import com.xmut.shop.mapper.RfAreaMapper;
import com.xmut.shop.service.LandCoverService;
import com.xmut.shop.service.RfAreaService;
import org.apache.ibatis.annotations.Param;
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
public class LandCoverServiceImpl extends ServiceImpl<LandCoverMapper, LandCover> implements LandCoverService {
    @Override
    public List<LandCover> getLandCoverStats(@Param("geojson")String geojson, @Param("algorithm") String algorithm) {
        return baseMapper.getLandCoverStats(geojson,algorithm);
    }
}
