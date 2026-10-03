package com.takeout.dish.api.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 菜品简要信息（跨模块契约 VO）
 */
@Data
@Builder
public class DishSimpleVO implements Serializable {

    private Long dishId;

    private Long merchantId;

    private Long categoryId;

    private String name;

    private String image;

    private BigDecimal price;

    private Integer stock;

    private Integer sales;

    /** 1 上架 / 0 下架 */
    private Integer status;
}