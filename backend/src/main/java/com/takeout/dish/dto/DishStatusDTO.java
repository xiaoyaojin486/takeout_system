package com.takeout.dish.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 菜品上下架入参
 */
@Data
@Schema(description = "菜品上下架入参")
public class DishStatusDTO {

    @NotNull(message = "菜品状态不能为空")
    @Schema(description = "1 上架 / 0 下架")
    private Integer status;
}