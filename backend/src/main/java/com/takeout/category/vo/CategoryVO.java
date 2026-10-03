package com.takeout.category.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 菜品分类 VO（返回前端）
 */
@Data
@Schema(description = "菜品分类信息")
public class CategoryVO implements Serializable {

    private Long id;

    private String name;

    private Integer sort;

    @Schema(description = "1 启用 / 0 禁用")
    private Integer status;

    /**
     * 分类下菜品数量
     *
     * <p>分类模块不能直接查 dish 表，跨模块契约 DishApi 也未提供计数方法，
     * 因此一期一律返回 null，由前端按需另行获取。</p>
     */
    @Schema(description = "分类下菜品数量（一期未实现，恒为 null）")
    private Integer dishCount;
}