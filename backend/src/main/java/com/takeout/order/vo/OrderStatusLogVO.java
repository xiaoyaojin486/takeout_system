package com.takeout.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单状态日志 VO（订单详情页的状态时间轴）
 */
@Data
@Schema(description = "订单状态变更记录")
public class OrderStatusLogVO implements Serializable {

    private Integer fromStatus;

    private String fromStatusDesc;

    private Integer toStatus;

    private String toStatusDesc;

    @Schema(description = "1 顾客 / 2 商家 / 3 系统 / 4 管理员")
    private Integer operatorType;

    private String operatorTypeDesc;

    /** 取消原因 / 拒单原因 */
    private String remark;

    private LocalDateTime createTime;
}