package com.takeout.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单 VO（列表与详情共用）
 */
@Data
@Schema(description = "订单信息")
public class OrderVO implements Serializable {

    private Long id;

    private String orderNo;

    private Long userId;

    private Long merchantId;

    /** 店铺名称（跨模块取，不在 orders 表冗余） */
    private String merchantName;

    private String receiver;

    private String receiverPhone;

    private String receiverAddress;

    private BigDecimal totalAmount;

    private BigDecimal deliveryFee;

    private BigDecimal payAmount;

    @Schema(description = "10 待支付/20 待接单/30 备餐中/40 配送中/50 已完成/60 已取消/70 已退款")
    private Integer status;

    private String statusDesc;

    private String remark;

    private String cancelReason;

    private LocalDateTime payTime;

    private LocalDateTime acceptTime;

    private LocalDateTime finishTime;

    private LocalDateTime cancelTime;

    private LocalDateTime createTime;

    /** 菜品总件数 */
    private Integer itemCount;

    private List<OrderItemVO> items;
}