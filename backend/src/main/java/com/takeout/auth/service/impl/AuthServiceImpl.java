package com.takeout.auth.service.impl;

import com.takeout.auth.dto.LoginDTO;
import com.takeout.auth.dto.LogoutDTO;
import com.takeout.auth.dto.RefreshTokenDTO;
import com.takeout.auth.service.AuthService;
import com.takeout.auth.vo.TokenVO;
import com.takeout.auth.vo.UserInfoVO;
import com.takeout.common.cache.CacheClient;
import com.takeout.common.constant.CacheKeys;
import com.takeout.common.constant.SecurityConstants;
import com.takeout.common.context.LoginUser;
import com.takeout.common.context.UserContextHolder;
import com.takeout.common.enums.UserTypeEnum;
import com.takeout.common.exception.BusinessException;
import com.takeout.common.exception.UnauthorizedException;
import com.takeout.common.properties.JwtProperties;
import com.takeout.common.result.ResultCode;
import com.takeout.common.util.JwtUtil;
import com.takeout.user.api.UserApi;
import com.takeout.user.api.vo.UserAuthVO;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务实现（双 token 核心）
 *
 * <p><b>设计要点</b>：</p>
 * <ol>
 *     <li><b>旋转刷新</b>：每次 refresh 都作废旧 refresh 并签发新的一对令牌。
 *         这样即使 refresh 被偷走，攻击者用过一次后真正的用户再用就会失败，
 *         等于把「令牌泄露」的窗口从 7 天压缩到一次使用；</li>
 *     <li><b>多端共存</b>：refresh 以 jti 为 key 独立存储，同一账号在 H5/商家端/后台
 *         同时登录互不影响，任意一端登出也不影响其他端；</li>
 *     <li><b>状态联动</b>：刷新时回查账号状态，被停用/删除的账号无法靠 refresh 续命；</li>
 *     <li><b>失败计数</b>：密码错误累计 5 次锁定 10 分钟，配合 IP 限流抬高暴力破解成本。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /** 连续密码错误上限 */
    private static final int MAX_LOGIN_FAIL = 5;

    /** 失败计数窗口（秒） */
    private static final long LOGIN_FAIL_WINDOW = 10 * 60L;

    private final UserApi userApi;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final CacheClient cacheClient;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public TokenVO login(LoginDTO dto) {
        String username = dto.getUsername().trim();
        String failKey = CacheKeys.LOGIN_FAIL + username;

        // ① 先看失败计数：已锁定就不再查库、不再比对密码，避免被当作密码预言机
        Long failCount = toLong(cacheClient.get(failKey));
        if (failCount != null && failCount >= MAX_LOGIN_FAIL) {
            throw new BusinessException(ResultCode.LOGIN_TOO_MANY_FAILURES);
        }

        // ② 查账号 + 校验密码：账号不存在与密码错误返回同一提示，不泄露账号是否存在
        UserAuthVO user = userApi.findAuthByUsername(username);
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            long current = cacheClient.increment(failKey, LOGIN_FAIL_WINDOW);
            log.warn("登录失败 username={} 累计失败次数={}", username, current);
            throw new BusinessException(ResultCode.USERNAME_PASSWORD_ERROR);
        }

        // ③ 账号状态
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        // ④ 端与身份必须匹配：商家账号不能从顾客端登录，反之亦然
        UserTypeEnum clientType = parseClientType(dto.getClientType());
        if (!clientType.getCode().equals(user.getUserType())) {
            log.warn("端身份不匹配 username={} clientType={} userType={}",
                    username, dto.getClientType(), user.getUserType());
            throw new BusinessException(ResultCode.USERNAME_PASSWORD_ERROR);
        }

        // ⑤ 登录成功：清失败计数 + 记录登录时间
        cacheClient.delete(failKey);
        userApi.updateLastLoginTime(user.getUserId());

        log.info("登录成功 userId={} username={} clientType={}", user.getUserId(), username, dto.getClientType());
        return issueTokens(user.getUserId(), user.getUsername(), user.getUserType());
    }

    @Override
    public TokenVO refresh(RefreshTokenDTO dto) {
        // ① 解析：过期/签名错误都会在这里被拒（refresh 过期返回 40102）
        Claims claims = jwtUtil.parse(dto.getRefreshToken());
        if (!SecurityConstants.TOKEN_TYPE_REFRESH.equals(claims.get(SecurityConstants.CLAIM_TYPE))) {
            // 拿 access token 来刷新：直接拒绝
            throw new UnauthorizedException(ResultCode.UNAUTHORIZED);
        }

        String jti = claims.getId();
        String cachedUserId = cacheClient.get(CacheKeys.AUTH_REFRESH + jti);
        if (cachedUserId == null || !cachedUserId.equals(claims.getSubject())) {
            // 缓存里没有 = 已登出，或已被上一次刷新旋转作废（重放攻击）
            log.warn("刷新令牌无效或已被旋转 jti={}", jti);
            throw new UnauthorizedException(ResultCode.REFRESH_TOKEN_INVALID);
        }

        Long userId = Long.valueOf(claims.getSubject());
        // ② 状态联动：停用/删除的账号不能靠刷新续命
        UserAuthVO user = userApi.findAuthById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            cacheClient.delete(CacheKeys.AUTH_REFRESH + jti);
            throw new UnauthorizedException(ResultCode.REFRESH_TOKEN_INVALID);
        }

        // ③ 旋转：旧 refresh 立刻作废，签发全新一对
        cacheClient.delete(CacheKeys.AUTH_REFRESH + jti);
        log.info("刷新令牌成功（已旋转）userId={} oldJti={}", userId, jti);
        return issueTokens(userId, user.getUsername(), user.getUserType());
    }

    @Override
    public void logout(String accessToken, LogoutDTO dto) {
        // ① access 进黑名单：TTL 取令牌剩余有效期，到期自动清理，黑名单不会无限膨胀
        if (accessToken != null && !accessToken.isBlank()) {
            try {
                Claims claims = jwtUtil.parse(accessToken);
                cacheClient.set(CacheKeys.AUTH_BLACKLIST + claims.getId(), "1", jwtUtil.remainingSeconds(claims));
            } catch (Exception e) {
                // 令牌本来就无效/过期，登出保持幂等，不报错
                log.debug("登出时解析 access token 失败（忽略）: {}", e.getMessage());
            }
        }
        // ② 删除 refresh 记录：对应设备彻底失效
        if (dto != null && dto.getRefreshToken() != null && !dto.getRefreshToken().isBlank()) {
            try {
                Claims claims = jwtUtil.parse(dto.getRefreshToken());
                cacheClient.delete(CacheKeys.AUTH_REFRESH + claims.getId());
            } catch (Exception e) {
                log.debug("登出时解析 refresh token 失败（忽略）: {}", e.getMessage());
            }
        }
    }

    @Override
    public UserInfoVO userInfo() {
        LoginUser user = UserContextHolder.requireLogin();
        UserTypeEnum type = UserTypeEnum.of(user.getUserType());
        return UserInfoVO.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .userType(user.getUserType())
                .userTypeDesc(type == null ? null : type.getDesc())
                .build();
    }

    /**
     * 签发令牌对
     *
     * <p>refresh 的 jti 写入缓存，缓存存活期 = refresh 有效期，
     * 因此「登出」「旋转」都只需删除这个 key 即可让令牌立即失效。</p>
     */
    private TokenVO issueTokens(Long userId, String username, Integer userType) {
        String accessToken = jwtUtil.generateAccessToken(userId, username, userType);
        String refreshToken = jwtUtil.generateRefreshToken(userId);

        Claims refreshClaims = jwtUtil.parse(refreshToken);
        cacheClient.set(CacheKeys.AUTH_REFRESH + refreshClaims.getId(),
                String.valueOf(userId), jwtProperties.getRefreshTokenTtl().toSeconds());

        return TokenVO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenTtl().toSeconds())
                .build();
    }

    /** clientType 字符串 → 枚举；非法值直接拒绝（不静默兜底成顾客，否则等于开了后门） */
    private UserTypeEnum parseClientType(String clientType) {
        if (clientType == null) {
            throw new BusinessException(ResultCode.CLIENT_TYPE_NOT_ALLOWED);
        }
        try {
            return UserTypeEnum.valueOf(clientType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ResultCode.CLIENT_TYPE_NOT_ALLOWED);
        }
    }

    private Long toLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}