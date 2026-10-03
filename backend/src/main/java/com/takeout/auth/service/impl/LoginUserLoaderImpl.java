package com.takeout.auth.service.impl;

import com.takeout.common.context.LoginUser;
import com.takeout.common.context.LoginUserLoader;
import com.takeout.user.api.UserApi;
import com.takeout.user.api.vo.UserAuthVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 登录用户加载器实现（common 定义的 SPI）
 *
 * <p>存在的意义：common 模块不能依赖业务模块，却又需要在鉴权后拿到完整用户信息。
 * 由 auth 模块实现该接口，Spring 通过 ObjectProvider 注入给过滤器，
 * 这样 common 保持零业务依赖，依赖方向也不会出现环路。</p>
 */
@Component
@RequiredArgsConstructor
public class LoginUserLoaderImpl implements LoginUserLoader {

    private final UserApi userApi;

    @Override
    public LoginUser load(Long userId) {
        UserAuthVO user = userApi.findAuthById(userId);
        // 用户不存在或已停用 → 返回 null，过滤器据此回 401（令牌未过期也不放行）
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            return null;
        }
        return LoginUser.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .userType(user.getUserType())
                .build();
    }
}