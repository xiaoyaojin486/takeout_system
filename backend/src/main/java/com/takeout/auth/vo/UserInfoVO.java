package com.takeout.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 当前登录用户信息 VO（前端据此渲染用户名与菜单）
 */
@Data
@Builder
@Schema(description = "登录用户信息")
public class UserInfoVO implements Serializable {

    private Long userId;

    private String username;

    private String nickname;

    private String avatar;

    @Schema(description = "1 顾客 / 2 商家 / 3 管理员")
    private Integer userType;

    @Schema(description = "用户类型描述")
    private String userTypeDesc;
}