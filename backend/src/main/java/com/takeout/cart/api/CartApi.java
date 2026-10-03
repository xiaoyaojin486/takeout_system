package com.takeout.cart.api;

import java.util.Collection;

/**
 * 购物车模块对外契约
 */
public interface CartApi {

    /**
     * 按用户 + 菜品集合移除购物车行
     *
     * <p>调用时机：下单成功后清掉本次已下单的菜品。
     * 只清这些菜品而不是清空整店，是为了保留用户在同一店铺里还没下单的其他菜品。</p>
     */
    void removeByUserIdAndDishIds(Long userId, Collection<Long> dishIds);
}