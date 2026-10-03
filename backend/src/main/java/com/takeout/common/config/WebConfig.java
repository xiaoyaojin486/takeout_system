package com.takeout.common.config;

import com.takeout.common.cache.CacheClient;
import com.takeout.common.context.LoginUserLoader;
import com.takeout.common.filter.AuthenticationFilter;
import com.takeout.common.interceptor.UserTypeInterceptor;
import com.takeout.common.properties.FileProperties;
import com.takeout.common.properties.SecurityProperties;
import com.takeout.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web 配置：认证过滤器 / 端级授权拦截器 / 上传目录静态映射 / CORS
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final SecurityProperties securityProperties;
    private final FileProperties fileProperties;
    private final JwtUtil jwtUtil;
    private final CacheClient cacheClient;
    private final ObjectProvider<LoginUserLoader> loginUserLoaderProvider;

    /**
     * 认证过滤器：注册为 Filter 而非 @Component，避免被 Spring Boot 自动注册两次
     *
     * <p>
     * order = 1 保证它在业务处理之前执行。
     * </p>
     */
    @Bean
    public FilterRegistrationBean<AuthenticationFilter> authenticationFilterRegistration() {
        FilterRegistrationBean<AuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AuthenticationFilter(
                securityProperties, jwtUtil, loginUserLoaderProvider, cacheClient));
        registration.addUrlPatterns("/*");
        registration.setName("authenticationFilter");
        registration.setOrder(1);
        return registration;
    }

    /** 端级授权：拦截三端前缀路径（白名单在拦截器内部放行） */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new UserTypeInterceptor(securityProperties))
                .addPathPatterns("/customer/**", "/merchant/**", "/admin/**");
    }

    /** 上传文件对外访问：把磁盘目录映射成 URL，前端 <img> 可直接引用 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get(fileProperties.getUploadPath()).toAbsolutePath().normalize();
        File dir = uploadDir.toFile();
        if (!dir.exists() && !dir.mkdirs()) {
            log.error("上传目录创建失败：{}，图片上传功能将不可用", uploadDir);
        }
        String location = "file:" + uploadDir + File.separator;
        registry.addResourceHandler(fileProperties.getAccessPrefix() + "/**")
                .addResourceLocations(location);
        log.info("上传目录：{}，访问前缀：{}", uploadDir, fileProperties.getAccessPrefix());
    }

    /**
     * CORS：开发期允许两个前端工程的 Vite 端口直接访问
     *
     * <p>
     * 生产环境应改为具体域名，不要用 * （带凭证的跨域请求会被浏览器拒绝）。
     * </p>
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(
                        "http://localhost:[*]",
                        "http://127.0.0.1:[*]")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(true)
                .maxAge(3600);
    }
}