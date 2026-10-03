package com.takeout.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登出 DTO
 *
 * <p>refreshToken 可选但建议传：只登出当前设备时传它，服务端据此删除对应的 refresh 记录；
 * accessToken 由过滤器从请求头里取，不需要前端传。</p>
 */
@Data
@Schema(description = "登出入参")
public class LogoutDTO {

    @Schema(description = "当前设备的刷新令牌，传了就一并作废")
    private String refreshToken;
}