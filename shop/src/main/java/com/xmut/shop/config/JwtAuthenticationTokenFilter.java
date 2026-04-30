package com.xmut.shop.config;

import com.xmut.shop.common.JwtUtils;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

// 核心变化：由 javax 变更为 jakarta
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. 使用 Java 17 的 var 关键字简化局部变量（可选，更现代）
        var token = request.getHeader("Authorization");

        // 如果 token 为空，直接放行
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. 解析 token
        var claims = JwtUtils.parseToken(token);
        if (claims == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. 封装用户信息
        // 注意：如果你在 Java 17 中使用了较新的 Spring Security，
        // 建议在这里明确指定权限，哪怕是空的 ArrayList
        var authenticationToken =
                new UsernamePasswordAuthenticationToken(claims.getSubject(), null, new ArrayList<>());

        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        // 放行
        filterChain.doFilter(request, response);
    }
}