package com.takeout.stats.controller;

import com.takeout.common.result.Result;
import com.takeout.order.api.vo.MerchantRankVO;
import com.takeout.stats.service.StatsService;
import com.takeout.stats.vo.PlatformOverviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理后台 - 平台看板接口
 */
@RestController
@RequestMapping("/admin/stats")
@RequiredArgsConstructor
@Tag(name = "管理后台-平台看板")
public class AdminStatsController {

    private final StatsService statsService;

    @GetMapping("/overview")
    @Operation(summary = "平台概览", description = "顾客数、店铺数、待审核数、今日与累计成交")
    public Result<PlatformOverviewVO> overview() {
        return Result.ok(statsService.platformOverview());
    }

    @GetMapping("/merchant-rank")
    @Operation(summary = "商家成交排行 TOP N")
    public Result<List<MerchantRankVO>> merchantRank(@RequestParam(defaultValue = "30") int days,
                                                     @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(statsService.merchantRank(days, limit));
    }
}