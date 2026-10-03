package com.takeout.cart.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.takeout.cart.dto.CartAddDTO;
import com.takeout.cart.entity.CartItem;
import com.takeout.cart.mapper.CartItemMapper;
import com.takeout.cart.service.CartService;
import com.takeout.cart.vo.CartItemVO;
import com.takeout.cart.vo.CartShopGroupVO;
import com.takeout.common.context.UserContextHolder;
import com.takeout.common.exception.BusinessException;
import com.takeout.common.exception.ForbiddenException;
import com.takeout.common.result.ResultCode;
import com.takeout.dish.api.DishApi;
import com.takeout.dish.api.vo.DishSimpleVO;
import com.takeout.merchant.api.MerchantApi;
import com.takeout.merchant.api.vo.MerchantSimpleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 购物车服务实现
 *
 * <p>
 * <b>跨模块只读契约</b>：菜品信息走 DishApi、店铺信息走 MerchantApi，
 * 不直接查 dish / merchant 表 —— 这是模块单体化架构的核心约束。
 * </p>
 *
 * <p>
 * <b>金额口径</b>：购物车里所有价格都在查询时实时读菜品表计算，
 * 不落库、不缓存，保证「购物车显示的金额 = 下单那一刻的金额」。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartItemMapper cartItemMapper;
    private final DishApi dishApi;
    private final MerchantApi merchantApi;

    @Override
    public List<CartShopGroupVO> list() {
        Long userId = UserContextHolder.getUserId();
        List<CartItem> items = listRaw(userId);
        if (items.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量取菜品与店铺：避免在循环里逐个查询（N+1）
        Set<Long> dishIds = items.stream().map(CartItem::getDishId).collect(Collectors.toSet());
        Map<Long, DishSimpleVO> dishMap = dishApi.listSimpleByIds(dishIds).stream()
                .collect(Collectors.toMap(DishSimpleVO::getDishId, d -> d, (a, b) -> a));

        Set<Long> merchantIds = items.stream().map(CartItem::getMerchantId).collect(Collectors.toSet());
        Map<Long, MerchantSimpleVO> merchantMap = merchantApi.listSimpleByIds(merchantIds).stream()
                .collect(Collectors.toMap(MerchantSimpleVO::getMerchantId, m -> m, (a, b) -> a));

        // 按店铺分组，保持加入顺序
        Map<Long, List<CartItemVO>> grouped = new LinkedHashMap<>();
        for (CartItem item : items) {
            DishSimpleVO dish = dishMap.get(item.getDishId());
            if (dish == null) {
                // 菜品已被删除（逻辑删除后查不到）：跳过展示
                continue;
            }
            grouped.computeIfAbsent(item.getMerchantId(), k -> new ArrayList<>()).add(toItemVO(item, dish));
        }

        List<CartShopGroupVO> result = new ArrayList<>();
        for (Map.Entry<Long, List<CartItemVO>> entry : grouped.entrySet()) {
            MerchantSimpleVO merchant = merchantMap.get(entry.getKey());
            if (merchant == null) {
                continue;
            }
            result.add(buildGroup(merchant, entry.getValue()));
        }
        // 营业中的店铺排前面，方便用户先结算可下单的
        result.sort(Comparator.comparing(g -> Integer.valueOf(1).equals(g.getBusinessStatus()) ? 0 : 1));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(CartAddDTO dto) {
        Long userId = UserContextHolder.getUserId();

        DishSimpleVO dish = dishApi.getSimpleById(dto.getDishId());
        if (dish == null) {
            throw new BusinessException(ResultCode.DISH_NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(dish.getStatus())) {
            throw new BusinessException(ResultCode.DISH_OFF_SHELF);
        }
        // 店铺没通过审核时不允许加购，否则用户白填一车菜却下不了单
        MerchantSimpleVO merchant = merchantApi.getSimpleById(dish.getMerchantId());
        if (merchant == null || !Integer.valueOf(1).equals(merchant.getAuditStatus())) {
            throw new BusinessException(ResultCode.MERCHANT_NOT_AUDITED);
        }

        CartItem exist = cartItemMapper.selectOne(Wrappers.<CartItem>lambdaQuery()
                .select(CartItem::getId, CartItem::getQuantity)
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getDishId, dto.getDishId()));

        int targetQuantity = dto.getQuantity() + (exist == null ? 0 : exist.getQuantity());
        requireStock(dish, targetQuantity);

        if (exist == null) {
            CartItem entity = new CartItem();
            entity.setUserId(userId);
            entity.setMerchantId(dish.getMerchantId());
            entity.setDishId(dto.getDishId());
            entity.setQuantity(targetQuantity);
            cartItemMapper.insert(entity);
        } else {
            CartItem update = new CartItem();
            update.setId(exist.getId());
            update.setQuantity(targetQuantity);
            cartItemMapper.updateById(update);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQuantity(Long id, Integer quantity) {
        CartItem exist = requireOwned(id);

        if (quantity == null || quantity <= 0) {
            cartItemMapper.deleteById(id);
            return;
        }
        DishSimpleVO dish = dishApi.getSimpleById(exist.getDishId());
        if (dish == null) {
            throw new BusinessException(ResultCode.DISH_NOT_FOUND);
        }
        requireStock(dish, quantity);

        CartItem update = new CartItem();
        update.setId(id);
        update.setQuantity(quantity);
        cartItemMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        requireOwned(id);
        cartItemMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clear() {
        Long userId = UserContextHolder.getUserId();
        cartItemMapper.delete(Wrappers.<CartItem>lambdaQuery().eq(CartItem::getUserId, userId));
    }

    @Override
    public Integer count() {
        Long userId = UserContextHolder.getUserId();
        return listRaw(userId).stream().mapToInt(CartItem::getQuantity).sum();
    }

    // ==================== 内部方法 ====================

    private List<CartItem> listRaw(Long userId) {
        return cartItemMapper.selectList(Wrappers.<CartItem>lambdaQuery()
                .select(CartItem::getId, CartItem::getUserId, CartItem::getMerchantId,
                        CartItem::getDishId, CartItem::getQuantity)
                .eq(CartItem::getUserId, userId)
                .orderByDesc(CartItem::getCreateTime));
    }

    /** 归属校验：购物车行只能被本人操作 */
    private CartItem requireOwned(Long id) {
        CartItem item = cartItemMapper.selectOne(Wrappers.<CartItem>lambdaQuery()
                .select(CartItem::getId, CartItem::getUserId, CartItem::getDishId)
                .eq(CartItem::getId, id));
        if (item == null) {
            throw new BusinessException(ResultCode.CART_ITEM_NOT_FOUND);
        }
        Long userId = UserContextHolder.getUserId();
        if (!userId.equals(item.getUserId())) {
            throw new ForbiddenException(ResultCode.FORBIDDEN, "无权操作他人购物车");
        }
        return item;
    }

    private void requireStock(DishSimpleVO dish, int quantity) {
        if (dish.getStock() == null || dish.getStock() < quantity) {
            throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH,
                    "「" + dish.getName() + "」库存不足，当前仅剩 " + (dish.getStock() == null ? 0 : dish.getStock()) + " 份");
        }
    }

    private CartItemVO toItemVO(CartItem item, DishSimpleVO dish) {
        CartItemVO vo = new CartItemVO();
        vo.setId(item.getId());
        vo.setDishId(dish.getDishId());
        vo.setDishName(dish.getName());
        vo.setDishImage(dish.getImage());
        vo.setPrice(dish.getPrice());
        vo.setQuantity(item.getQuantity());
        vo.setAmount(dish.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        vo.setStock(dish.getStock());
        vo.setAvailable(Integer.valueOf(1).equals(dish.getStatus()));
        return vo;
    }

    private CartShopGroupVO buildGroup(MerchantSimpleVO merchant, List<CartItemVO> items) {
        BigDecimal totalAmount = items.stream()
                .map(CartItemVO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalQuantity = items.stream().mapToInt(CartItemVO::getQuantity).sum();

        CartShopGroupVO group = new CartShopGroupVO();
        group.setMerchantId(merchant.getMerchantId());
        group.setMerchantName(merchant.getName());
        group.setMerchantLogo(merchant.getLogo());
        group.setBusinessStatus(merchant.getBusinessStatus());
        group.setMinAmount(merchant.getMinAmount());
        group.setDeliveryFee(merchant.getDeliveryFee());
        group.setTotalAmount(totalAmount);
        group.setTotalQuantity(totalQuantity);
        group.setReachMinAmount(merchant.getMinAmount() == null
                || totalAmount.compareTo(merchant.getMinAmount()) >= 0);
        group.setItems(items);
        return group;
    }
}