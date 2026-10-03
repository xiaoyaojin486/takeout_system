package com.takeout.stats.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商家端工作台概览
 */
@Data
@Schema(description = "商家经营概览")
public class MerchantOverviewVO implements Serializable {

    private Long merchantId;

    private String merchantName;

    @Schema(description = "1 营业中 / 0 休息中")
    private Integer businessStatus;

    private Integer todayOrderCount;

    private BigDecimal todayAmount;

    private Integer monthOrderCount;

    private BigDecimal monthAmount;

    @Schema(description = "待接单数（实时，需尽快处理）")
    private Integer pendingAcceptCount;

    @Schema(description = "待支付数（实时）")
    private Integer pendingPaymentCount;
}