package com.takeout.merchant.api.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 店铺简要信息（跨模块契约 VO）
 *
 * <p>包含下单校验所需的关键字段：营业状态、审核状态、起送价、配送费。</p>
 */
@Data
@Builder
public class MerchantSimpleVO implements Serializable {

    private Long merchantId;

    /** 店主账号 id */
    private Long userId;

    private String name;

    private String logo;

    private String address;

    private String phone;

    /** 起送价 */
    private BigDecimal minAmount;

    /** 配送费 */
    private BigDecimal deliveryFee;

    /** 1 营业中 / 0 休息中 */
    private Integer businessStatus;

    /** 0 待审核 / 1 已通过 / 2 已驳回 */
    private Integer auditStatus;

    private BigDecimal rating;

    private Integer sales;
}