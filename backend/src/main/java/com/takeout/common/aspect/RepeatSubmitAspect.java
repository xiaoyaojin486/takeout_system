package com.takeout.common.aspect;

import com.takeout.common.annotation.RepeatSubmit;
import com.takeout.common.cache.CacheClient;
import com.takeout.common.constant.CacheKeys;
import com.takeout.common.context.UserContextHolder;
import com.takeout.common.exception.BusinessException;
import com.takeout.common.result.ResultCode;
import com.takeout.common.util.JsonUtil;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 防重复提交切面：同一用户 + 同一接口 + 同一参数，在窗口期内只放行一次
 *
 * <p>
 * 实现要点：不缓存请求体、不包 RequestWrapper，直接对方法参数做 JSON 指纹，
 * 因此对 Controller 无侵入，也不会与文件流类接口冲突。
 * </p>
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RepeatSubmitAspect {

    private final CacheClient cacheClient;

    @Before("@annotation(repeatSubmit)")
    public void before(JoinPoint joinPoint, RepeatSubmit repeatSubmit) {
        String fingerprint = buildFingerprint(joinPoint);
        String key = CacheKeys.REPEAT_SUBMIT + fingerprint;

        if (cacheClient.hasKey(key)) {
            throw new BusinessException(ResultCode.REPEAT_SUBMIT,
                    repeatSubmit.message().isEmpty() ? ResultCode.REPEAT_SUBMIT.getMessage() : repeatSubmit.message());
        }
        // 先占位再执行：窗口期内后续同参数请求会被拦截
        cacheClient.set(key, "1", repeatSubmit.interval());
    }

    /**
     * 指纹 = 用户 + 方法签名 + 可序列化参数
     *
     * <p>
     * 注意排除 ServletRequest/Response/MultipartFile：它们无法序列化，
     * 且其 toString 不稳定（含对象地址），会让指纹每次都不一样，导致防重失效。
     * </p>
     */
    static String buildFingerprint(JoinPoint joinPoint) {
        Long userId = UserContextHolder.getUserIdOrNull();
        String who = userId == null ? "anon" : String.valueOf(userId);
        String signature = joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "." + joinPoint.getSignature().getName();

        List<String> parts = new ArrayList<>();
        parts.add(who);
        parts.add(signature);
        for (Object arg : joinPoint.getArgs()) {
            if (arg == null
                    || arg instanceof ServletRequest
                    || arg instanceof ServletResponse
                    || arg instanceof MultipartFile) {
                continue;
            }
            parts.add(JsonUtil.toJson(arg));
        }
        return AspectUtils.fingerprint("repeat", parts.toArray(new String[0]));
    }
}