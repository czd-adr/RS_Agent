package com.xmut.shop.config;

import com.xmut.shop.common.JwtUtils;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 1. 获取请求头中的 token
        String token = request.getHeader("Authorization");

        // 如果 token 为空，直接放行（交给后面的 Security 规则判断是否拦截）
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. 解析 token
        Claims claims = JwtUtils.parseToken(token);
        if (claims == null) {
            // token 非法或过期
            filterChain.doFilter(request, response);
            return;
        }

        // 3. 将解析出来的用户信息存入 SecurityContext 上下文，告知系统此用户已登录
        // 这里可以根据 claim 里的 role 封装权限信息
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(claims.getSubject(), null, new ArrayList<>());

        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        // 放行
        filterChain.doFilter(request, response);
    }
}