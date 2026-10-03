package com.takeout.dish.api;

import com.takeout.dish.api.vo.DishSimpleVO;

import java.util.Collection;
import java.util.List;

/**
 * 菜品模块对外契约
 *
 * <p>库存相关的三个方法都要求调用方在事务内使用，保证「扣减 + 落订单」是原子的。</p>
 */
public interface DishApi {

    DishSimpleVO getSimpleById(Long dishId);

    List<DishSimpleVO> listSimpleByIds(Collection<Long> dishIds);

    /**
     * 原子扣减库存
     *
     * <p>SQL 形如 {@code UPDATE dish SET stock = stock - #{qty} WHERE id = ? AND stock >= #{qty}}，
     * 靠数据库行锁保证并发下不会超卖，不需要应用层加锁。</p>
     *
     * @return 影响行数：1 成功 / 0 库存不足（调用方据此抛 STOCK_NOT_ENOUGH）
     */
    int deductStock(Long dishId, int quantity);

    /** 归还库存（订单取消/超时/拒单时调用） */
    void restoreStock(Long dishId, int quantity);

    /** 累加销量（订单完成时调用） */
    void incrSales(Long dishId, int quantity);
}