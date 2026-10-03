package com.takeout.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.takeout.order.api.vo.DailyStatVO;
import com.takeout.order.api.vo.DishRankVO;
import com.takeout.order.api.vo.MerchantRankVO;
import com.takeout.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单 Mapper
 *
 * <p>自定义 @Select 不会被 MyBatis-Plus 的逻辑删除自动改写，
 * 因此每个统计 SQL 都必须手写 {@code del_flag = 0}，漏写会把已删除订单算进报表。</p>
 *
 * <p><b>营业额口径</b>：一律取「已完成（status = 50）」订单的 pay_amount 之和，
 * 与「销量在订单完成时累加」保持同一口径，报表数字与菜品销量能对上账。</p>
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /** 商家在区间内的完成订单数 */
    @Select("SELECT COUNT(*) FROM orders WHERE merchant_id = #{merchantId} AND status = 50 "
            + "AND finish_time >= #{from} AND finish_time < #{to} AND del_flag = 0")
    int countCompletedByMerchant(@Param("merchantId") Long merchantId,
                                 @Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to);

    /** 商家在区间内的营业额 */
    @Select("SELECT IFNULL(SUM(pay_amount), 0) FROM orders WHERE merchant_id = #{merchantId} AND status = 50 "
            + "AND finish_time >= #{from} AND finish_time < #{to} AND del_flag = 0")
    BigDecimal sumAmountByMerchant(@Param("merchantId") Long merchantId,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);

    /** 商家按日趋势 */
    @Select("SELECT DATE(finish_time) AS statDate, COUNT(*) AS orderCount, IFNULL(SUM(pay_amount), 0) AS amount "
            + "FROM orders WHERE merchant_id = #{merchantId} AND status = 50 "
            + "AND finish_time >= #{from} AND finish_time < #{to} AND del_flag = 0 "
            + "GROUP BY DATE(finish_time) ORDER BY statDate")
    List<DailyStatVO> statDailyByMerchant(@Param("merchantId") Long merchantId,
                                          @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);

    /** 商家热销菜品 TOP N（按份数） */
    @Select("SELECT oi.dish_id AS dishId, oi.dish_name AS dishName, "
            + "SUM(oi.quantity) AS quantity, IFNULL(SUM(oi.amount), 0) AS amount "
            + "FROM order_item oi INNER JOIN orders o ON oi.order_id = o.id "
            + "WHERE o.merchant_id = #{merchantId} AND o.status = 50 "
            + "AND o.finish_time >= #{from} AND o.finish_time < #{to} AND o.del_flag = 0 "
            + "GROUP BY oi.dish_id, oi.dish_name ORDER BY quantity DESC LIMIT #{limit}")
    List<DishRankVO> statHotDishByMerchant(@Param("merchantId") Long merchantId,
                                          @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to,
                                          @Param("limit") int limit);

    /** 平台完成订单数 */
    @Select("SELECT COUNT(*) FROM orders WHERE status = 50 "
            + "AND finish_time >= #{from} AND finish_time < #{to} AND del_flag = 0")
    int countCompletedPlatform(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** 平台成交额 */
    @Select("SELECT IFNULL(SUM(pay_amount), 0) FROM orders WHERE status = 50 "
            + "AND finish_time >= #{from} AND finish_time < #{to} AND del_flag = 0")
    BigDecimal sumAmountPlatform(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** 平台取消订单数 */
    @Select("SELECT COUNT(*) FROM orders WHERE status = 60 "
            + "AND create_time >= #{from} AND create_time < #{to} AND del_flag = 0")
    int countCanceledPlatform(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** 平台商家成交排行 */
    @Select("SELECT merchant_id AS merchantId, COUNT(*) AS orderCount, IFNULL(SUM(pay_amount), 0) AS amount "
            + "FROM orders WHERE status = 50 "
            + "AND finish_time >= #{from} AND finish_time < #{to} AND del_flag = 0 "
            + "GROUP BY merchant_id ORDER BY amount DESC LIMIT #{limit}")
    List<MerchantRankVO> statMerchantRank(@Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to,
                                          @Param("limit") int limit);

    /**
     * 统计指定状态的订单数
     *
     * <p>用于「待接单数」「待支付数」这类不看时间区间的实时指标。</p>
     */
    @Select("SELECT COUNT(*) FROM orders WHERE merchant_id = #{merchantId} AND status = #{status} AND del_flag = 0")
    int countByMerchantAndStatus(@Param("merchantId") Long merchantId, @Param("status") Integer status);
}