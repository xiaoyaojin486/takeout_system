package com.takeout.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流：固定窗口计数，命中上限直接拒绝
 *
 * <p>典型用法：登录接口按 IP 限流（抗暴力破解），下单接口按用户限流（抗脚本刷单）。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 窗口内允许的最大次数 */
    int count() default 10;

    /** 窗口长度（秒） */
    int time() default 60;

    /** 自定义 key 后缀，为空时用「类名.方法名」 */
    String key() default "";

    /** 限流维度 */
    LimitType type() default LimitType.USER;

    enum LimitType {
        /** 按客户端 IP（未登录场景，如登录接口） */
        IP,
        /** 按当前登录用户（已登录场景） */
        USER
    }
}