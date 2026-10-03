package com.takeout.dish.dto;

import com.takeout.common.page.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜品分页查询条件（商家端）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "菜品查询条件")
public class DishQuery extends PageQuery {

    @Schema(description = "关键词：菜品名模糊匹配")
    private String keyword;

    @Schema(description = "所属分类 id")
    private Long categoryId;

    @Schema(description = "1 上架 / 0 下架")
    private Integer status;
}