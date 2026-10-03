package com.takeout.cart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.takeout.cart.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 购物车 Mapper
 */
@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {
}