package com.takeout.cart.api.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.takeout.cart.api.CartApi;
import com.takeout.cart.entity.CartItem;
import com.takeout.cart.mapper.CartItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * 购物车契约实现
 */
@Service
@RequiredArgsConstructor
public class CartApiImpl implements CartApi {

    private final CartItemMapper cartItemMapper;

    @Override
    public void removeByUserIdAndDishIds(Long userId, Collection<Long> dishIds) {
        if (userId == null || dishIds == null || dishIds.isEmpty()) {
            return;
        }
        cartItemMapper.delete(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId)
                .in(CartItem::getDishId, dishIds));
    }
}