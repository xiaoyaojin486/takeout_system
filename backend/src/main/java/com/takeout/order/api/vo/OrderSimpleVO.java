package com.takeout.order.api.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单简要信息（跨模块契约 VO）
 */
@Data
@Builder
public class OrderSimpleVO implements Serializable {

    private Long orderId;

    private String orderNo;

    private Long userId;

    private Long merchantId;

    private BigDecimal payAmount;

    /** 10 待支付 / 20 待接单 / 30 备餐中 / 40 配送中 / 50 已完成 / 60 已取消 / 70 已退款 */
    private Integer status;

    private String receiver;

    private String receiverPhone;

    private String receiverAddress;
}