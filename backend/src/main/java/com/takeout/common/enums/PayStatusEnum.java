package com.takeout.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付状态
 */
@Getter
@AllArgsConstructor
public enum PayStatusEnum {

    UNPAID(0, "待支付"),
    PAID(1, "已支付"),
    REFUNDED(2, "已退款"),
    /** 订单已取消（超时/顾客取消/商家拒单）后，未支付的支付单作废 */
    CLOSED(3, "已关闭");

    private final Integer code;
    private final String desc;

    public static PayStatusEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (PayStatusEnum item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return null;
    }
}