package com.takeout.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 下单结果 VO：前端拿到后直接跳支付页
 */
@Data
@Schema(description = "下单结果")
public class OrderCreateVO implements Serializable {

    private Long orderId;

    private String orderNo;

    @Schema(description = "实付金额")
    private BigDecimal payAmount;
}