package com.takeout.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户类型：三端账号统一存 sys_user，用本字段区分身份
 *
 * <p>登录时校验 clientType 与 userType 一致，避免商家账号从顾客端登录。</p>
 */
@Getter
@AllArgsConstructor
public enum UserTypeEnum {

    /** 顾客：只能访问 /customer/** */
    CUSTOMER(1, "顾客"),

    /** 商家：只能访问 /merchant/** */
    MERCHANT(2, "商家"),

    /** 平台管理员：只能访问 /admin/** */
    ADMIN(3, "管理员");

    private final Integer code;
    private final String desc;

    public static UserTypeEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (UserTypeEnum item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return null;
    }
}