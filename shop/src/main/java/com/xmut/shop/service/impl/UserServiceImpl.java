package com.xmut.shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xmut.shop.common.JwtUtils;
import com.xmut.shop.entity.User;
import com.xmut.shop.mapper.UserMapper;
import com.xmut.shop.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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

    @Autowired
    private BCryptPasswordEncoder encoder;

    @Override
    public User login(String username, String password) {
        // 1. 先根据用户名查询该用户（假设数据库存的是 BCrypt 后的密文）
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = this.getOne(wrapper);

        if (user == null) return null;

        // 2. 校验明文密码是否匹配数据库密文
        if (encoder.matches(password, user.getPassword())) {
            // 3. 匹配成功，生成 Token 存入对象并返回
            String token = JwtUtils.createToken(user.getId(), user.getUsername(), user.getRole());
            user.setToken(token);
            // 注意：不要把密码返回给前端，哪怕是密文
            user.setPassword(null);
            return user;
        }
        // 在你的测试类或 main 方法运行一下
        System.out.println(new BCryptPasswordEncoder().encode("123456"));
// 输出类似：$2a$10$7zBvR... (每次运行都不同，这是正常的)
        return null;
    }
}