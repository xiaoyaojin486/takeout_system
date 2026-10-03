package com.takeout.cart.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车按店铺分组的 VO
 *
 * <p>外卖的结算单位是「店铺」：一个订单只能属于一个商家，
 * 因此购物车按店铺分组展示，每组独立显示金额并独立下单。</p>
 */
@Data
@Schema(description = "购物车（按店铺分组）")
public class CartShopGroupVO implements Serializable {

    private Long merchantId;

    private String merchantName;

    private String merchantLogo;

    @Schema(description = "1 营业中 / 0 休息中")
    private Integer businessStatus;

    /** 起送价 */
    private BigDecimal minAmount;

    private BigDecimal deliveryFee;

    /** 菜品总额 */
    private BigDecimal totalAmount;

    /** 菜品总件数 */
    private Integer totalQuantity;

    @Schema(description = "是否达到起送价，前端据此禁用结算按钮")
    private Boolean reachMinAmount;

    private List<CartItemVO> items;
}