package com.takeout.common.constant;

/**
 * 缓存 Key 前缀：local / redis 两套实现共用同一命名空间
 *
 * <p>命名规范：{@code 域:用途:{标识}}，便于 redis 下用 SCAN 排查问题。</p>
 */
public final class CacheKeys {

    private CacheKeys() {
    }

    /** 刷新令牌: auth:refresh:{jti} -> userId */
    public static final String AUTH_REFRESH = "auth:refresh:";

    /** 访问令牌黑名单: auth:blacklist:{jti} -> 1，TTL = 令牌剩余有效期 */
    public static final String AUTH_BLACKLIST = "auth:blacklist:";

    /** 登录失败计数: auth:login:fail:{username} */
    public static final String LOGIN_FAIL = "auth:login:fail:";

    /** 限流计数: rl:{key} */
    public static final String RATE_LIMIT = "rl:";

    /** 防重提交: rs:{key} */
    public static final String REPEAT_SUBMIT = "rs:";

    /** 幂等令牌: idem:{key} */
    public static final String IDEMPOTENT = "idem:";

    /** 业务序号: seq:{key}，用于生成订单号/支付单号 */
    public static final String SEQ = "seq:";
}