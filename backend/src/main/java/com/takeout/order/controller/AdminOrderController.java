package com.takeout.order.controller;

import com.takeout.common.result.PageResult;
import com.takeout.common.result.Result;
import com.takeout.order.dto.OrderQuery;
import com.takeout.order.service.OrderService;
import com.takeout.order.vo.OrderDetailVO;
import com.takeout.order.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台 - 订单总览接口（只读）
 */
@RestController
@RequestMapping("/admin/order")
@RequiredArgsConstructor
@Tag(name = "管理后台-订单总览")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "全平台订单分页", description = "可用 merchantId 筛选某店铺")
    public Result<PageResult<OrderVO>> page(@Validated OrderQuery query) {
        return Result.ok(orderService.adminOrders(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "订单详情")
    public Result<OrderDetailVO> detail(@PathVariable Long id) {
        return Result.ok(orderService.adminOrderDetail(id));
    }
}