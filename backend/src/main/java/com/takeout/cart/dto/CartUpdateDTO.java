package com.takeout.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改购物车数量 DTO
 */
@Data
@Schema(description = "修改购物车数量入参")
public class CartUpdateDTO {

    @NotNull(message = "数量不能为空")
    @Min(value = 0, message = "数量不能为负")
    @Max(value = 99, message = "单个菜品数量不能超过 99")
    @Schema(description = "传 0 表示从购物车移除该菜品")
    private Integer quantity;
}