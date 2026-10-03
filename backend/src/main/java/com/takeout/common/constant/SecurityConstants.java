package com.takeout.common.constant;

/**
 * 安全相关常量
 */
public final class SecurityConstants {

    private SecurityConstants() {
    }

    /** 认证请求头 */
    public static final String AUTH_HEADER = "Authorization";

    /** Bearer 前缀（注意尾部空格） */
    public static final String BEARER_PREFIX = "Bearer ";

    /** claim：令牌类型 */
    public static final String CLAIM_TYPE = "type";

    /** claim：登录账号 */
    public static final String CLAIM_USERNAME = "username";

    /** claim：用户类型 1顾客 2商家 3管理员 */
    public static final String CLAIM_USER_TYPE = "ut";

    /** 访问令牌 */
    public static final String TOKEN_TYPE_ACCESS = "access";

    /** 刷新令牌 */
    public static final String TOKEN_TYPE_REFRESH = "refresh";
}