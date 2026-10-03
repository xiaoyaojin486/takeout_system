package com.takeout.common.context;

/**
 * 登录用户加载 SPI
 *
 * <p>为什么要有这层：common 不依赖任何业务模块，但过滤器需要在鉴权后拿到完整的登录用户。
 * 于是 common 只定义接口，由业务模块（auth）实现，过滤器通过 ObjectProvider 拿到实现。
 * 这样 common 保持零业务依赖，同时避免「过滤器 → user 模块」的反向依赖。</p>
 */
public interface LoginUserLoader {

    /**
     * 按 userId 加载登录用户
     *
     * @return null 表示用户不存在或已停用，过滤器据此返回 401
     */
    LoginUser load(Long userId);
}