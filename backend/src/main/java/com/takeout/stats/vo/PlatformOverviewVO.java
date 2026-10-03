package com.takeout.stats.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 平台看板概览
 */
@Data
@Schema(description = "平台运营概览")
public class PlatformOverviewVO implements Serializable {

    @Schema(description = "顾客账号总数")
    private Long customerCount;

    @Schema(description = "已通过审核的店铺数")
    private Long merchantCount;

    @Schema(description = "待审核店铺数")
    private Long pendingAuditCount;

    private Integer todayOrderCount;

    private BigDecimal todayAmount;

    @Schema(description = "累计完成订单数")
    private Integer totalOrderCount;

    @Schema(description = "累计成交额")
    private BigDecimal totalAmount;

    @Schema(description = "区间内取消订单数（此处为累计）")
    private Integer canceledCount;
}