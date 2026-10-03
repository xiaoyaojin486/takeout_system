package com.takeout.order.dto;

import com.takeout.common.page.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 订单分页查询条件（三端共用）
 *
 * <p>顾客端/商家端只会用到 status 与 keyword；
 * merchantId 仅管理后台用来筛某个商家的订单。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "订单查询条件")
public class OrderQuery extends PageQuery {

    @Schema(description = "订单状态：10/20/30/40/50/60/70")
    private Integer status;

    @Schema(description = "关键词：订单号 / 收货人 模糊匹配")
    private String keyword;

    @Schema(description = "店铺 id（仅管理后台使用）")
    private Long merchantId;
}