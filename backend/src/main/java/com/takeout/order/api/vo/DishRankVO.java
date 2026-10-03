package com.takeout.order.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 菜品销量排行（跨模块契约 VO）
 */
@Data
public class DishRankVO implements Serializable {

    private Long dishId;

    private String dishName;

    /** 销量（份数） */
    private Integer quantity;

    /** 销售额 */
    private BigDecimal amount;
}