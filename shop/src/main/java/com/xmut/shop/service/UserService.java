package com.xmut.shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xmut.shop.entity.User;


/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Bennu
 * @since 2025-08-13
 */
public interface UserService extends IService<User> {
    // 定义业务逻辑方法，例如登录校验
    User login(String username, String password);
}
