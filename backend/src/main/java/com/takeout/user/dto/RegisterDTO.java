package com.takeout.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 顾客注册 DTO
 */
@Data
@Schema(description = "顾客注册入参")
public class RegisterDTO {

    @NotBlank(message = "登录账号不能为空")
    @Size(min = 4, max = 20, message = "登录账号长度需在 4~20 之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "登录账号只能包含字母、数字、下划线")
    @Schema(description = "登录账号", example = "customer2")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需在 6~20 之间")
    @Schema(description = "明文密码（HTTPS 传输，由后端 BCrypt 加密落库）", example = "123456")
    private String password;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", example = "13900000001")
    private String phone;

    @Size(max = 20, message = "昵称长度不能超过 20")
    @Schema(description = "昵称，留空时默认用账号名", example = "小明")
    private String nickname;
}