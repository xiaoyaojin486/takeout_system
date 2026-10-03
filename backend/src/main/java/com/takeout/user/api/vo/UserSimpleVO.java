package com.takeout.user.api.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 账号简要信息（跨模块契约 VO，不含密码等敏感字段）
 */
@Data
@Builder
public class UserSimpleVO implements Serializable {

    private Long userId;

    private String username;

    private String nickname;

    private String phone;
}