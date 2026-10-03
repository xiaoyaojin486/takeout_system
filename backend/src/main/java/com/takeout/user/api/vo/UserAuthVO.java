package com.takeout.user.api.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 账号认证信息（跨模块契约 VO）
 *
 * <p><b>含密码密文</b>，只允许 auth 模块在登录校验时使用，
 * 严禁放入任何返回前端的响应体。</p>
 */
@Data
@Builder
public class UserAuthVO implements Serializable {

    private Long userId;

    private String username;

    /** BCrypt 密文，供 auth 模块 matches() 校验 */
    private String password;

    private String nickname;

    private String avatar;

    private String phone;

    /** 1 顾客 / 2 商家 / 3 管理员 */
    private Integer userType;

    /** 1 正常 / 0 停用 */
    private Integer status;
}