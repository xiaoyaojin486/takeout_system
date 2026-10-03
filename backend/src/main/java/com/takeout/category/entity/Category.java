package com.takeout.category.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.takeout.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜品分类实体
 *
 * <p><b>只在 Service / Mapper 内部流转，永不直接返回前端</b>——对外统一走 CategoryVO。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("category")
public class Category extends BaseEntity {

    /** 所属店铺，商家数据权限的归属字段 */
    private Long merchantId;

    private String name;

    /** 排序值，越小越靠前 */
    private Integer sort;

    /** 1 启用 / 0 禁用 */
    private Integer status;
}