package com.takeout.cart.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 购物车行 VO
 */
@Data
@Schema(description = "购物车条目")
public class CartItemVO implements Serializable {

    /** 购物车行 id，改数量/删除时用 */
    private Long id;

    private Long dishId;

    private String dishName;

    private String dishImage;

    /** 实时单价（来自 dish 表，不是快照） */
    private BigDecimal price;

    private Integer quantity;

    /** 小计 = 单价 × 数量（后端计算，前端不自己算钱） */
    private BigDecimal amount;

    /** 菜品当前库存，前端据此禁用「+」按钮 */
    private Integer stock;

    @Schema(description = "菜品是否仍在上架：false 时前端置灰并提示")
    private Boolean available;
}