package com.takeout.cart.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 购物车行实体
 *
 * <p>
 * <b>刻意不继承 BaseEntity</b>，因为 BaseEntity 带 delFlag 字段，
 * 而 MyBatis-Plus 会把「实体有 delFlag」的删除操作改写成逻辑删除（UPDATE del_flag = 1）。
 * 购物车行有唯一索引 {@code uk_user_dish(user_id, dish_id)}，逻辑删除后行仍在表里，
 * 于是「移除菜品后再加回同一菜品」会插入一条新行并撞上唯一键，
 * 直接抛 DuplicateKeyException（表现为 500）。
 * </p>
 *
 * <p>
 * 购物车是临时数据、不需要审计追溯，因此这里去掉 delFlag，
 * 让删除变成真正的物理删除 —— 这才是购物车该有的语义。
 * </p>
 *
 * <p>
 * 同理，<b>刻意不存价格快照</b>：购物车是「暂存意向」不是「成交凭证」，
 * 价格一律在查询/下单时实时读 dish 表，避免商家改价后购物车金额与实际下单金额不一致。
 * 真正的价格快照落在 order_item 上。
 * </p>
 */
@Data
@TableName("cart_item")
public class CartItem implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long merchantId;

    private Long dishId;

    private Integer quantity;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}