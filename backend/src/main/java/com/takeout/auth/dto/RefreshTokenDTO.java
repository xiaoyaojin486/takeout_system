package com.takeout.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 刷新令牌 DTO
 */
@Data
@Schema(description = "刷新令牌入参")
public class RefreshTokenDTO {

    @NotBlank(message = "刷新令牌不能为空")
    @Schema(description = "登录时下发的 refreshToken")
    private String refreshToken;
}