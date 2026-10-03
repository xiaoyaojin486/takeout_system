package com.takeout.cart.service;

import com.takeout.cart.dto.CartAddDTO;
import com.takeout.cart.vo.CartShopGroupVO;

import java.util.List;

/**
 * 购物车服务
 */
public interface CartService {

    /** 我的购物车（按店铺分组，价格实时读菜品表） */
    List<CartShopGroupVO> list();

    /** 加入购物车：已存在则累加数量 */
    void add(CartAddDTO dto);

    /** 修改数量（传 0 视为移除） */
    void updateQuantity(Long id, Integer quantity);

    /** 移除单个菜品 */
    void remove(Long id);

    /** 清空我的购物车 */
    void clear();

    /** 购物车菜品总件数（H5 角标用） */
    Integer count();
}