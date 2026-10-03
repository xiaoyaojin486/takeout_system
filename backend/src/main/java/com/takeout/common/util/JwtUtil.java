package com.takeout.common.util;

import com.takeout.common.constant.SecurityConstants;
import com.takeout.common.exception.UnauthorizedException;
import com.takeout.common.properties.JwtProperties;
import com.takeout.common.result.ResultCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 工具：双 token 的生成与解析（jjwt 0.12 API）
 *
 * <p>
 * 两种令牌用同一密钥签名，靠 {@code type} claim 区分，防止 refresh 冒充 access 直接访问接口。
 * </p>
 *
 * <p>
 * 签名算法由 jjwt 依据密钥长度自动选择（HS256/HS384/HS512），密钥越长算法越强，
 * 不硬编码算法是为了避免「密钥长度与算法强度不匹配」被 jjwt 拒绝。
 * </p>
 */
@Component
public class JwtUtil {

    private final JwtProperties properties;

    /** 密钥懒解析 + 缓存，避免每次签名都重新构造 */
    private volatile SecretKey secretKey;

    public JwtUtil(JwtProperties properties) {
        if (properties == null || !StringUtils.hasText(properties.getSecret())
                || properties.getSecret().length() < 32) {
            throw new IllegalArgumentException(
                    "JWT 密钥未配置或长度小于 32 字符，请在项目根目录 .env 中设置 JWT_SECRET");
        }
        this.properties = properties;
    }

    /**
     * 生成访问令牌（默认 30 分钟）
     * claims: sub=userId、username、ut=userType、type=access、jti
     */
    public String generateAccessToken(Long userId, String username, Integer userType) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(SecurityConstants.CLAIM_USERNAME, username)
                .claim(SecurityConstants.CLAIM_USER_TYPE, userType)
                .claim(SecurityConstants.CLAIM_TYPE, SecurityConstants.TOKEN_TYPE_ACCESS)
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(properties.getAccessTokenTtl())))
                .signWith(key())
                .compact();
    }

    /**
     * 生成刷新令牌（默认 7 天）
     * claims: sub=userId、type=refresh、jti —— refresh 不带 userType，
     * 因为它唯一的用途是换取新令牌，业务接口不接受它（过滤器会拒绝 type != access）
     */
    public String generateRefreshToken(Long userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(SecurityConstants.CLAIM_TYPE, SecurityConstants.TOKEN_TYPE_REFRESH)
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(properties.getRefreshTokenTtl())))
                .signWith(key())
                .compact();
    }

    /**
     * 解析并校验签名；过期时按令牌类型返回不同业务码
     *
     * <p>
     * 40101 = access 过期（前端静默刷新后重放）；40102 = refresh 失效（前端跳登录）。
     * </p>
     */
    public Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(key()).build()
                    .parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            Object type = e.getClaims().get(SecurityConstants.CLAIM_TYPE);
            throw new UnauthorizedException(SecurityConstants.TOKEN_TYPE_REFRESH.equals(type)
                    ? ResultCode.REFRESH_TOKEN_INVALID
                    : ResultCode.ACCESS_TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            // 签名不合法 / 格式错误 / 空串：统一按未认证处理，不向调用方暴露具体原因
            throw new UnauthorizedException(ResultCode.UNAUTHORIZED);
        }
    }

    /** 计算令牌剩余有效秒数，用于黑名单 TTL（黑名单只需存活到令牌自身过期） */
    public long remainingSeconds(Claims claims) {
        long remain = (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000;
        return Math.max(1, remain);
    }

    private SecretKey key() {
        if (secretKey == null) {
            synchronized (this) {
                if (secretKey == null) {
                    secretKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
                }
            }
        }
        return secretKey;
    }
}