package com.takeout.stats.service;

import com.takeout.order.api.vo.DailyStatVO;
import com.takeout.order.api.vo.DishRankVO;
import com.takeout.order.api.vo.MerchantRankVO;
import com.takeout.stats.vo.MerchantOverviewVO;
import com.takeout.stats.vo.PlatformOverviewVO;

import java.util.List;

/**
 * 统计服务
 *
 * <p>本模块<b>不拥有任何表</b>，只做聚合编排：
 * 订单类指标走 OrderApi，店铺/账号类指标走 MerchantApi / UserApi。</p>
 */
public interface StatsService {

    /** 商家端概览 */
    MerchantOverviewVO merchantOverview();

    /** 商家端近 N 日趋势（无订单的日期补 0，保证折线图连续） */
    List<DailyStatVO> merchantTrend(int days);

    /** 商家端热销菜品 TOP N */
    List<DishRankVO> merchantHotDish(int days, int limit);

    /** 平台概览 */
    PlatformOverviewVO platformOverview();

    /** 平台商家成交排行 TOP N */
    List<MerchantRankVO> merchantRank(int days, int limit);
}