package com.takeout.common.filter;

import com.takeout.common.cache.CacheClient;
import com.takeout.common.constant.CacheKeys;
import com.takeout.common.constant.SecurityConstants;
import com.takeout.common.context.LoginUser;
import com.takeout.common.context.LoginUserLoader;
import com.takeout.common.context.UserContextHolder;
import com.takeout.common.exception.UnauthorizedException;
import com.takeout.common.properties.SecurityProperties;
import com.takeout.common.result.Result;
import com.takeout.common.result.ResultCode;
import com.takeout.common.util.JsonUtil;
import com.takeout.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 认证过滤器（第一道闸）：白名单 → 解析 access token → 黑名单 → 加载登录用户 → 绑定上下文
 *
 * <p>
 * <b>校验顺序不能随意调整</b>：
 * </p>
 * <ol>
 * <li>白名单最先，否则登录接口自己就被拦了；</li>
 * <li>解析令牌在建黑名单校验之前，因为黑名单 key 依赖解析出的 jti；</li>
 * <li>校验 type == access 必须在黑名单之前：refresh token 不参与黑名单（它靠缓存删除失效），
 * 若先查黑名单则会用 refresh 的 jti 去查、永远查不到，等于放行了 refresh；</li>
 * <li>用户加载放最后，避免为无效令牌做无谓查库。</li>
 * </ol>
 *
 * <p>
 * 本过滤器只负责「你是谁」（认证）；「你能干什么」（授权）由 UserTypeInterceptor 按路径前缀负责。
 * </p>
 */
@Slf4j
public class AuthenticationFilter extends OncePerRequestFilter {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private final SecurityProperties securityProperties;
    private final JwtUtil jwtUtil;
    private final ObjectProvider<LoginUserLoader> loaderProvider;
    private final CacheClient cacheClient;

    public AuthenticationFilter(SecurityProperties securityProperties,
            JwtUtil jwtUtil,
            ObjectProvider<LoginUserLoader> loaderProvider,
            CacheClient cacheClient) {
        this.securityProperties = securityProperties;
        this.jwtUtil = jwtUtil;
        this.loaderProvider = loaderProvider;
        this.cacheClient = cacheClient;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            if (isWhitelist(request)) {
                chain.doFilter(request, response);
                return;
            }

            String token = resolveToken(request);
            if (!StringUtils.hasText(token)) {
                writeUnauthorized(response, ResultCode.UNAUTHORIZED);
                return;
            }

            Claims claims = jwtUtil.parse(token);
            if (!SecurityConstants.TOKEN_TYPE_ACCESS.equals(claims.get(SecurityConstants.CLAIM_TYPE))) {
                // refresh token 不能直接访问业务接口
                writeUnauthorized(response, ResultCode.UNAUTHORIZED);
                return;
            }

            // 已登出的 access token 落在黑名单里，TTL 到令牌自然过期为止
            if (cacheClient.hasKey(CacheKeys.AUTH_BLACKLIST + claims.getId())) {
                writeUnauthorized(response, ResultCode.UNAUTHORIZED);
                return;
            }

            LoginUserLoader loader = loaderProvider.getIfAvailable();
            if (loader == null) {
                log.error("LoginUserLoader 未装配：缺少 auth 模块的实现");
                writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        Result.fail(ResultCode.ERROR.getCode(), "系统未装配登录加载器"));
                return;
            }

            LoginUser loginUser = loader.load(Long.valueOf(claims.getSubject()));
            if (loginUser == null) {
                // 用户被删除或已停用：即使令牌没过期也不放行
                writeUnauthorized(response, ResultCode.UNAUTHORIZED);
                return;
            }

            UserContextHolder.set(loginUser);
            chain.doFilter(request, response);
        } catch (UnauthorizedException e) {
            writeUnauthorized(response, e.getResultCode());
        } finally {
            // 必须清理：Tomcat 线程池复用，残留上下文会把身份串给下一个请求
            UserContextHolder.clear();
        }
    }

    private boolean isWhitelist(HttpServletRequest request) {
        // 跨域预检请求不带 token，直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = stripContextPath(request);
        for (String pattern : securityProperties.getWhitelist()) {
            if (MATCHER.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }

    /** 去掉 context-path(/api)，让白名单配置与 Controller 路径写法保持一致 */
    public static String stripContextPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        return uri;
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(SecurityConstants.AUTH_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(SecurityConstants.BEARER_PREFIX)) {
            return header.substring(SecurityConstants.BEARER_PREFIX.length()).trim();
        }
        return null;
    }

    private void writeUnauthorized(HttpServletResponse response, ResultCode resultCode) throws IOException {
        writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, Result.fail(resultCode));
    }

    private void writeJson(HttpServletResponse response, int status, Result<Void> result) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JsonUtil.toJson(result));
        response.getWriter().flush();
    }
}