package com.takeout.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT 双 token 配置
 */
@Data
@ConfigurationProperties(prefix = "takeout.jwt")
public class JwtProperties {

    /**
     * 签名密钥，长度须 >= 32 字符（JwtUtil 启动时校验，不满足直接启动失败）
     *
     * <p>
     * 算法由 jjwt 按密钥长度自动选择：32~47 字符 → HS256，48~63 → HS384，64+ → HS512。
     * 因此 .env 里默认的 55 字符密钥实际签出的是 HS384（比 HS256 更强），
     * 若因对接其他系统而必须固定 HS256，把密钥长度控制在 32~47 字符之间即可。
     */
    private String secret;

    /** 访问令牌有效期，默认 30 分钟 */
    private Duration accessTokenTtl = Duration.ofMinutes(30);

    /** 刷新令牌有效期，默认 7 天 */
    private Duration refreshTokenTtl = Duration.ofDays(7);
}