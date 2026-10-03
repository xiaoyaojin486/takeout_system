package com.takeout.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 双令牌返回 VO
 */
@Data
@Builder
@Schema(description = "令牌信息")
public class TokenVO implements Serializable {

    @Schema(description = "访问令牌，放进 Authorization: Bearer {accessToken}")
    private String accessToken;

    @Schema(description = "刷新令牌，仅用于 /auth/refresh 换取新令牌")
    private String refreshToken;

    @Schema(description = "令牌类型，固定 Bearer")
    private String tokenType;

    @Schema(description = "访问令牌有效期（秒），前端可据此提前刷新")
    private Long expiresIn;
}