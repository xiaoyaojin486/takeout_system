package com.takeout.dish.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.takeout.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 菜品实体
 *
 * <p><b>只在 Service / Mapper 内部流转，永不直接返回前端</b>——对外统一走 DishVO。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dish")
public class Dish extends BaseEntity {

    /** 所属店铺，商家数据权限的归属字段 */
    private Long merchantId;

    /** 所属分类 */
    private Long categoryId;

    private String name;

    private String image;

    private String description;

    /** 单价 */
    private BigDecimal price;

    /** 库存（下单原子扣减，取消归还） */
    private Integer stock;

    /** 销量（订单完成时累加） */
    private Integer sales;

    /** 1 上架 / 0 下架 */
    private Integer status;
}