package com.xmut.shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xmut.shop.common.JwtUtils;
import com.xmut.shop.entity.User;
import com.xmut.shop.mapper.UserMapper;
import com.xmut.shop.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    // 1. 定义为 final，确保不可变性
    private final PasswordEncoder encoder;

    // 2. 构造器注入（Spring Boot 会自动完成，无需显式写 @Autowired）
    public UserServiceImpl(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public User login(String username, String password) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = this.getOne(wrapper);

        if (user != null && encoder.matches(password, user.getPassword())) {
            String token = JwtUtils.createToken(user.getId(), user.getUsername(), user.getRole());
            user.setToken(token);
            user.setPassword(null);
            return user;
        }
        return null;
    }
}