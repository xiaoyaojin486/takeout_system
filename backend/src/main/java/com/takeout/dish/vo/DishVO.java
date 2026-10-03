package com.takeout.dish.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 菜品 VO（返回前端）
 */
@Data
@Schema(description = "菜品信息")
public class DishVO implements Serializable {

    private Long id;

    private Long categoryId;

    @Schema(description = "分类名称（由 Service 跨模块补全）")
    private String categoryName;

    private Long merchantId;

    private String name;

    private String image;

    private String description;

    private BigDecimal price;

    private Integer stock;

    private Integer sales;

    @Schema(description = "1 上架 / 0 下架")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;

    private LocalDateTime createTime;
}