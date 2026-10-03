package com.takeout.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 接口文档配置：Swagger UI 里可直接填 Bearer token 调试三端接口
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI takeoutOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("外卖系统 API")
                        .version("1.0.0")
                        .description("""
                                三端接口按路径前缀隔离，登录后需在 Swagger 右上角 Authorize 填入 accessToken：

                                - 顾客端 /customer/**   （账号类型 1）
                                - 商家端 /merchant/**   （账号类型 2）
                                - 管理端 /admin/**      （账号类型 3）

                                双 token 说明：accessToken 默认 30 分钟，过期返回 40101；
                                用 /auth/refresh 换新令牌；refreshToken 失效返回 40102。
                                """)
                        .contact(new Contact().name("takeout")))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("填写 accessToken，不需要加 Bearer 前缀")));
    }
}