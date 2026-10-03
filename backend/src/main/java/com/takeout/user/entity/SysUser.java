package com.takeout.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.takeout.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 统一账号实体（三端共用）
 *
 * <p><b>只在 Service / Mapper 内部流转，永不直接返回前端</b>——password 字段尤其如此。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    /** 登录账号，全局唯一 */
    private String username;

    /** BCrypt 密文，禁止明文赋值 */
    private String password;

    private String phone;

    private String nickname;

    private String avatar;

    /** 1 顾客 / 2 商家 / 3 管理员 */
    private Integer userType;

    /** 1 正常 / 0 停用 */
    private Integer status;

    private LocalDateTime lastLoginTime;
}