package com.takeout.dish.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 按分类分组的菜单（顾客端菜品列表）
 *
 * <p>前端据此渲染「左侧分类锚点 + 右侧菜品列表」，无需自己再分组。</p>
 */
@Data
@Schema(description = "按分类分组的菜单")
public class DishGroupVO implements Serializable {

    private Long categoryId;

    private String categoryName;

    @Schema(description = "该分类下的上架菜品")
    private List<DishVO> dishes;
}