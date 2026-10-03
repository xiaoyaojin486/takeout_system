package com.takeout.auth.service;

import com.takeout.auth.dto.LoginDTO;
import com.takeout.auth.dto.LogoutDTO;
import com.takeout.auth.dto.RefreshTokenDTO;
import com.takeout.auth.vo.TokenVO;
import com.takeout.auth.vo.UserInfoVO;

/**
 * 认证服务：双 token 全生命周期
 */
public interface AuthService {

    /** 登录：校验密码 + 端身份，签发 access/refresh */
    TokenVO login(LoginDTO dto);

    /** 刷新：校验 refresh 有效性并旋转（旧 refresh 立即作废） */
    TokenVO refresh(RefreshTokenDTO dto);

    /** 登出：access 进黑名单 + 删除 refresh 记录 */
    void logout(String accessToken, LogoutDTO dto);

    /** 当前登录用户信息 */
    UserInfoVO userInfo();
}