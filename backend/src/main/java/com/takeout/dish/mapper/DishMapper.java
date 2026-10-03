package com.takeout.dish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.takeout.dish.entity.Dish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 菜品 Mapper：只做数据库访问，不写业务逻辑
 *
 * <p>库存与销量的三个自增自减方法都用原子 SQL 完成，交由数据库行锁保证并发安全。</p>
 */
@Mapper
public interface DishMapper extends BaseMapper<Dish> {

    /**
     * 原子扣减库存
     *
     * <p>把「库存是否充足」的判断下沉到 WHERE 条件里：影响行数为 0 即库存不足，
     * 并发下单时无需应用层加锁也不会超卖。</p>
     *
     * @return 影响行数：1 成功 / 0 库存不足
     */
    @Update("UPDATE dish SET stock = stock - #{quantity} WHERE id = #{dishId} AND stock >= #{quantity} AND del_flag = 0")
    int deductStock(@Param("dishId") Long dishId, @Param("quantity") int quantity);

    /** 归还库存（订单取消/超时/拒单时调用） */
    @Update("UPDATE dish SET stock = stock + #{quantity} WHERE id = #{dishId} AND del_flag = 0")
    int restoreStock(@Param("dishId") Long dishId, @Param("quantity") int quantity);

    /** 累加销量（订单完成时调用，取消链路不回退） */
    @Update("UPDATE dish SET sales = sales + #{quantity} WHERE id = #{dishId} AND del_flag = 0")
    int incrSales(@Param("dishId") Long dishId, @Param("quantity") int quantity);
}