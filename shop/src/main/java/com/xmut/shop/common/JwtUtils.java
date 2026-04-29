package com.xmut.shop.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;

public class JwtUtils {
    // 关键修改：密钥长度必须 >= 32 个字符，且建议使用随机字符串
    private static final String SECRET_STR = "AzimuthGeospatialSecretKey123456789_Secure";
    private static final long EXPIRE = 604800000; // 7天 (毫秒)

    // 将字符串转换为适用于 HS256 的 SecretKey 对象
    private static SecretKey getSigningKey() {
        byte[] keyBytes = SECRET_STR.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // 生成 Token
    public static String createToken(Long userId, String username, String role) {
        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setSubject("azimuth-user")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE))
                .claim("userId", userId)
                .claim("username", username)
                .claim("role", role)
                // 方案一：如果你坚持用旧版风格，必须确保 SECRET_STR 够长
                // .signWith(SignatureAlgorithm.HS256, SECRET_STR)
                // 方案二：符合 0.11.5+ 标准的写法（推荐）
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // 解析 Token
    public static Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }
}