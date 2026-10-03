package com.takeout.stats.service.impl;

import com.takeout.common.context.UserContextHolder;
import com.takeout.common.enums.UserTypeEnum;
import com.takeout.common.exception.BusinessException;
import com.takeout.common.result.ResultCode;
import com.takeout.merchant.api.MerchantApi;
import com.takeout.merchant.api.vo.MerchantSimpleVO;
import com.takeout.order.api.OrderApi;
import com.takeout.order.api.vo.DailyStatVO;
import com.takeout.order.api.vo.DishRankVO;
import com.takeout.order.api.vo.MerchantOrderStatVO;
import com.takeout.order.api.vo.MerchantRankVO;
import com.takeout.order.api.vo.PlatformOrderStatVO;
import com.takeout.stats.service.StatsService;
import com.takeout.stats.vo.MerchantOverviewVO;
import com.takeout.stats.vo.PlatformOverviewVO;
import com.takeout.user.api.UserApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 统计服务实现
 *
 * <p><b>补零</b>：数据库按天聚合只会返回「有订单的日期」，
 * 直接丢给前端画折线图会出现断点。因此这里主动把缺失日期补成 0。</p>
 */
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private static final int MAX_DAYS = 90;
    private static final int MAX_LIMIT = 20;
    /** 平台累计口径的起点：早于系统存在的时间即可 */
    private static final LocalDate PLATFORM_START = LocalDate.of(2020, 1, 1);

    private final OrderApi orderApi;
    private final MerchantApi merchantApi;
    private final UserApi userApi;

    @Override
    public MerchantOverviewVO merchantOverview() {
        Long merchantId = currentMerchantId();
        MerchantSimpleVO merchant = merchantApi.getSimpleById(merchantId);

        LocalDate today = LocalDate.now();
        MerchantOrderStatVO todayStat = orderApi.statMerchant(merchantId, today, today);
        MerchantOrderStatVO monthStat = orderApi.statMerchant(merchantId, today.withDayOfMonth(1), today);

        MerchantOverviewVO vo = new MerchantOverviewVO();
        vo.setMerchantId(merchantId);
        vo.setMerchantName(merchant == null ? null : merchant.getName());
        vo.setBusinessStatus(merchant == null ? null : merchant.getBusinessStatus());
        vo.setTodayOrderCount(todayStat.getOrderCount());
        vo.setTodayAmount(todayStat.getSalesAmount());
        vo.setMonthOrderCount(monthStat.getOrderCount());
        vo.setMonthAmount(monthStat.getSalesAmount());
        // 待办指标取今天的实时值，与区间无关
        vo.setPendingAcceptCount(todayStat.getPendingAcceptCount());
        vo.setPendingPaymentCount(todayStat.getPendingPaymentCount());
        return vo;
    }

    @Override
    public List<DailyStatVO> merchantTrend(int days) {
        Long merchantId = currentMerchantId();
        int safeDays = normalizeDays(days);

        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(safeDays - 1L);
        return fillMissingDays(orderApi.statMerchantTrend(merchantId, from, to), from, to);
    }

    @Override
    public List<DishRankVO> merchantHotDish(int days, int limit) {
        Long merchantId = currentMerchantId();
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(normalizeDays(days) - 1L);
        return orderApi.statHotDish(merchantId, from, to, normalizeLimit(limit));
    }

    @Override
    public PlatformOverviewVO platformOverview() {
        LocalDate today = LocalDate.now();
        PlatformOrderStatVO todayStat = orderApi.statPlatform(today, today);
        PlatformOrderStatVO totalStat = orderApi.statPlatform(PLATFORM_START, today);

        PlatformOverviewVO vo = new PlatformOverviewVO();
        vo.setCustomerCount(userApi.countByType(UserTypeEnum.CUSTOMER.getCode()));
        vo.setMerchantCount(merchantApi.countByAuditStatus(1));
        vo.setPendingAuditCount(merchantApi.countByAuditStatus(0));
        vo.setTodayOrderCount(todayStat.getOrderCount());
        vo.setTodayAmount(todayStat.getSalesAmount());
        vo.setTotalOrderCount(totalStat.getOrderCount());
        vo.setTotalAmount(totalStat.getSalesAmount());
        vo.setCanceledCount(totalStat.getCanceledCount());
        return vo;
    }

    @Override
    public List<MerchantRankVO> merchantRank(int days, int limit) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(normalizeDays(days) - 1L);
        List<MerchantRankVO> ranks = orderApi.statMerchantRank(from, to, normalizeLimit(limit));
        if (ranks.isEmpty()) {
            return ranks;
        }
        // 补店铺名：订单模块的聚合 SQL 只拿得到 merchant_id
        Map<Long, MerchantSimpleVO> merchantMap = merchantApi
                .listSimpleByIds(ranks.stream().map(MerchantRankVO::getMerchantId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(MerchantSimpleVO::getMerchantId, Function.identity(), (a, b) -> a));
        ranks.forEach(rank -> {
            MerchantSimpleVO merchant = merchantMap.get(rank.getMerchantId());
            rank.setMerchantName(merchant == null ? null : merchant.getName());
        });
        return ranks;
    }

    // ==================== 内部方法 ====================

    private Long currentMerchantId() {
        MerchantSimpleVO merchant = merchantApi.getSimpleByUserId(UserContextHolder.getUserId());
        if (merchant == null) {
            throw new BusinessException(ResultCode.MERCHANT_NOT_FOUND, "当前账号还没有店铺，请先完成入驻");
        }
        return merchant.getMerchantId();
    }

    private int normalizeDays(int days) {
        if (days < 1) {
            return 7;
        }
        return Math.min(days, MAX_DAYS);
    }

    private int normalizeLimit(int limit) {
        if (limit < 1) {
            return 5;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    /** 把 [from, to] 区间内没有数据的日期补成 0 */
    private List<DailyStatVO> fillMissingDays(List<DailyStatVO> raw, LocalDate from, LocalDate to) {
        Map<LocalDate, DailyStatVO> byDate = raw.stream()
                .filter(item -> item.getStatDate() != null)
                .collect(Collectors.toMap(DailyStatVO::getStatDate, Function.identity(), (a, b) -> a));

        List<DailyStatVO> result = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            DailyStatVO exist = byDate.get(date);
            if (exist != null) {
                result.add(exist);
            } else {
                DailyStatVO empty = new DailyStatVO();
                empty.setStatDate(date);
                empty.setOrderCount(0);
                empty.setAmount(BigDecimal.ZERO);
                result.add(empty);
            }
        }
        return result;
    }
}