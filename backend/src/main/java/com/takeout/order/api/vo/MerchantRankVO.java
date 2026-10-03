package com.takeout.order.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商家成交排行（跨模块契约 VO）
 */
@Data
public class MerchantRankVO implements Serializable {

    private Long merchantId;

    /** 店铺名称：SQL 查不到，由 stats 模块通过 MerchantApi 补全 */
    private String merchantName;

    private Integer orderCount;

    private BigDecimal amount;
}