package com.takeout.merchant.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 店铺 VO（返回前端）
 */
@Data
@Schema(description = "店铺信息")
public class MerchantVO implements Serializable {

    private Long id;

    private String name;

    private String logo;

    private String description;

    private String phone;

    private String address;

    private BigDecimal minAmount;

    private BigDecimal deliveryFee;

    @Schema(description = "1 营业中 / 0 休息中")
    private Integer businessStatus;

    @Schema(description = "营业状态描述")
    private String businessStatusDesc;

    @Schema(description = "0 待审核 / 1 已通过 / 2 已驳回")
    private Integer auditStatus;

    @Schema(description = "审核状态描述")
    private String auditStatusDesc;

    private String auditRemark;

    private BigDecimal rating;

    private Integer sales;

    @Schema(description = "店主账号 id")
    private Long ownerUserId;

    @Schema(description = "店主登录账号")
    private String ownerUsername;

    private LocalDateTime createTime;
}