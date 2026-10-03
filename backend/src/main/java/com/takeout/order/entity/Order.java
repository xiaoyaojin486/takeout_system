package com.takeout.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.takeout.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主表实体
 *
 * <p><b>表名为什么是 orders</b>：order 是 SQL 保留字，用作表名会导致所有 SQL 都要加反引号，
 * 直接取名 orders 更省事。</p>
 *
 * <p><b>地址字段是快照</b>：下单那一刻从 address 表复制过来，之后用户改地址、删地址
 * 都不会影响历史订单，这对「订单可追溯」是必须的。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("orders")
public class Order extends BaseEntity {

    private String orderNo;

    private Long userId;

    private Long merchantId;

    private String receiver;

    private String receiverPhone;

    private String receiverAddress;

    /** 菜品总额 */
    private BigDecimal totalAmount;

    private BigDecimal deliveryFee;

    /** 实付 = 菜品总额 + 配送费 */
    private BigDecimal payAmount;

    /** 10 待支付 / 20 待接单 / 30 备餐中 / 40 配送中 / 50 已完成 / 60 已取消 / 70 已退款 */
    private Integer status;

    private String remark;

    private String cancelReason;

    private LocalDateTime payTime;

    private LocalDateTime acceptTime;

    private LocalDateTime finishTime;

    private LocalDateTime cancelTime;
}