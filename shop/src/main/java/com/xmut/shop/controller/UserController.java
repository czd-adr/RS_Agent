package com.xmut.shop.controller;

import com.xmut.shop.entity.User;
import com.xmut.shop.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;

    /**
     * 用户登录接口
     * 请求路径: POST /user/login
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody User loginParam) {
        Map<String, Object> result = new HashMap<>();
        System.out.println("param"+loginParam);
        // 1. 调用 Service 进行登录校验
        User user = userService.login(loginParam.getUsername(), loginParam.getPassword());

        // 2. 根据查询结果返回不同的响应
        if (user != null) {
            // 登录成功
            result.put("status", "success");
            result.put("msg", "登录成功");
            // 建议：实际开发中不要把密码返回给前端，可以手动置空
            user.setPassword(null);
            result.put("data", user);
        } else {
            // 登录失败
            result.put("status", "error");
            result.put("msg", "用户名或密码错误");
            result.put("data", null);
        }

        return result;
    }
}
