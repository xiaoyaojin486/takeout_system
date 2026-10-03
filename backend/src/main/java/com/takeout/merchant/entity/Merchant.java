package com.takeout.merchant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.takeout.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 店铺实体
 *
 * <p><b>只在 Service / Mapper 内部流转，永不直接返回前端</b>——对外统一走 MerchantVO。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant")
public class Merchant extends BaseEntity {

    /** 店主账号 id（sys_user.id），商家数据权限的唯一归属字段 */
    private Long userId;

    private String name;

    private String logo;

    private String description;

    private String phone;

    private String address;

    /** 起送价 */
    private BigDecimal minAmount;

    /** 配送费 */
    private BigDecimal deliveryFee;

    /** 1 营业中 / 0 休息中 */
    private Integer businessStatus;

    /** 0 待审核 / 1 已通过 / 2 已驳回 */
    private Integer auditStatus;

    /** 审核意见 */
    private String auditRemark;

    /** 平均评分 */
    private BigDecimal rating;

    /** 累计销量（完成的订单数） */
    private Integer sales;
}