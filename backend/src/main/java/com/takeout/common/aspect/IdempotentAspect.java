package com.takeout.common.aspect;

import com.takeout.common.annotation.Idempotent;
import com.takeout.common.cache.CacheClient;
import com.takeout.common.constant.CacheKeys;
import com.takeout.common.exception.BusinessException;
import com.takeout.common.result.ResultCode;
import com.takeout.common.util.JsonUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 幂等切面：同一标记只允许成功执行一次；执行失败则释放标记，允许重试
 *
 * <p>典型用法：下单接口。前端每次点击「提交订单」生成一个 UUID 放进
 * {@code X-Idempotent-Token} 头，网络抖动重发时后端直接拦掉，不会产生两笔订单。</p>
 */
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {

    /** 前端可选的幂等标记请求头 */
    public static final String IDEMPOTENT_HEADER = "X-Idempotent-Token";

    private final CacheClient cacheClient;

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String key = buildKey(joinPoint, idempotent);

        if (cacheClient.hasKey(key)) {
            throw new BusinessException(ResultCode.IDEMPOTENT_PROCESSING);
        }
        // 先占位，防止并发重复进入
        cacheClient.set(key, "1", idempotent.ttl());

        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            // 失败必须释放：否则用户「余额不足 → 充值 → 重新下单」会被自己的失败记录挡住
            cacheClient.delete(key);
            throw e;
        }
    }

    private String buildKey(ProceedingJoinPoint joinPoint, Idempotent idempotent) {
        String prefix = idempotent.key().isEmpty() ? "idem" : idempotent.key();

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String token = request.getHeader(IDEMPOTENT_HEADER);
            if (StringUtils.hasText(token)) {
                return CacheKeys.IDEMPOTENT + prefix + ":" + token.trim();
            }
        }
        // 未携带幂等标记：退化为参数指纹（与防重提交同源，但窗口更长）
        return CacheKeys.IDEMPOTENT + prefix + ":" + RepeatSubmitAspect.buildFingerprint(joinPoint);
    }
}