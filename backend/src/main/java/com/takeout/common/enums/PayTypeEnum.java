package com.takeout.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付方式：一期只实现 SIMULATE，其余预留
 */
@Getter
@AllArgsConstructor
public enum PayTypeEnum {

    SIMULATE(1, "模拟支付"),
    WECHAT(2, "微信支付"),
    ALIPAY(3, "支付宝支付");

    private final Integer code;
    private final String desc;

    public static PayTypeEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (PayTypeEnum item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return null;
    }
}