package com.takeout.category.api.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 分类简要信息（跨模块契约 VO）
 */
@Data
@Builder
public class CategorySimpleVO implements Serializable {

    private Long categoryId;

    private Long merchantId;

    private String name;

    private Integer sort;

    /** 1 启用 / 0 禁用 */
    private Integer status;
}