package com.takeout.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单明细 VO（展示快照值，不受菜品后续改动影响）
 */
@Data
@Schema(description = "订单明细")
public class OrderItemVO implements Serializable {

    private Long dishId;

    /** 下单时的菜名快照 */
    private String dishName;

    /** 下单时的图片快照 */
    private String dishImage;

    /** 下单时的单价快照 */
    private BigDecimal price;

    private Integer quantity;

    private BigDecimal amount;
}