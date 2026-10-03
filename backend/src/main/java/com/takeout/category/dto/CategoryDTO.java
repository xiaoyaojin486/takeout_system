package com.takeout.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 菜品分类入参（新增与编辑共用）
 */
@Data
@Schema(description = "菜品分类入参")
public class CategoryDTO {

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 30, message = "分类名称长度不能超过 30")
    @Schema(description = "分类名称")
    private String name;

    @NotNull(message = "排序值不能为空")
    @Schema(description = "排序值，越小越靠前")
    private Integer sort;

    @Schema(description = "1 启用 / 0 禁用，留空默认启用")
    private Integer status;
}