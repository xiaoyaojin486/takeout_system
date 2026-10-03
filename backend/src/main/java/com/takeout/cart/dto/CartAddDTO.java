package com.takeout.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 加入购物车 DTO
 */
@Data
@Schema(description = "加入购物车入参")
public class CartAddDTO {

    @NotNull(message = "菜品不能为空")
    private Long dishId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为 1")
    @Max(value = 99, message = "单个菜品数量不能超过 99")
    private Integer quantity;
}