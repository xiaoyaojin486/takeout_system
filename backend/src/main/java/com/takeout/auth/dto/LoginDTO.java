package com.takeout.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 三端统一登录 DTO
 */
@Data
@Schema(description = "登录入参")
public class LoginDTO {

    @NotBlank(message = "登录账号不能为空")
    @Schema(description = "登录账号", example = "customer1")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "明文密码", example = "123456")
    private String password;

    /**
     * 端标识，必须与账号自身身份一致
     * CUSTOMER 顾客端 / MERCHANT 商家端 / ADMIN 管理后台
     */
    @NotBlank(message = "端标识不能为空")
    @Schema(description = "CUSTOMER / MERCHANT / ADMIN", example = "CUSTOMER")
    private String clientType;
}