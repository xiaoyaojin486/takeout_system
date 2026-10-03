package com.takeout.order.api;

import com.takeout.order.api.vo.DailyStatVO;
import com.takeout.order.api.vo.DishRankVO;
import com.takeout.order.api.vo.MerchantOrderStatVO;
import com.takeout.order.api.vo.MerchantRankVO;
import com.takeout.order.api.vo.OrderSimpleVO;
import com.takeout.order.api.vo.PlatformOrderStatVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 订单模块对外契约
 *
 * <p>
 * 为什么统计接口也放在这里：只有 order 模块能访问 orders / order_item 表，
 * stats 模块要出报表就必须通过这些契约取聚合结果，不能自己去 join 别人的表。
 * </p>
 */
public interface OrderApi {

    /** 按订单号查（支付模块用） */
    OrderSimpleVO getByOrderNo(String orderNo);

    /** 按订单 id 查（支付模块作废未支付支付单时用） */
    OrderSimpleVO getById(Long orderId);

    /**
     * 标记订单已支付：状态 10 → 20，写 pay_time 与状态日志
     *
     * <p>
     * 调用方：payment 模块支付成功后。方法内部会做状态校验，
     * 重复调用会因状态不合法而被拒，天然幂等。
     * </p>
     */
    void markPaid(Long orderId);

    /** 商家区间统计（含当前待接单/待支付数） */
    MerchantOrderStatVO statMerchant(Long merchantId, LocalDate from, LocalDate to);

    /** 商家按日趋势（用于折线图） */
    List<DailyStatVO> statMerchantTrend(Long merchantId, LocalDate from, LocalDate to);

    /** 商家热销菜品 TOP N */
    List<DishRankVO> statHotDish(Long merchantId, LocalDate from, LocalDate to, int limit);

    /** 平台区间统计 */
    PlatformOrderStatVO statPlatform(LocalDate from, LocalDate to);

    /** 平台商家成交排行 TOP N */
    List<MerchantRankVO> statMerchantRank(LocalDate from, LocalDate to, int limit);
}