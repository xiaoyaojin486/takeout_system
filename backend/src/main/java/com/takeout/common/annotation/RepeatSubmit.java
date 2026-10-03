package com.takeout.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 防重复提交（短窗口去重）
 *
 * <p>key = 登录用户 + 请求方法 + 请求路径 + 参数指纹，窗口内重复请求直接拒绝。
 * 用于解决「手抖连点两次下单」这类问题。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RepeatSubmit {

    /** 去重窗口（秒），窗口结束自动放行 */
    int interval() default 3;

    /** 提示信息，为空时用 ResultCode.REPEAT_SUBMIT */
    String message() default "";
}