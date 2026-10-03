package com.takeout.dish.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 菜品入参（新增与编辑共用）
 */
@Data
@Schema(description = "菜品入参")
public class DishDTO {

    @NotNull(message = "所属分类不能为空")
    @Schema(description = "所属分类 id")
    private Long categoryId;

    @NotBlank(message = "菜品名称不能为空")
    @Size(max = 50, message = "菜品名称长度不能超过 50")
    @Schema(description = "菜品名称")
    private String name;

    @Size(max = 255, message = "菜品图片地址过长")
    @Schema(description = "菜品图片 URL")
    private String image;

    @Size(max = 255, message = "菜品描述过长")
    @Schema(description = "菜品描述")
    private String description;

    @NotNull(message = "单价不能为空")
    @DecimalMin(value = "0.01", message = "单价必须大于 0")
    @Schema(description = "单价")
    private BigDecimal price;

    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能为负数")
    @Schema(description = "库存")
    private Integer stock;

    @Schema(description = "1 上架 / 0 下架，留空默认上架")
    private Integer status;
}