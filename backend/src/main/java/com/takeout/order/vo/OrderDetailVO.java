package com.takeout.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 订单详情 VO：订单信息 + 明细 + 状态时间轴
 */
@Data
@Schema(description = "订单详情")
public class OrderDetailVO implements Serializable {

    private OrderVO order;

    @Schema(description = "状态流转时间轴，按时间正序")
    private List<OrderStatusLogVO> statusLogs;
}