package com.takeout.order.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 平台维度订单统计（跨模块契约 VO）
 */
@Data
public class PlatformOrderStatVO implements Serializable {

    /** 区间内完成订单数 */
    private Integer orderCount;

    /** 区间内成交额（已完成订单实付之和） */
    private BigDecimal salesAmount;

    /** 区间内取消订单数 */
    private Integer canceledCount;
}