package com.takeout.stats.controller;

import com.takeout.common.result.Result;
import com.takeout.order.api.vo.DailyStatVO;
import com.takeout.order.api.vo.DishRankVO;
import com.takeout.stats.service.StatsService;
import com.takeout.stats.vo.MerchantOverviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商家端 - 经营统计接口
 */
@RestController
@RequestMapping("/merchant/stats")
@RequiredArgsConstructor
@Tag(name = "商家端-经营统计")
public class MerchantStatsController {

    private final StatsService statsService;

    @GetMapping("/overview")
    @Operation(summary = "经营概览", description = "今日/本月订单数与营业额、待接单、待支付")
    public Result<MerchantOverviewVO> overview() {
        return Result.ok(statsService.merchantOverview());
    }

    @GetMapping("/trend")
    @Operation(summary = "近 N 日营业额趋势", description = "无订单的日期补 0，可直接用于折线图")
    public Result<List<DailyStatVO>> trend(@RequestParam(defaultValue = "7") int days) {
        return Result.ok(statsService.merchantTrend(days));
    }

    @GetMapping("/hot-dish")
    @Operation(summary = "热销菜品 TOP N", description = "按完成订单的销售份数排序")
    public Result<List<DishRankVO>> hotDish(@RequestParam(defaultValue = "30") int days,
                                            @RequestParam(defaultValue = "5") int limit) {
        return Result.ok(statsService.merchantHotDish(days, limit));
    }
}