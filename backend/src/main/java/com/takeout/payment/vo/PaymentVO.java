package com.takeout.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付单 VO
 */
@Data
@Schema(description = "支付信息")
public class PaymentVO implements Serializable {

    private Long id;

    private String payNo;

    private Long orderId;

    private BigDecimal amount;

    @Schema(description = "1 模拟 / 2 微信 / 3 支付宝")
    private Integer payType;

    private String payTypeDesc;

    @Schema(description = "0 待支付 / 1 已支付 / 2 已退款 / 3 已关闭")
    private Integer status;

    private String statusDesc;

    private String tradeNo;

    private LocalDateTime payTime;
}