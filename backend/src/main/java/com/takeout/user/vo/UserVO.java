package com.takeout.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 账号 VO（返回前端）
 *
 * <p><b>刻意不含 password 字段</b>：即使上游 Entity 带了密文，也无法被序列化出去。</p>
 */
@Data
@Schema(description = "账号信息")
public class UserVO implements Serializable {

    private Long id;

    private String username;

    private String phone;

    private String nickname;

    private String avatar;

    @Schema(description = "1 顾客 / 2 商家 / 3 管理员")
    private Integer userType;

    @Schema(description = "用户类型描述")
    private String userTypeDesc;

    @Schema(description = "1 正常 / 0 停用")
    private Integer status;

    private LocalDateTime lastLoginTime;

    private LocalDateTime createTime;
}