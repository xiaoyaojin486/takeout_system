package com.takeout.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态变更的操作方：写进 order_status_log，用于回答「是谁把订单改成这个状态的」
 */
@Getter
@AllArgsConstructor
public enum OrderOperatorEnum {

    CUSTOMER(1, "顾客"),
    MERCHANT(2, "商家"),
    SYSTEM(3, "系统"),
    ADMIN(4, "管理员");

    private final Integer code;
    private final String desc;

    public static OrderOperatorEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (OrderOperatorEnum item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return null;
    }
}