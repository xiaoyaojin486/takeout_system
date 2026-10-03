package com.takeout.order.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商家维度订单统计（跨模块契约 VO，供 stats 模块使用）
 */
@Data
public class MerchantOrderStatVO implements Serializable {

    /** 区间内完成订单数 */
    private Integer orderCount;

    /** 区间内营业额（口径：已完成订单的实付金额之和） */
    private BigDecimal salesAmount;

    /** 当前待接单数（不受时间区间限制） */
    private Integer pendingAcceptCount;

    /** 当前待支付数（不受时间区间限制） */
    private Integer pendingPaymentCount;
}