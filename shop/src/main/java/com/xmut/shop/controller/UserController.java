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

        // 调用 Service 直接传入对象进行简单校验
        User user = userService.login(loginParam.getUsername(), loginParam.getPassword());

        if (user != null) {
            result.put("status", "success");
            result.put("msg", "登录成功");
            // 关键点：只返回前端需要的 id 和 role，为了安全屏蔽密码
            user.setPassword(null);
            result.put("data", user);
        } else {
            result.put("status", "error");
            result.put("msg", "用户名或密码错误");
        }

        return result;
    }
}
