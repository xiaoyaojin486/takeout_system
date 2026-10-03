package com.takeout.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单明细实体
 *
 * <p>菜名/图片/单价全部<b>快照落库</b>：商家后续改名、改价、换图、下架甚至删除菜品，
 * 历史订单展示都不会变形。这是订单系统的基本要求，不能靠 join 现查。</p>
 */
@Data
@TableName("order_item")
public class OrderItem implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long dishId;

    /** 菜品名称（下单时快照） */
    private String dishName;

    /** 菜品图片（下单时快照） */
    private String dishImage;

    /** 下单时单价（快照） */
    private BigDecimal price;

    private Integer quantity;

    /** 小计 = 单价 × 数量 */
    private BigDecimal amount;
}