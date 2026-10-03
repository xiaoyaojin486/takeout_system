package com.takeout.common.context;

import com.takeout.common.exception.UnauthorizedException;
import com.takeout.common.result.ResultCode;

/**
 * 登录用户 ThreadLocal 持有器
 *
 * <p><b>生命周期</b>：AuthenticationFilter 鉴权通过后 set，请求结束在 finally 中 clear。
 * clear 不能省，否则 Tomcat 线程复用会把 A 用户的身份串给 B 用户。</p>
 */
public final class UserContextHolder {

    private UserContextHolder() {
    }

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser loginUser) {
        HOLDER.set(loginUser);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    /** 取当前登录用户，未登录直接抛 401（用于必须登录的 Service 方法） */
    public static LoginUser requireLogin() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw new UnauthorizedException(ResultCode.UNAUTHORIZED);
        }
        return user;
    }

    /** 当前登录用户 id：数据归属校验的唯一可信来源 */
    public static Long getUserId() {
        return requireLogin().getUserId();
    }

    /** 当前登录用户 id，未登录返回 null（用于可选登录场景） */
    public static Long getUserIdOrNull() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getUserId();
    }

    public static Integer getUserType() {
        return requireLogin().getUserType();
    }

    public static void clear() {
        HOLDER.remove();
    }
}