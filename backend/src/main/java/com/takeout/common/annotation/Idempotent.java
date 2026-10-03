package com.takeout.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 幂等控制（长窗口去重 + 失败自动放行）
 *
 * <p>与 {@link RepeatSubmit} 的区别：</p>
 * <ul>
 *     <li>RepeatSubmit 是短窗口防连点，窗口过后自然放行；</li>
 *     <li>Idempotent 是长窗口幂等，<b>成功后标记保留</b>，同一请求重复提交一律拒绝；
 *         但<b>如果本次执行抛异常，标记会立即释放</b>，允许用户修正后重试。</li>
 * </ul>
 *
 * <p>key 优先使用请求头 {@code X-Idempotent-Token}（前端可生成一次性的 UUID），
 * 未携带时退化为「参数指纹」，效果与 RepeatSubmit 类似但窗口更长。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /** 标记保留时长（秒） */
    int ttl() default 300;

    /** 自定义 key 前缀 */
    String key() default "";
}