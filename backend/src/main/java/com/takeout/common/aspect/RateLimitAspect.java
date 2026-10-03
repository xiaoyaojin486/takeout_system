package com.takeout.common.aspect;

import com.takeout.common.annotation.RateLimit;
import com.takeout.common.cache.CacheClient;
import com.takeout.common.constant.CacheKeys;
import com.takeout.common.context.UserContextHolder;
import com.takeout.common.exception.BusinessException;
import com.takeout.common.result.ResultCode;
import com.takeout.common.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 限流切面：固定窗口计数，超过阈值抛 RATE_LIMITED
 *
 * <p>计数走 CacheClient，因此 local 与 redis 两套实现下行为一致；
 * 多实例部署时必须用 redis，否则每个实例各算各的，实际阈值会翻倍。</p>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final CacheClient cacheClient;

    @Before("@annotation(rateLimit)")
    public void before(JoinPoint joinPoint, RateLimit rateLimit) {
        String keySuffix = rateLimit.key().isEmpty()
                ? joinPoint.getSignature().getDeclaringType().getSimpleName() + "." + joinPoint.getSignature().getName()
                : rateLimit.key();

        String identity;
        if (rateLimit.type() == RateLimit.LimitType.IP) {
            identity = resolveIp();
        } else {
            Long userId = UserContextHolder.getUserIdOrNull();
            // 已登录按用户；未登录（如登录接口误配 USER）退化为按 IP，避免全局共用一个计数器
            identity = userId == null ? resolveIp() : String.valueOf(userId);
        }

        String key = CacheKeys.RATE_LIMIT + keySuffix + ":" + identity;
        long count = cacheClient.increment(key, rateLimit.time());
        if (count > rateLimit.count()) {
            log.warn("触发限流 key={} 当前次数={} 阈值={}/{}s", key, count, rateLimit.count(), rateLimit.time());
            throw new BusinessException(ResultCode.RATE_LIMITED);
        }
    }

    private String resolveIp() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        HttpServletRequest request = attributes.getRequest();
        return "ip:" + IpUtil.getClientIp(request);
    }
}