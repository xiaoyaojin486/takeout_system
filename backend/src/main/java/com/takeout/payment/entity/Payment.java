package com.takeout.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.takeout.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付单实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment")
public class Payment extends BaseEntity {

    private String payNo;

    private Long orderId;

    private Long userId;

    private BigDecimal amount;

    /** 1 模拟 / 2 微信 / 3 支付宝 */
    private Integer payType;

    /** 0 待支付 / 1 已支付 / 2 已退款 / 3 已关闭 */
    private Integer status;

    /** 第三方流水号；模拟支付为 MOCK 前缀 */
    private String tradeNo;

    private LocalDateTime payTime;
}