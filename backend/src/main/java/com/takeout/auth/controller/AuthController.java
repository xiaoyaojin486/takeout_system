package com.takeout.auth.controller;

import com.takeout.auth.dto.LoginDTO;
import com.takeout.auth.dto.LogoutDTO;
import com.takeout.auth.dto.RefreshTokenDTO;
import com.takeout.auth.service.AuthService;
import com.takeout.auth.vo.TokenVO;
import com.takeout.auth.vo.UserInfoVO;
import com.takeout.common.annotation.RateLimit;
import com.takeout.common.constant.SecurityConstants;
import com.takeout.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

/**
 * 认证接口（三端共用）
 *
 * <p>登录/刷新在白名单里，免 token 访问；登出与 /me 需要携带 access token。</p>
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证（三端共用）")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @RateLimit(count = 10, time = 60, key = "auth:login", type = RateLimit.LimitType.IP)
    @Operation(summary = "登录", description = "clientType 必须与账号身份一致：CUSTOMER / MERCHANT / ADMIN")
    public Result<TokenVO> login(@Validated @RequestBody LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }

    @PostMapping("/refresh")
    @RateLimit(count = 30, time = 60, key = "auth:refresh", type = RateLimit.LimitType.IP)
    @Operation(summary = "刷新令牌", description = "旧 refreshToken 会被立即作废（旋转），返回全新一对令牌")
    public Result<TokenVO> refresh(@Validated @RequestBody RefreshTokenDTO dto) {
        return Result.ok(authService.refresh(dto));
    }

    @PostMapping("/logout")
    @Operation(summary = "登出", description = "accessToken 进黑名单 + 删除当前设备的 refresh 记录")
    public Result<Void> logout(HttpServletRequest request, @RequestBody(required = false) LogoutDTO dto) {
        authService.logout(resolveAccessToken(request), dto);
        return Result.ok();
    }

    @GetMapping("/me")
    @Operation(summary = "当前登录用户信息")
    public Result<UserInfoVO> me() {
        return Result.ok(authService.userInfo());
    }

    /** 从请求头取 access token：登出要把它的 jti 拉黑，所以必须原样拿到 */
    private String resolveAccessToken(HttpServletRequest request) {
        String header = request.getHeader(SecurityConstants.AUTH_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(SecurityConstants.BEARER_PREFIX)) {
            return header.substring(SecurityConstants.BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}