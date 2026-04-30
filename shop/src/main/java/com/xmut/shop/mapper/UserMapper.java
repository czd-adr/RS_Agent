package com.xmut.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xmut.shop.entity.LandCover;
import com.xmut.shop.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;

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
public interface UserMapper extends BaseMapper<User> {
    // MyBatis-Plus 已内置基础 CRUD，如需自定义 SQL 在此添加
}


