package com.takeout.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码加密配置
 *
 * <p>只引 spring-security-crypto 拿 BCrypt，不引入完整 Spring Security 的过滤器链 ——
 * 认证由自己的 AuthenticationFilter 负责，避免两套机制打架。</p>
 *
 * <p>BCrypt 自带随机盐，同一个明文两次加密结果不同，因此校验只能用 matches()，
 * <b>不能</b>用「加密后比对字符串」的方式。</p>
 */
@Configuration
public class PasswordConfig {

    /**
     * strength = 10（默认值）：约 2^10 次哈希迭代，单次约 50~100ms，
     * 既能抵抗离线暴力破解，又不至于登录接口被拖慢。
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}