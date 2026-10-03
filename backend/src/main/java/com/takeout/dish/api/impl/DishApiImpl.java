package com.takeout.dish.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.takeout.dish.api.DishApi;
import com.takeout.dish.api.vo.DishSimpleVO;
import com.takeout.dish.convert.DishConvert;
import com.takeout.dish.entity.Dish;
import com.takeout.dish.mapper.DishMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 菜品模块对外契约实现
 *
 * <p>库存三件套直接委托 Mapper 的原子 SQL；调用方需自行保证在事务内使用。</p>
 */
@Service
@RequiredArgsConstructor
public class DishApiImpl implements DishApi {

    private final DishMapper dishMapper;
    private final DishConvert dishConvert;

    @Override
    public DishSimpleVO getSimpleById(Long dishId) {
        if (dishId == null) {
            return null;
        }
        Dish dish = dishMapper.selectOne(selectSimpleColumns().eq(Dish::getId, dishId));
        return dish == null ? null : dishConvert.toSimpleVO(dish);
    }

    @Override
    public List<DishSimpleVO> listSimpleByIds(Collection<Long> dishIds) {
        if (dishIds == null || dishIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Dish> dishes = dishMapper.selectList(selectSimpleColumns().in(Dish::getId, dishIds));
        return dishConvert.toSimpleVOList(dishes);
    }

    @Override
    public int deductStock(Long dishId, int quantity) {
        return dishMapper.deductStock(dishId, quantity);
    }

    @Override
    public void restoreStock(Long dishId, int quantity) {
        dishMapper.restoreStock(dishId, quantity);
    }

    @Override
    public void incrSales(Long dishId, int quantity) {
        dishMapper.incrSales(dishId, quantity);
    }

    /** 契约 VO 只需要的列，显式列出避免 SELECT * */
    private LambdaQueryWrapper<Dish> selectSimpleColumns() {
        return Wrappers.<Dish>lambdaQuery()
                .select(Dish::getId, Dish::getMerchantId, Dish::getCategoryId, Dish::getName,
                        Dish::getImage, Dish::getPrice, Dish::getStock, Dish::getSales, Dish::getStatus);
    }
}